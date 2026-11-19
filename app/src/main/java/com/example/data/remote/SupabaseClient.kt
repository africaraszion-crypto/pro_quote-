package com.example.data.remote

import com.example.data.local.SessionStore
import com.example.data.model.AuthCredentialsRequest
import com.example.data.model.AuthResponse
import com.example.data.model.AuthSession
import com.example.data.model.RefreshTokenRequest
import com.example.data.model.SupabaseConfig
import com.example.data.model.SupabaseErrorResponse
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.util.concurrent.TimeUnit

class SupabaseApiException(
    val statusCode: Int,
    override val message: String,
    val errorCode: String? = null,
    val details: String? = null
) : Exception(message)

class SupabaseClient(
    private val sessionStore: SessionStore,
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
        )
        .build()
) {

    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val errorAdapter = moshi.adapter(SupabaseErrorResponse::class.java)

    suspend fun testConnection(url: String, key: String): Result<Unit> = withContext(Dispatchers.IO) {
        val cleanUrl = url.trim().trimEnd('/')
        val cleanKey = key.trim()
        if (!cleanUrl.startsWith("https://") && !cleanUrl.startsWith("http://")) {
            return@withContext Result.failure(IllegalArgumentException("Project URL must start with https://"))
        }
        if (cleanKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Publishable / Anon Key is required"))
        }
        try {
            val builder = Request.Builder()
                .url("$cleanUrl/auth/v1/settings")
            applyAuthHeaders(builder, cleanKey, null)
            val req = builder.get().build()
            okHttpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    Result.success(Unit)
                } else {
                    val body = resp.body?.string()
                    Result.failure(buildApiException(body, resp.code))
                }
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error: ${e.localizedMessage ?: "Unable to reach Supabase"}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUp(email: String, password: String): Result<AuthResponse> {
        val config = requireConfig() ?: return Result.failure(IllegalStateException("Supabase is not configured"))
        val adapter = moshi.adapter(AuthCredentialsRequest::class.java)
        val payload = adapter.toJson(AuthCredentialsRequest(email.trim(), password))
        return executeRaw(
            config = config,
            path = "/auth/v1/signup",
            method = "POST",
            bodyJson = payload,
            useUserToken = false,
            responseAdapter = moshi.adapter(AuthResponse::class.java)
        ).map { authResp ->
            val resolvedUserId = authResp.user?.id ?: authResp.id
            if (!authResp.accessToken.isNullOrBlank() && !resolvedUserId.isNullOrBlank()) {
                sessionStore.saveSession(
                    AuthSession(
                        accessToken = authResp.accessToken,
                        refreshToken = authResp.refreshToken.orEmpty(),
                        userId = resolvedUserId,
                        email = authResp.user?.email ?: authResp.email ?: email.trim()
                    )
                )
            }
            authResp
        }
    }

    suspend fun signIn(email: String, password: String): Result<AuthSession> {
        val config = requireConfig() ?: return Result.failure(IllegalStateException("Supabase is not configured"))
        val adapter = moshi.adapter(AuthCredentialsRequest::class.java)
        val payload = adapter.toJson(AuthCredentialsRequest(email.trim(), password))
        return executeRaw(
            config = config,
            path = "/auth/v1/token?grant_type=password",
            method = "POST",
            bodyJson = payload,
            useUserToken = false,
            responseAdapter = moshi.adapter(AuthResponse::class.java)
        ).mapCatching { authResp ->
            val token = authResp.accessToken
                ?: throw SupabaseApiException(401, "Missing access token from Supabase Auth")
            val user = authResp.user
                ?: throw SupabaseApiException(401, "Missing user profile from Supabase Auth")
            val session = AuthSession(
                accessToken = token,
                refreshToken = authResp.refreshToken.orEmpty(),
                userId = user.id,
                email = user.email ?: email.trim()
            )
            sessionStore.saveSession(session)
            session
        }
    }

    suspend fun signOut(): Result<Unit> {
        val config = sessionStore.getConfig()
        val session = sessionStore.getSession()
        if (config.isConfigured && session != null) {
            withContext(Dispatchers.IO) {
                try {
                    val req = Request.Builder()
                        .url("${config.url}/auth/v1/logout")
                        .header("apikey", config.key)
                        .header("Authorization", "Bearer ${session.accessToken}")
                        .post("".toRequestBody(jsonMediaType))
                        .build()
                    okHttpClient.newCall(req).execute().close()
                } catch (_: Exception) {
                }
            }
        }
        sessionStore.clearSession()
        return Result.success(Unit)
    }

    suspend inline fun <reified T> getList(
        tableAndQuery: String,
        useUserToken: Boolean = true
    ): Result<List<T>> {
        val listType = Types.newParameterizedType(List::class.java, T::class.java)
        val adapter: JsonAdapter<List<T>> = moshi.adapter(listType)
        return executeRest(
            path = "/rest/v1/$tableAndQuery",
            method = "GET",
            bodyJson = null,
            useUserToken = useUserToken,
            prefer = null,
            responseAdapter = adapter
        )
    }

    suspend inline fun <reified Req, reified Res> postSingle(
        table: String,
        body: Req,
        useUserToken: Boolean = true
    ): Result<Res> {
        val reqAdapter = moshi.adapter(Req::class.java)
        val listType = Types.newParameterizedType(List::class.java, Res::class.java)
        val resAdapter: JsonAdapter<List<Res>> = moshi.adapter(listType)
        return executeRest(
            path = "/rest/v1/$table",
            method = "POST",
            bodyJson = reqAdapter.toJson(body),
            useUserToken = useUserToken,
            prefer = "return=representation",
            responseAdapter = resAdapter
        ).mapCatching { list ->
            list.firstOrNull() ?: throw SupabaseApiException(500, "Empty response returned from $table")
        }
    }

    suspend inline fun <reified Req, reified Res> postList(
        table: String,
        items: List<Req>,
        useUserToken: Boolean = true
    ): Result<List<Res>> {
        if (items.isEmpty()) return Result.success(emptyList())
        val reqType = Types.newParameterizedType(List::class.java, Req::class.java)
        val reqAdapter: JsonAdapter<List<Req>> = moshi.adapter(reqType)
        val resType = Types.newParameterizedType(List::class.java, Res::class.java)
        val resAdapter: JsonAdapter<List<Res>> = moshi.adapter(resType)
        return executeRest(
            path = "/rest/v1/$table",
            method = "POST",
            bodyJson = reqAdapter.toJson(items),
            useUserToken = useUserToken,
            prefer = "return=representation",
            responseAdapter = resAdapter
        )
    }

    suspend inline fun <reified Req, reified Res> patchList(
        tableAndQuery: String,
        body: Req,
        useUserToken: Boolean = true
    ): Result<List<Res>> {
        val reqAdapter = moshi.adapter(Req::class.java)
        val resType = Types.newParameterizedType(List::class.java, Res::class.java)
        val resAdapter: JsonAdapter<List<Res>> = moshi.adapter(resType)
        return executeRest(
            path = "/rest/v1/$tableAndQuery",
            method = "PATCH",
            bodyJson = reqAdapter.toJson(body),
            useUserToken = useUserToken,
            prefer = "return=representation",
            responseAdapter = resAdapter
        )
    }

    suspend fun delete(
        tableAndQuery: String,
        useUserToken: Boolean = true
    ): Result<Unit> {
        val config = requireConfig() ?: return Result.failure(IllegalStateException("Supabase is not configured"))
        return executeRawUnit(
            config = config,
            path = "/rest/v1/$tableAndQuery",
            method = "DELETE",
            useUserToken = useUserToken
        )
    }

    suspend fun <T> executeRest(
        path: String,
        method: String,
        bodyJson: String?,
        useUserToken: Boolean,
        prefer: String?,
        responseAdapter: JsonAdapter<T>
    ): Result<T> {
        val config = requireConfig() ?: return Result.failure(IllegalStateException("Supabase is not configured"))
        val firstAttempt = executeRaw(config, path, method, bodyJson, useUserToken, prefer, responseAdapter)
        val ex = firstAttempt.exceptionOrNull()
        if (useUserToken && ex is SupabaseApiException && ex.statusCode == 401) {
            if (refreshAccessToken(config)) {
                return executeRaw(config, path, method, bodyJson, useUserToken, prefer, responseAdapter)
            }
        }
        return firstAttempt
    }

    private suspend fun executeRawUnit(
        config: SupabaseConfig,
        path: String,
        method: String,
        useUserToken: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val session = if (useUserToken) sessionStore.getSession() else null
        val userToken = if (useUserToken) session?.accessToken else null
        try {
            val builder = Request.Builder()
                .url("${config.url}$path")
            applyAuthHeaders(builder, config.key, userToken)
            if (method == "DELETE") {
                builder.delete()
            }
            okHttpClient.newCall(builder.build()).execute().use { resp ->
                if (resp.isSuccessful) {
                    Result.success(Unit)
                } else {
                    val body = resp.body?.string()
                    if (useUserToken && resp.code == 401 && refreshAccessToken(config)) {
                        val refreshed = sessionStore.getSession()
                        val retryBuilder = Request.Builder()
                            .url("${config.url}$path")
                        applyAuthHeaders(retryBuilder, config.key, refreshed?.accessToken)
                        val retryReq = retryBuilder.delete().build()
                        okHttpClient.newCall(retryReq).execute().use { retryResp ->
                            if (retryResp.isSuccessful) Result.success(Unit)
                            else Result.failure(buildApiException(retryResp.body?.string(), retryResp.code))
                        }
                    } else {
                        Result.failure(buildApiException(body, resp.code))
                    }
                }
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error: ${e.localizedMessage ?: "Request failed"}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun <T> executeRaw(
        config: SupabaseConfig,
        path: String,
        method: String,
        bodyJson: String?,
        useUserToken: Boolean,
        prefer: String? = null,
        responseAdapter: JsonAdapter<T>
    ): Result<T> = withContext(Dispatchers.IO) {
        val session = if (useUserToken) sessionStore.getSession() else null
        val userToken = if (useUserToken) session?.accessToken else null
        try {
            val builder = Request.Builder()
                .url("${config.url}$path")
                .header("Content-Type", "application/json")
            applyAuthHeaders(builder, config.key, userToken)
            if (!prefer.isNullOrBlank()) {
                builder.header("Prefer", prefer)
            }
            when (method) {
                "GET" -> builder.get()
                "POST" -> builder.post((bodyJson ?: "{}").toRequestBody(jsonMediaType))
                "PATCH" -> builder.patch((bodyJson ?: "{}").toRequestBody(jsonMediaType))
                "PUT" -> builder.put((bodyJson ?: "{}").toRequestBody(jsonMediaType))
                "DELETE" -> builder.delete()
            }
            okHttpClient.newCall(builder.build()).execute().use { resp ->
                val rawBody = resp.body?.string().orEmpty()
                if (resp.isSuccessful) {
                    val parsed = responseAdapter.fromJson(rawBody)
                        ?: return@withContext Result.failure(
                            SupabaseApiException(resp.code, "Unable to parse Supabase response")
                        )
                    Result.success(parsed)
                } else {
                    Result.failure(buildApiException(rawBody, resp.code))
                }
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error: ${e.localizedMessage ?: "Unable to connect"}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun refreshAccessToken(config: SupabaseConfig): Boolean {
        val currentSession = sessionStore.getSession() ?: return false
        if (currentSession.refreshToken.isBlank()) return false
        val reqAdapter = moshi.adapter(RefreshTokenRequest::class.java)
        val payload = reqAdapter.toJson(RefreshTokenRequest(currentSession.refreshToken))
        val res = executeRaw(
            config = config,
            path = "/auth/v1/token?grant_type=refresh_token",
            method = "POST",
            bodyJson = payload,
            useUserToken = false,
            responseAdapter = moshi.adapter(AuthResponse::class.java)
        )
        val auth = res.getOrNull() ?: return false
        val newToken = auth.accessToken ?: return false
        sessionStore.saveSession(
            AuthSession(
                accessToken = newToken,
                refreshToken = auth.refreshToken ?: currentSession.refreshToken,
                userId = auth.user?.id ?: currentSession.userId,
                email = auth.user?.email ?: currentSession.email
            )
        )
        return true
    }

    private fun applyAuthHeaders(
        builder: Request.Builder,
        apiKey: String,
        userJwt: String?
    ) {
        builder.header("apikey", apiKey)
        if (!userJwt.isNullOrBlank()) {
            builder.header("Authorization", "Bearer $userJwt")
        } else if (apiKey.startsWith("eyJ")) {
            builder.header("Authorization", "Bearer $apiKey")
        }
    }

    private suspend fun requireConfig(): SupabaseConfig? {
        val cfg = sessionStore.getConfig()
        return if (cfg.isConfigured) cfg else null
    }

    private fun buildApiException(body: String?, code: Int): SupabaseApiException {
        if (!body.isNullOrBlank()) {
            try {
                val parsed = errorAdapter.fromJson(body)
                val msg = parsed?.message ?: parsed?.errorDescription ?: parsed?.msg ?: parsed?.error
                if (!msg.isNullOrBlank()) {
                    val fullMsg = if (!parsed?.hint.isNullOrBlank()) "$msg (${parsed?.hint})" else msg
                    return SupabaseApiException(
                        statusCode = code,
                        message = fullMsg,
                        errorCode = parsed?.code,
                        details = parsed?.details
                    )
                }
            } catch (_: Exception) {
            }
        }
        return SupabaseApiException(code, "Request failed (HTTP $code)")
    }
}
