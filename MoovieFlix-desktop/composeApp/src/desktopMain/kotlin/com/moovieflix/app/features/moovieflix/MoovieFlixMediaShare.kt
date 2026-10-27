package com.moovieflix.app.features.moovieflix

import com.moovieflix.app.core.network.SupabaseProvider
import com.moovieflix.app.core.storage.DesktopStorage
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.addJsonObject
import java.io.File
import java.io.RandomAccessFile
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.SecureRandom
import java.util.concurrent.Executors

/**
 * Moovie Flix Media Share (optional, off by default).
 *
 * Serves the My Movies folders read-only over HTTP on the LAN as a plain directory listing under a
 * random token path (http://<pc-ip>:<port>/<token>/), which the Android TV app's existing HTTP folder
 * scanner understands. The share URL is published for the signed-in account in
 * public.moovieflix_library_sources so the TV adds it automatically.
 */
object MoovieFlixMediaShare : MoovieFlixMediaShareControl {
    override fun isEnabled() = enabled

    private val store by lazy { DesktopStorage.store("moovieflix_media_share") }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val PREFERRED_PORTS = listOf(47631, 47632, 47633, 47641, 0)
    private val VIDEO_MIME = mapOf("mp4" to "video/mp4", "m4v" to "video/mp4", "mkv" to "video/x-matroska", "webm" to "video/webm", "avi" to "video/x-msvideo", "mov" to "video/quicktime")
    private var server: HttpServer? = null

    val enabled: Boolean get() = store.getString("enabled") == "true"
    val shareUrl: String? get() = server?.let { "http://${lanAddress()}:${it.address.port}/${token()}/" }

    private fun token(): String = store.getString("token") ?: ByteArray(18).also { SecureRandom().nextBytes(it) }
        .joinToString("") { "%02x".format(it) }.also { store.putString("token", it) }

    private fun folders(): List<File> = MoovieFlixDesktopLocalLibrary.loadFolders().map(::File).filter { it.isDirectory }

    fun startIfEnabled() { if (enabled) scope.launch { start() } }

    override fun setEnabled(on: Boolean) {
        store.putString("enabled", on.toString())
        scope.launch { if (on) start() else { stop(); publish(emptyList()) } }
    }

    fun onFoldersChanged() { if (enabled) scope.launch { publishWithRetry() } }

    @Synchronized
    private fun start() {
        if (server == null) {
            val savedPort = store.getString("port")?.toIntOrNull()
            for (p in listOfNotNull(savedPort) + PREFERRED_PORTS) {
                val s = runCatching { HttpServer.create(InetSocketAddress(p), 0) }.getOrNull() ?: continue
                s.createContext("/") { ex -> runCatching { handle(ex) }.onFailure { runCatching { ex.sendResponseHeaders(500, -1); ex.close() } } }
                s.executor = Executors.newFixedThreadPool(6) { r -> Thread(r, "moovieflix-share").apply { isDaemon = true } }
                s.start(); server = s
                store.putString("port", s.address.port.toString())
                break
            }
        }
        scope.launch { publishWithRetry() }
    }

    @Synchronized
    private fun stop() { server?.stop(0); server = null }

    private suspend fun publishWithRetry() {
        repeat(20) {
            val url = shareUrl
            if (url != null && publish(listOf(url))) return
            delay(15_000)
        }
    }

    /** Upserts the share URL list for the signed-in account. */
    private suspend fun publish(urls: List<String>): Boolean = runCatching {
        val userId = SupabaseProvider.client.auth.currentUserOrNull()?.id ?: return false
        val row = buildJsonObject {
            put("user_id", userId)
            putJsonArray("sources") { urls.forEach { u -> addJsonObject { put("name", "PC: " + (System.getenv("COMPUTERNAME") ?: "Desktop")); put("url", u) } } }
        }
        SupabaseProvider.client.postgrest.from("moovieflix_library_sources").upsert(row)
        true
    }.getOrDefault(false)

    private fun lanAddress(): String = runCatching {
        NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp && !it.isLoopback && !it.isVirtual && !it.displayName.contains("virtual", true) && !it.displayName.contains("vEthernet", true) && !it.displayName.contains("WSL", true) }
            .flatMap { it.inetAddresses.toList() }.filterIsInstance<Inet4Address>()
            .sortedBy { if (it.hostAddress.startsWith("192.168.")) 0 else if (it.hostAddress.startsWith("10.")) 1 else 2 }
            .firstOrNull { it.isSiteLocalAddress }?.hostAddress
    }.getOrNull() ?: "127.0.0.1"

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8").replace("+", "%20")

    private fun handle(ex: HttpExchange) {
        if (ex.requestMethod != "GET" && ex.requestMethod != "HEAD") { ex.sendResponseHeaders(405, -1); ex.close(); return }
        val segs = ex.requestURI.rawPath.split('/').filter { it.isNotEmpty() }.map { URLDecoder.decode(it, "UTF-8") }
        if (segs.isEmpty() || segs[0] != token()) { ex.sendResponseHeaders(404, -1); ex.close(); return }
        val roots = folders()
        if (segs.size == 1) {
            return listing(ex, roots.mapIndexed { i, f -> "$i-${f.name.ifBlank { "Drive" }}/" })
        }
        val root = roots.getOrNull(segs[1].substringBefore('-').toIntOrNull() ?: -1) ?: run { ex.sendResponseHeaders(404, -1); ex.close(); return }
        if (segs.drop(2).any { it == ".." || it.startsWith(".") || it.contains('\\') || it.contains(':') }) { ex.sendResponseHeaders(403, -1); ex.close(); return }
        val target = File(root, segs.drop(2).joinToString(File.separator)).canonicalFile
        if (!target.path.startsWith(root.canonicalPath)) { ex.sendResponseHeaders(403, -1); ex.close(); return }
        when {
            target.isDirectory -> listing(ex, target.listFiles().orEmpty().filter { !it.name.startsWith(".") && (it.isDirectory || LocalMediaParser.isVideo(it.name)) }
                .sortedBy { it.name.lowercase() }.map { if (it.isDirectory) it.name + "/" else it.name })
            target.isFile && LocalMediaParser.isVideo(target.name) -> sendFile(ex, target)
            else -> { ex.sendResponseHeaders(404, -1); ex.close() }
        }
    }

    private fun listing(ex: HttpExchange, names: List<String>) {
        val html = buildString {
            append("<html><head><meta charset=\"utf-8\"><title>Moovie Flix Media Share</title></head><body><ul>")
            names.forEach { n -> val href = if (n.endsWith("/")) enc(n.dropLast(1)) + "/" else enc(n); append("<li><a href=\"$href\">${n.replace("<", "&lt;")}</a></li>") }
            append("</ul></body></html>")
        }.toByteArray()
        ex.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
        ex.sendResponseHeaders(200, if (ex.requestMethod == "HEAD") -1 else html.size.toLong())
        if (ex.requestMethod != "HEAD") ex.responseBody.use { it.write(html) } else ex.close()
    }

    private fun sendFile(ex: HttpExchange, f: File) {
        val len = f.length()
        val range = ex.requestHeaders.getFirst("Range")?.let { Regex("""bytes=(\d*)-(\d*)""").find(it) }
        var start = 0L; var end = len - 1; var status = 200
        if (range != null) {
            val (a, b) = range.destructured
            if (a.isEmpty() && b.isNotEmpty()) { start = maxOf(0, len - b.toLong()) } else { start = a.toLongOrNull() ?: 0; if (b.isNotEmpty()) end = minOf(b.toLong(), len - 1) }
            if (start > end || start >= len) { ex.responseHeaders.add("Content-Range", "bytes */$len"); ex.sendResponseHeaders(416, -1); ex.close(); return }
            status = 206; ex.responseHeaders.add("Content-Range", "bytes $start-$end/$len")
        }
        ex.responseHeaders.add("Accept-Ranges", "bytes")
        ex.responseHeaders.add("Content-Type", VIDEO_MIME[f.extension.lowercase()] ?: "application/octet-stream")
        val count = end - start + 1
        if (ex.requestMethod == "HEAD") { ex.responseHeaders.add("Content-Length", count.toString()); ex.sendResponseHeaders(status, -1); ex.close(); return }
        ex.sendResponseHeaders(status, count)
        RandomAccessFile(f, "r").use { raf ->
            raf.seek(start); val buf = ByteArray(256 * 1024); var left = count
            ex.responseBody.use { out -> while (left > 0) { val n = raf.read(buf, 0, minOf(buf.size.toLong(), left).toInt()); if (n < 0) break; out.write(buf, 0, n); left -= n } }
        }
    }
}
