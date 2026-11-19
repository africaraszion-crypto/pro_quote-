package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.BuildConfig
import com.example.data.model.AuthSession
import com.example.data.model.SupabaseConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pro_quote_prefs")

class SessionStore(private val context: Context) {

    companion object {
        const val FALLBACK_SUPABASE_URL = "https://zomitegahtxyqseqrrpp.supabase.co"
        const val FALLBACK_SUPABASE_KEY = "sb_publishable_pRiYO7oCTOtnO0aIPDGYIQ_-XMSpxCi"

        private val KEY_SB_URL = stringPreferencesKey("sb_url")
        private val KEY_SB_KEY = stringPreferencesKey("sb_key")
        private val KEY_ACCESS_TOKEN = stringPreferencesKey("sb_access_token")
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("sb_refresh_token")
        private val KEY_USER_ID = stringPreferencesKey("sb_user_id")
        private val KEY_USER_EMAIL = stringPreferencesKey("sb_user_email")
        private val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
        private fun paymentInstructionsKey(workspaceId: String) =
            stringPreferencesKey("default_payment_instructions_$workspaceId")
    }

    val languageFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_APP_LANGUAGE]?.trim().takeUnless { it.isNullOrBlank() } ?: "en"
    }

    val configFlow: Flow<SupabaseConfig> = context.dataStore.data.map { prefs ->
        val storedUrl = prefs[KEY_SB_URL]?.trim().orEmpty()
        val storedKey = prefs[KEY_SB_KEY]?.trim().orEmpty()
        val envUrl = BuildConfig.DEFAULT_SUPABASE_URL.trim()
        val envKey = BuildConfig.DEFAULT_SUPABASE_KEY.trim()
        val finalUrl = storedUrl.ifBlank { envUrl.ifBlank { FALLBACK_SUPABASE_URL } }
        val finalKey = storedKey.ifBlank { envKey.ifBlank { FALLBACK_SUPABASE_KEY } }
        SupabaseConfig(url = finalUrl.trimEnd('/'), key = finalKey)
    }

    val sessionFlow: Flow<AuthSession?> = context.dataStore.data.map { prefs ->
        val token = prefs[KEY_ACCESS_TOKEN]
        val refresh = prefs[KEY_REFRESH_TOKEN]
        val userId = prefs[KEY_USER_ID]
        val email = prefs[KEY_USER_EMAIL]
        if (!token.isNullOrBlank() && !userId.isNullOrBlank()) {
            AuthSession(
                accessToken = token,
                refreshToken = refresh.orEmpty(),
                userId = userId,
                email = email.orEmpty()
            )
        } else {
            null
        }
    }

    suspend fun getLanguageCode(): String = languageFlow.first()

    suspend fun saveLanguageCode(languageCode: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_APP_LANGUAGE] = languageCode.trim().lowercase().ifBlank { "en" }
        }
    }

    suspend fun getConfig(): SupabaseConfig = configFlow.first()

    suspend fun getSession(): AuthSession? = sessionFlow.first()

    suspend fun saveConfig(url: String, key: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SB_URL] = url.trim().trimEnd('/')
            prefs[KEY_SB_KEY] = key.trim()
        }
    }

    suspend fun clearConfig() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_SB_URL)
            prefs.remove(KEY_SB_KEY)
            prefs.remove(KEY_ACCESS_TOKEN)
            prefs.remove(KEY_REFRESH_TOKEN)
            prefs.remove(KEY_USER_ID)
            prefs.remove(KEY_USER_EMAIL)
        }
    }

    suspend fun saveSession(session: AuthSession) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = session.accessToken
            prefs[KEY_REFRESH_TOKEN] = session.refreshToken
            prefs[KEY_USER_ID] = session.userId
            prefs[KEY_USER_EMAIL] = session.email
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_ACCESS_TOKEN)
            prefs.remove(KEY_REFRESH_TOKEN)
            prefs.remove(KEY_USER_ID)
            prefs.remove(KEY_USER_EMAIL)
        }
    }

    suspend fun saveDefaultPaymentInstructions(workspaceId: String, instructions: String) {
        context.dataStore.edit { prefs ->
            prefs[paymentInstructionsKey(workspaceId)] = instructions
        }
    }

    suspend fun getDefaultPaymentInstructions(workspaceId: String): String {
        return context.dataStore.data.first()[paymentInstructionsKey(workspaceId)].orEmpty()
    }
}
