package com.moovieflix.app.core.auth

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.first

/**
 * MoovieFlixLocalAutoAuth
 *
 * Moovie Flix talks to the owner's private self-hosted backend, so the app signs in silently
 * to one local owner account (shared with the TV app so sync keeps working) instead of showing
 * the sign-in screen. Credentials come only from local.properties
 * (MOOVIEFLIX_LOCAL_AUTO_AUTH_EMAIL / _PASSWORD) via generated MoovieFlixLocalAutoAuthConfig.
 * Uses the existing AuthRepository.signInWithEmail path; no other behaviour is changed.
 */
object MoovieFlixLocalAutoAuth {
    private val log = Logger.withTag("MoovieFlixLocalAutoAuth")

    // Embedded server (per-install random owner account) wins; otherwise local.properties values.
    private val email: String
        get() = com.moovieflix.app.core.network.MoovieFlixEmbeddedBackend.ownerEmail.ifBlank { MoovieFlixLocalAutoAuthConfig.EMAIL }
    private val password: String
        get() = com.moovieflix.app.core.network.MoovieFlixEmbeddedBackend.ownerPassword.ifBlank { MoovieFlixLocalAutoAuthConfig.PASSWORD }

    val isEnabled: Boolean
        get() = email.isNotBlank() && password.isNotBlank()

    suspend fun ensureSignedIn() {
        if (!isEnabled) return
        val state = AuthRepository.state.first { it !is AuthState.Loading }
        if (state is AuthState.Authenticated && !state.isAnonymous) return
        AuthRepository.signInWithEmail(email, password)
            .onFailure { log.w(it) { "Local auto sign-in failed" } }
    }
}
