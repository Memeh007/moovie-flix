package com.moovieflix.tv.core.auth

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.moovieflix.tv.domain.model.AuthState
import com.moovieflix.tv.locallibrary.MoovieFlixSharedSources
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Keeps a paired TV connected to its PC while the app is in the foreground:
 * - re-finds the PC by LAN broadcast when its address changed (DHCP) and restarts the app on the
 *   next start/resume so the backend client is rebuilt with the new address;
 * - signs in again with the paired credentials whenever there is no session (PC was off or
 *   asleep, refresh failed, first launch after pairing while the PC was offline);
 * - refreshes the PC's shared My Movies folders.
 * Sync of addons, library, progress etc. stays with the normal sync services; this only makes
 * sure they have a reachable server and a session.
 */
object MoovieFlixConnectionKeeper {
    private const val TAG = "MoovieFlixConnection"
    private const val HEALTHY_INTERVAL_MS = 120_000L
    private const val MIN_RETRY_MS = 15_000L
    private const val MAX_RETRY_MS = 300_000L

    @Volatile private var pendingRestart = false

    fun attach(activity: ComponentActivity, authManager: AuthManager, client: SupabaseClient) {
        activity.lifecycleScope.launch {
            activity.repeatOnLifecycle(Lifecycle.State.STARTED) {
                var first = true
                var retry = MIN_RETRY_MS
                while (isActive) {
                    val healthy = runCatching { check(activity, authManager, client, allowRestart = first) }
                        .onFailure { Log.w(TAG, "Connection check failed: ${it.message}") }
                        .getOrDefault(false)
                    first = false
                    if (healthy) { retry = MIN_RETRY_MS; delay(HEALTHY_INTERVAL_MS) }
                    else { delay(retry); retry = (retry * 2).coerceAtMost(MAX_RETRY_MS) }
                }
            }
        }
    }

    /** Returns true when the TV is signed in to a reachable server (or needs nothing). */
    private suspend fun check(activity: ComponentActivity, authManager: AuthManager, client: SupabaseClient, allowRestart: Boolean): Boolean {
        val paired = MoovieFlixPairing.credentials(activity) != null
        if (paired) {
            if (MoovieFlixPairing.refreshAddressIfMoved(activity)) {
                Log.i(TAG, "Paired PC moved to a new address")
                pendingRestart = true
            }
            if (pendingRestart && allowRestart) {
                pendingRestart = false
                MoovieFlixPairing.restartApp(activity)
                return false
            }
            if (!MoovieFlixPairing.isBackendReachable(activity)) {
                Log.d(TAG, "Paired PC not reachable yet")
                return false
            }
        }
        if (!MoovieFlixLocalAutoAuth.isEnabled) return true
        MoovieFlixLocalAutoAuth.ensureSignedIn(authManager, client)
        val signedIn = authManager.authState.value is AuthState.FullAccount
        if (signedIn) MoovieFlixSharedSources.sync(activity, client)
        return signedIn
    }
}
