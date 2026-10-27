package com.moovieflix.app.server

import co.touchlab.kermit.Logger
import com.moovieflix.app.core.network.MoovieFlixEmbeddedBackend
import com.moovieflix.app.core.network.SupabaseConfig
import com.moovieflix.app.core.storage.DesktopStorage
import com.moovieflix.app.features.moovieflix.MoovieFlixHooks
import com.moovieflix.app.features.moovieflix.MoovieFlixTvPairingControl
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Starts the embedded Moovie Flix sync server inside the desktop app (no Docker needed).
 *
 * Active when the build has no MOOVIEFLIX_SUPABASE_URL (the default for shared builds), or when
 * forced with -Dmoovieflix.embeddedServer=true / MOOVIEFLIX_EMBEDDED_SERVER=true. Builds that set
 * MOOVIEFLIX_SUPABASE_URL in local.properties keep using that backend unchanged.
 */
object MoovieFlixEmbeddedServer : MoovieFlixTvPairingControl {
    private val log = Logger.withTag("MoovieFlixEmbeddedServer")
    private var server: MoovieFlixSyncServer? = null
    private var discovery: MoovieFlixLanDiscovery? = null

    val shouldRun: Boolean
        get() {
            val forced = (System.getProperty("moovieflix.embeddedServer") ?: System.getenv("MOOVIEFLIX_EMBEDDED_SERVER"))?.trim()?.lowercase()
            if (forced == "false") return false
            return forced == "true" || SupabaseConfig.URL.isBlank()
        }

    fun dataDir(): File = File(DesktopStorage.rootDir.toFile(), "server")

    fun startIfNeeded() {
        if (!shouldRun || server != null) return
        runCatching {
            val s = MoovieFlixSyncServer(dataDir())
            val port = s.start()
            server = s
            MoovieFlixEmbeddedBackend.configure(
                backendUrl = s.localUrl,
                publishableKey = s.anonKey,
                ownerEmail = s.ownerEmail,
                ownerPassword = s.ownerPassword,
            )
            MoovieFlixHooks.tvPairing = this
            discovery = MoovieFlixLanDiscovery(port).also { it.start() }
            log.i { "Embedded sync server on port $port" }
        }.onFailure { log.e(it) { "Embedded sync server failed to start" } }
    }

    override fun pairingCode(): String? = server?.pairingCode()
    override fun serverAddress(): String? = server?.let { "http://${lanAddress()}:${it.port}" }
    override fun pairedDevices(): List<String> = server?.pairedDevices().orEmpty()

    internal fun lanAddress(): String = runCatching {
        NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp && !it.isLoopback && !it.isVirtual && !it.displayName.contains("virtual", true) && !it.displayName.contains("vEthernet", true) && !it.displayName.contains("WSL", true) }
            .flatMap { it.inetAddresses.toList() }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { it.isSiteLocalAddress }?.hostAddress
    }.getOrNull() ?: "127.0.0.1"
}
