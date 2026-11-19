package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SessionStore
import com.example.data.model.AuthSession
import com.example.data.model.EditableLineItem
import com.example.data.remote.SupabaseClient
import com.example.data.repository.MissingWorkspaceException
import com.example.data.repository.ProQuoteRepository
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WorkspaceVerificationTest {

    private lateinit var sessionStore: SessionStore
    private val userId = "user-auth-uid-123"
    private val realWorkspaceId = "ws-real-9999-aaaa"

    @Before
    fun setup() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        sessionStore = SessionStore(context)
        sessionStore.saveConfig("https://example.supabase.co", "test-anon-key")
        sessionStore.saveSession(
            AuthSession(
                accessToken = "test-jwt-token",
                refreshToken = "test-refresh-token",
                userId = userId,
                email = "founder@acme.com"
            )
        )
    }

    private fun buildRepository(
        workspaceExists: Boolean,
        recordedRequests: MutableList<Pair<String, String>>
    ): ProQuoteRepository {
        val interceptor = Interceptor { chain ->
            val request = chain.request()
            val path = request.url.encodedPath + (request.url.encodedQuery?.let { "?$it" } ?: "")
            val bodyString = request.body?.let { body ->
                val buffer = Buffer()
                body.writeTo(buffer)
                buffer.readUtf8()
            }.orEmpty()
            recordedRequests += ("${request.method} $path" to bodyString)

            val responseJson = when {
                request.method == "GET" && path.startsWith("/rest/v1/pro_quote_workspace") -> {
                    if (workspaceExists) {
                        """[{"id":"$realWorkspaceId","user_id":"$userId","company_name":"Acme Studio","brand_color":"#0f172a","currency":"USD","tax_rate":18.0}]"""
                    } else {
                        "[]"
                    }
                }
                request.method == "POST" && path == "/rest/v1/clients" -> {
                    """[{"id":"client-1","workspace_id":"$realWorkspaceId","client_name":"Beta Corp"}]"""
                }
                request.method == "POST" && path == "/rest/v1/items_library" -> {
                    """[{"id":"item-1","workspace_id":"$realWorkspaceId","item_name":"Consulting","unit_price":500.0}]"""
                }
                request.method == "POST" && path == "/rest/v1/quotes" -> {
                    """[{"id":"quote-1","workspace_id":"$realWorkspaceId","quote_number":"QT-1001","status":"Draft","total_amount":500.0}]"""
                }
                request.method == "POST" && path == "/rest/v1/quote_items" -> {
                    """[{"id":"qi-1","quote_id":"quote-1","item_name":"Consulting","quantity":1.0,"unit_price":500.0,"total_price":500.0}]"""
                }
                else -> "[]"
            }

            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(responseJson.toResponseBody("application/json".toMediaType()))
                .build()
        }

        val okHttp = OkHttpClient.Builder().addInterceptor(interceptor).build()
        val supabaseClient = SupabaseClient(sessionStore, okHttp)
        return ProQuoteRepository(sessionStore, supabaseClient)
    }

    @Test
    fun `inserts are blocked with MissingWorkspaceException when no workspace exists for auth uid`() = runTest {
        val requests = mutableListOf<Pair<String, String>>()
        val repo = buildRepository(workspaceExists = false, recordedRequests = requests)

        val clientRes = repo.createClient("stale-fake-ws-id", "Beta Corp", null, null, null)
        assertTrue(clientRes.exceptionOrNull() is MissingWorkspaceException)

        val itemRes = repo.createLibraryItem("stale-fake-ws-id", "Consulting", null, 500.0)
        assertTrue(itemRes.exceptionOrNull() is MissingWorkspaceException)

        val quoteRes = repo.saveQuoteWithItems(
            existingQuoteId = null,
            workspaceId = "stale-fake-ws-id",
            clientId = null,
            quoteNumber = "QT-1001",
            status = "Draft",
            validUntil = null,
            notes = null,
            paymentInstructions = null,
            subtotal = 500.0,
            taxAmount = 0.0,
            discountAmount = 0.0,
            totalAmount = 500.0,
            lineItems = listOf(EditableLineItem("1", "Consulting", "", "1", "500"))
        )
        assertTrue(quoteRes.exceptionOrNull() is MissingWorkspaceException)

        // Verify pro_quote_workspace was checked before each insert, and NO inserts were attempted
        assertEquals(3, requests.size)
        assertTrue(requests.all { it.first.startsWith("GET /rest/v1/pro_quote_workspace?user_id=eq.$userId") })
        assertFalse(requests.any { it.first.startsWith("POST ") })
    }

    @Test
    fun `inserts verify pro_quote_workspace and always use real verified workspace_id`() = runTest {
        val requests = mutableListOf<Pair<String, String>>()
        val repo = buildRepository(workspaceExists = true, recordedRequests = requests)

        val clientRes = repo.createClient("fake-uuid-ignored", "Beta Corp", null, null, null)
        assertTrue(clientRes.isSuccess)
        assertEquals(realWorkspaceId, clientRes.getOrThrow().workspaceId)

        val itemRes = repo.createLibraryItem("fake-uuid-ignored", "Consulting", null, 500.0)
        assertTrue(itemRes.isSuccess)
        assertEquals(realWorkspaceId, itemRes.getOrThrow().workspaceId)

        val quoteRes = repo.saveQuoteWithItems(
            existingQuoteId = null,
            workspaceId = "fake-uuid-ignored",
            clientId = null,
            quoteNumber = "QT-1001",
            status = "Draft",
            validUntil = null,
            notes = null,
            paymentInstructions = null,
            subtotal = 500.0,
            taxAmount = 0.0,
            discountAmount = 0.0,
            totalAmount = 500.0,
            lineItems = listOf(EditableLineItem("1", "Consulting", "", "1", "500"))
        )
        assertTrue(quoteRes.isSuccess)
        assertEquals(realWorkspaceId, quoteRes.getOrThrow().workspaceId)

        val postRequests = requests.filter { it.first.startsWith("POST /rest/v1/clients") || it.first.startsWith("POST /rest/v1/items_library") || it.first.startsWith("POST /rest/v1/quotes") }
        assertEquals(3, postRequests.size)
        assertTrue(postRequests.all { it.second.contains(realWorkspaceId) && !it.second.contains("fake-uuid-ignored") })
    }

    @Test
    fun `jwt sub claim overrides stale session userId when verifying pro_quote_workspace`() = runTest {
        val header = android.util.Base64.encodeToString(
            """{"alg":"HS256","typ":"JWT"}""".toByteArray(),
            android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING
        )
        val payload = android.util.Base64.encodeToString(
            """{"sub":"$userId","role":"authenticated"}""".toByteArray(),
            android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING
        )
        sessionStore.saveSession(
            AuthSession(
                accessToken = "$header.$payload.sig",
                refreshToken = "test-refresh-token",
                userId = "stale-different-user-id",
                email = "founder@acme.com"
            )
        )

        val requests = mutableListOf<Pair<String, String>>()
        val repo = buildRepository(workspaceExists = true, recordedRequests = requests)

        val clientRes = repo.createClient("fake-uuid-ignored", "Gamma Ltd", null, null, null)
        assertTrue(clientRes.isSuccess)
        assertEquals(realWorkspaceId, clientRes.getOrThrow().workspaceId)
        assertTrue(
            requests.any { it.first.startsWith("GET /rest/v1/pro_quote_workspace?user_id=eq.$userId") }
        )
    }
}
