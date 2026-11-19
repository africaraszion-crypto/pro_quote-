package com.example.data.repository

import android.util.Base64
import com.example.data.local.SessionStore
import com.example.data.model.AuthResponse
import com.example.data.model.AuthSession
import com.example.data.model.Client
import com.example.data.model.ClientUpsertRequest
import com.example.data.model.EditableLineItem
import com.example.data.model.LibraryItem
import com.example.data.model.LibraryItemUpsertRequest
import com.example.data.model.PublicQuoteBundle
import com.example.data.model.Quote
import com.example.data.model.QuoteAcceptRequest
import com.example.data.model.QuoteDeclineRequest
import com.example.data.model.QuoteItem
import com.example.data.model.QuoteItemInsertRequest
import com.example.data.model.QuoteUpsertRequest
import com.example.data.model.SupabaseConfig
import com.example.data.model.Workspace
import com.example.data.model.WorkspaceCreateRequest
import com.example.data.model.WorkspaceUpdateRequest
import com.example.data.remote.SupabaseClient
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class MissingWorkspaceException(
    message: String = "No workspace found for your account. Please complete onboarding first."
) : IllegalStateException(message)

class NotAuthenticatedException(
    message: String = "Not authenticated. Please sign in."
) : IllegalStateException(message)

class ProQuoteRepository(
    private val sessionStore: SessionStore,
    private val supabase: SupabaseClient
) {
    val configFlow: Flow<SupabaseConfig> = sessionStore.configFlow
    val sessionFlow: Flow<AuthSession?> = sessionStore.sessionFlow

    suspend fun getConfig(): SupabaseConfig = sessionStore.getConfig()
    suspend fun getSession(): AuthSession? = sessionStore.getSession()

    private fun resolveAuthUid(session: AuthSession): String {
        val parts = session.accessToken.split('.')
        if (parts.size == 3) {
            try {
                val decodedBytes = Base64.decode(
                    parts[1],
                    Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
                )
                val json = JSONObject(String(decodedBytes, StandardCharsets.UTF_8))
                val sub = json.optString("sub").trim()
                if (sub.isNotBlank()) return sub
            } catch (_: Exception) {
            }
        }
        return session.userId.trim()
    }

    private suspend fun requireAuthenticatedSession(): Result<AuthSession> {
        val session = sessionStore.getSession()
        if (session == null || session.accessToken.isBlank()) {
            return Result.failure(NotAuthenticatedException())
        }
        val uid = resolveAuthUid(session)
        if (uid.isBlank()) {
            return Result.failure(NotAuthenticatedException())
        }
        return Result.success(if (uid != session.userId) session.copy(userId = uid) else session)
    }

    suspend fun testAndSaveSupabaseConfig(url: String, key: String): Result<Unit> {
        val test = supabase.testConnection(url, key)
        return if (test.isSuccess) {
            sessionStore.saveConfig(url, key)
            Result.success(Unit)
        } else {
            test
        }
    }

    suspend fun clearSupabaseConfig() {
        sessionStore.clearConfig()
    }

    suspend fun signIn(email: String, password: String): Result<AuthSession> {
        return supabase.signIn(email, password)
    }

    suspend fun signUp(email: String, password: String): Result<AuthResponse> {
        return supabase.signUp(email, password)
    }

    suspend fun signOut(): Result<Unit> {
        return supabase.signOut()
    }

    // --- Workspace ---
    suspend fun fetchWorkspaceForCurrentUser(): Result<Workspace?> {
        val session = requireAuthenticatedSession().getOrElse { return Result.failure(it) }
        val authUid = resolveAuthUid(session)
        return supabase.getList<Workspace>(
            "pro_quote_workspace?user_id=eq.$authUid&select=*&limit=1"
        ).map { list ->
            list.firstOrNull { it.id.isNotBlank() && it.userId.equals(authUid, ignoreCase = true) }
        }
    }

    suspend fun verifyWorkspaceForCurrentUser(): Result<Workspace> {
        val workspace = fetchWorkspaceForCurrentUser().getOrElse { return Result.failure(it) }
            ?: return Result.failure(MissingWorkspaceException())
        return Result.success(workspace)
    }

    suspend fun createWorkspace(
        companyName: String,
        currency: String,
        taxRate: Double,
        brandColor: String,
        logoUrl: String? = null
    ): Result<Workspace> {
        val session = requireAuthenticatedSession().getOrElse { return Result.failure(it) }
        val authUid = resolveAuthUid(session)
        val existing = fetchWorkspaceForCurrentUser().getOrElse { return Result.failure(it) }
        if (existing != null) {
            return supabase.patchList<WorkspaceUpdateRequest, Workspace>(
                tableAndQuery = "pro_quote_workspace?id=eq.${existing.id}&user_id=eq.$authUid",
                body = WorkspaceUpdateRequest(
                    companyName = companyName.trim(),
                    logoUrl = logoUrl?.trim()?.ifBlank { null } ?: existing.logoUrl,
                    brandColor = brandColor.trim().ifBlank { "#0f172a" },
                    currency = currency.trim().ifBlank { "TZS" },
                    taxRate = taxRate
                )
            ).mapCatching { list ->
                list.firstOrNull() ?: existing
            }
        }
        return supabase.postSingle<WorkspaceCreateRequest, Workspace>(
            table = "pro_quote_workspace",
            body = WorkspaceCreateRequest(
                userId = authUid,
                companyName = companyName.trim(),
                logoUrl = logoUrl?.trim()?.ifBlank { null },
                brandColor = brandColor.trim().ifBlank { "#0f172a" },
                currency = currency.trim().ifBlank { "TZS" },
                taxRate = taxRate
            )
        )
    }

    suspend fun updateWorkspace(
        workspaceId: String,
        companyName: String,
        logoUrl: String?,
        brandColor: String,
        currency: String,
        taxRate: Double,
        defaultPaymentInstructions: String
    ): Result<Workspace> {
        val verifiedWorkspace = verifyWorkspaceForCurrentUser().getOrElse { return Result.failure(it) }
        val res = supabase.patchList<WorkspaceUpdateRequest, Workspace>(
            tableAndQuery = "pro_quote_workspace?id=eq.${verifiedWorkspace.id}",
            body = WorkspaceUpdateRequest(
                companyName = companyName.trim(),
                logoUrl = logoUrl?.trim()?.ifBlank { null },
                brandColor = brandColor.trim().ifBlank { "#0f172a" },
                currency = currency.trim().ifBlank { "TZS" },
                taxRate = taxRate
            )
        ).mapCatching { list ->
            list.firstOrNull() ?: throw MissingWorkspaceException()
        }
        if (res.isSuccess) {
            sessionStore.saveDefaultPaymentInstructions(verifiedWorkspace.id, defaultPaymentInstructions)
        }
        return res
    }

    suspend fun getDefaultPaymentInstructions(workspaceId: String): String {
        return sessionStore.getDefaultPaymentInstructions(workspaceId)
    }

    // --- Clients ---
    suspend fun fetchClients(workspaceId: String): Result<List<Client>> {
        requireAuthenticatedSession().getOrElse { return Result.failure(it) }
        if (workspaceId.isBlank()) return Result.failure(MissingWorkspaceException())
        return supabase.getList("clients?workspace_id=eq.$workspaceId&select=*&order=created_at.desc")
    }

    suspend fun createClient(
        workspaceId: String,
        clientName: String,
        email: String?,
        phone: String?,
        address: String?
    ): Result<Client> {
        val verifiedWorkspace = verifyWorkspaceForCurrentUser().getOrElse { return Result.failure(it) }
        return supabase.postSingle(
            table = "clients",
            body = ClientUpsertRequest(
                workspaceId = verifiedWorkspace.id,
                clientName = clientName.trim(),
                email = email?.trim()?.ifBlank { null },
                phone = phone?.trim()?.ifBlank { null },
                address = address?.trim()?.ifBlank { null }
            )
        )
    }

    suspend fun updateClient(
        clientId: String,
        workspaceId: String,
        clientName: String,
        email: String?,
        phone: String?,
        address: String?
    ): Result<Client> {
        val verifiedWorkspace = verifyWorkspaceForCurrentUser().getOrElse { return Result.failure(it) }
        return supabase.patchList<ClientUpsertRequest, Client>(
            tableAndQuery = "clients?id=eq.$clientId",
            body = ClientUpsertRequest(
                workspaceId = verifiedWorkspace.id,
                clientName = clientName.trim(),
                email = email?.trim()?.ifBlank { null },
                phone = phone?.trim()?.ifBlank { null },
                address = address?.trim()?.ifBlank { null }
            )
        ).mapCatching { it.firstOrNull() ?: throw IllegalStateException("Client not found") }
    }

    suspend fun deleteClient(clientId: String): Result<Unit> {
        requireAuthenticatedSession().getOrElse { return Result.failure(it) }
        return supabase.delete("clients?id=eq.$clientId")
    }

    // --- Item Library ---
    suspend fun fetchLibraryItems(workspaceId: String): Result<List<LibraryItem>> {
        requireAuthenticatedSession().getOrElse { return Result.failure(it) }
        if (workspaceId.isBlank()) return Result.failure(MissingWorkspaceException())
        return supabase.getList("items_library?workspace_id=eq.$workspaceId&select=*&order=created_at.desc")
    }

    suspend fun createLibraryItem(
        workspaceId: String,
        itemName: String,
        description: String?,
        unitPrice: Double
    ): Result<LibraryItem> {
        val verifiedWorkspace = verifyWorkspaceForCurrentUser().getOrElse { return Result.failure(it) }
        return supabase.postSingle(
            table = "items_library",
            body = LibraryItemUpsertRequest(
                workspaceId = verifiedWorkspace.id,
                itemName = itemName.trim(),
                description = description?.trim()?.ifBlank { null },
                unitPrice = unitPrice
            )
        )
    }

    suspend fun updateLibraryItem(
        itemId: String,
        workspaceId: String,
        itemName: String,
        description: String?,
        unitPrice: Double
    ): Result<LibraryItem> {
        val verifiedWorkspace = verifyWorkspaceForCurrentUser().getOrElse { return Result.failure(it) }
        return supabase.patchList<LibraryItemUpsertRequest, LibraryItem>(
            tableAndQuery = "items_library?id=eq.$itemId",
            body = LibraryItemUpsertRequest(
                workspaceId = verifiedWorkspace.id,
                itemName = itemName.trim(),
                description = description?.trim()?.ifBlank { null },
                unitPrice = unitPrice
            )
        ).mapCatching { it.firstOrNull() ?: throw IllegalStateException("Item not found") }
    }

    suspend fun deleteLibraryItem(itemId: String): Result<Unit> {
        requireAuthenticatedSession().getOrElse { return Result.failure(it) }
        return supabase.delete("items_library?id=eq.$itemId")
    }

    // --- Quotes & Quote Items ---
    suspend fun fetchQuotes(workspaceId: String): Result<List<Quote>> {
        requireAuthenticatedSession().getOrElse { return Result.failure(it) }
        if (workspaceId.isBlank()) return Result.failure(MissingWorkspaceException())
        return supabase.getList("quotes?workspace_id=eq.$workspaceId&select=*&order=created_at.desc")
    }

    suspend fun fetchQuoteItems(quoteId: String): Result<List<QuoteItem>> {
        requireAuthenticatedSession().getOrElse { return Result.failure(it) }
        return supabase.getList("quote_items?quote_id=eq.$quoteId&select=*")
    }

    suspend fun saveQuoteWithItems(
        existingQuoteId: String?,
        workspaceId: String,
        clientId: String?,
        quoteNumber: String,
        status: String,
        validUntil: String?,
        notes: String?,
        paymentInstructions: String?,
        subtotal: Double,
        taxAmount: Double,
        discountAmount: Double,
        totalAmount: Double,
        lineItems: List<EditableLineItem>
    ): Result<Quote> {
        val verifiedWorkspace = verifyWorkspaceForCurrentUser().getOrElse { return Result.failure(it) }
        val request = QuoteUpsertRequest(
            workspaceId = verifiedWorkspace.id,
            clientId = clientId?.ifBlank { null },
            quoteNumber = quoteNumber.trim(),
            status = status,
            validUntil = validUntil?.ifBlank { null },
            notes = notes?.trim()?.ifBlank { null },
            paymentInstructions = paymentInstructions?.trim()?.ifBlank { null },
            subtotal = subtotal,
            taxAmount = taxAmount,
            discountAmount = discountAmount,
            totalAmount = totalAmount
        )

        val quoteResult: Result<Quote> = if (existingQuoteId.isNullOrBlank()) {
            supabase.postSingle("quotes", request)
        } else {
            supabase.patchList<QuoteUpsertRequest, Quote>(
                tableAndQuery = "quotes?id=eq.$existingQuoteId",
                body = request
            ).mapCatching { it.firstOrNull() ?: throw IllegalStateException("Quote not found") }
        }

        val savedQuote = quoteResult.getOrElse { return Result.failure(it) }

        if (!existingQuoteId.isNullOrBlank()) {
            val deleteRes = supabase.delete("quote_items?quote_id=eq.${savedQuote.id}")
            if (deleteRes.isFailure) {
                return Result.failure(deleteRes.exceptionOrNull() ?: Exception("Failed to replace line items"))
            }
        }

        val itemsToInsert = lineItems
            .filter { it.itemName.isNotBlank() }
            .map { line ->
                QuoteItemInsertRequest(
                    quoteId = savedQuote.id,
                    itemName = line.itemName.trim(),
                    description = line.description.trim().ifBlank { null },
                    quantity = line.qtyValue,
                    unitPrice = line.unitPriceValue,
                    totalPrice = line.totalPriceValue
                )
            }

        if (itemsToInsert.isNotEmpty()) {
            val insertRes = supabase.postList<QuoteItemInsertRequest, QuoteItem>(
                table = "quote_items",
                items = itemsToInsert
            )
            if (insertRes.isFailure) {
                return Result.failure(insertRes.exceptionOrNull() ?: Exception("Failed to save quote line items"))
            }
        }

        return Result.success(savedQuote)
    }

    suspend fun deleteQuote(quoteId: String): Result<Unit> {
        requireAuthenticatedSession().getOrElse { return Result.failure(it) }
        supabase.delete("quote_items?quote_id=eq.$quoteId")
        return supabase.delete("quotes?id=eq.$quoteId")
    }

    // --- Public Magic Link Viewer (/q/:id) - NO AUTH REQUIRED ---
    suspend fun fetchPublicQuoteBundle(quoteId: String): Result<PublicQuoteBundle> {
        val cleanId = quoteId.trim()
        if (cleanId.isBlank()) {
            return Result.failure(IllegalArgumentException("Quote ID is required"))
        }

        // Works with anon/publishable key without user auth
        val quoteRes = supabase.getList<Quote>(
            tableAndQuery = "quotes?id=eq.$cleanId&select=*&limit=1",
            useUserToken = false
        )
        val quote = quoteRes.getOrElse { return Result.failure(it) }.firstOrNull()
            ?: return Result.failure(IllegalStateException("Quote not found or no longer accessible."))

        val items = supabase.getList<QuoteItem>(
            tableAndQuery = "quote_items?quote_id=eq.${quote.id}&select=*",
            useUserToken = false
        ).getOrDefault(emptyList())

        val client = if (!quote.clientId.isNullOrBlank()) {
            supabase.getList<Client>(
                tableAndQuery = "clients?id=eq.${quote.clientId}&select=*&limit=1",
                useUserToken = false
            ).getOrNull()?.firstOrNull()
        } else null

        val workspace = supabase.getList<Workspace>(
            tableAndQuery = "pro_quote_workspace?id=eq.${quote.workspaceId}&select=*&limit=1",
            useUserToken = false
        ).getOrNull()?.firstOrNull()

        return Result.success(
            PublicQuoteBundle(
                quote = quote,
                items = items,
                client = client,
                workspace = workspace
            )
        )
    }

    suspend fun acceptAndSignPublicQuote(
        quoteId: String,
        signatureDataUrl: String,
        signedByName: String
    ): Result<Quote> {
        val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val nowIso = isoFormatter.format(Date())
        return supabase.patchList<QuoteAcceptRequest, Quote>(
            tableAndQuery = "quotes?id=eq.$quoteId",
            body = QuoteAcceptRequest(
                status = "Accepted",
                signatureDataUrl = signatureDataUrl,
                signedByName = signedByName.trim(),
                signedAt = nowIso
            ),
            useUserToken = false
        ).mapCatching {
            it.firstOrNull() ?: throw IllegalStateException("Unable to update quote status. Check RLS policies.")
        }
    }

    suspend fun declinePublicQuote(quoteId: String): Result<Quote> {
        return supabase.patchList<QuoteDeclineRequest, Quote>(
            tableAndQuery = "quotes?id=eq.$quoteId",
            body = QuoteDeclineRequest(status = "Declined"),
            useUserToken = false
        ).mapCatching {
            it.firstOrNull() ?: throw IllegalStateException("Unable to decline quote. Check RLS policies.")
        }
    }
}
