package com.moovieflix.app.core.network

/**
 * Runtime connection details of the embedded Moovie Flix sync server (desktop only).
 * Set by the desktop entry point before the Supabase client is created; empty elsewhere.
 * Nothing here is baked into the build: every install generates its own values on first launch.
 */
object MoovieFlixEmbeddedBackend {
    var backendUrl: String = ""; private set
    var publishableKey: String = ""; private set
    var ownerEmail: String = ""; private set
    var ownerPassword: String = ""; private set

    val isActive: Boolean get() = backendUrl.isNotBlank() && publishableKey.isNotBlank()

    fun configure(backendUrl: String, publishableKey: String, ownerEmail: String, ownerPassword: String) {
        this.backendUrl = backendUrl
        this.publishableKey = publishableKey
        this.ownerEmail = ownerEmail
        this.ownerPassword = ownerPassword
    }
}
