package com.moovieflix.tv.core.auth

import android.util.Log
import com.moovieflix.tv.BuildConfig
import com.moovieflix.tv.domain.model.AuthState
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.first

/**
 * MoovieFlixLocalAutoAuth
 *
 * Moovie Flix runs against the owner's private self-hosted backend, so the app signs in
 * silently to a single local owner account instead of asking for credentials.
 * Credentials come only from build config (MOOVIEFLIX_LOCAL_AUTO_AUTH_* in local.properties /
 * local.dev.properties). It reuses the existing AuthManager.signInWithEmail path; nothing
 * else (sync, profiles, addons, debrid) is touched. Leave the properties empty to disable.
 */
object MoovieFlixLocalAutoAuth {
    private const val TAG = "MoovieFlixLocalAutoAuth"

    private val hasPassword: Boolean
        get() = BuildConfig.LOCAL_AUTO_AUTH_EMAIL.isNotBlank() && BuildConfig.LOCAL_AUTO_AUTH_PASSWORD.isNotBlank()

    /** Credentials this TV got when pairing with the PC's embedded sync server (per install). */
    private val paired: MoovieFlixPairing.Credentials?
        get() = MoovieFlixPairing.cached

    val isEnabled: Boolean
        get() = paired != null || hasPassword || BuildConfig.LOCAL_AUTO_AUTH_USERNAME.isNotBlank()

    suspend fun ensureSignedIn(authManager: AuthManager, client: io.github.jan.supabase.SupabaseClient? = null) {
        if (!isEnabled) return
        val state = authManager.authState.first { it !is AuthState.Loading }
        if (state is AuthState.FullAccount) return
        paired?.let { creds ->
            authManager.signInWithEmail(creds.email, creds.password)
                .onFailure { Log.w(TAG, "Paired sign-in failed: ${it.message}") }
            return
        }
        val ok = hasPassword && authManager.signInWithEmail(BuildConfig.LOCAL_AUTO_AUTH_EMAIL, BuildConfig.LOCAL_AUTO_AUTH_PASSWORD)
            .onFailure { Log.w(TAG, "Local auto sign-in failed: ${it.message}") }.isSuccess
        if (!ok && client != null && BuildConfig.LOCAL_AUTO_AUTH_USERNAME.isNotBlank()) {
            signInWithUsername(client, BuildConfig.LOCAL_AUTO_AUTH_USERNAME)
                .onFailure { Log.w(TAG, "Username sign-in failed: ${it.message}") }
        }
    }

    /** LAN-only fallback: the backend's moovieflix-username-login function turns a username into a session. */
    suspend fun signInWithUsername(client: io.github.jan.supabase.SupabaseClient, username: String): Result<Unit> = runCatching {
        val body = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val conn = (java.net.URL(BuildConfig.SUPABASE_URL.trimEnd('/') + "/functions/v1/moovieflix-username-login").openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "POST"; doOutput = true; connectTimeout = 10_000; readTimeout = 20_000
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
            }
            try {
                conn.outputStream.use { it.write(org.json.JSONObject().put("username", username).toString().toByteArray()) }
                if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode}")
                conn.inputStream.bufferedReader().use { it.readText() }
            } finally { conn.disconnect() }
        }
        val result = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
            .decodeFromString<com.moovieflix.tv.data.remote.supabase.TvLoginExchangeResult>(body)
        client.auth.importTokenResponse(result)
    }
}
