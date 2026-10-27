package com.moovieflix.tv.core.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.util.Log
import com.moovieflix.tv.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.net.URL

/**
 * Moovie Flix TV pairing with the desktop app's embedded sync server.
 *
 * - Finds the PC on the LAN with a UDP broadcast (no URL typing).
 * - Pairs with the 6-digit code shown on the PC; the PC returns this TV's own revocable
 *   password for the household account. Address + key + credentials are stored in prefs.
 * - Builds that set MOOVIEFLIX_SUPABASE_URL (e.g. a private Docker backend) skip pairing.
 */
object MoovieFlixPairing {
    private const val TAG = "MoovieFlixPairing"
    private const val PREFS = "moovieflix_pairing"
    private const val SERVER_PREFS = "server_configuration" // same file ServerConfigurationStore reads
    const val DISCOVERY_PORT = 47630
    private const val PROBE = "MOOVIEFLIX_DISCOVER_V1"

    data class Found(val url: String, val name: String)
    data class Credentials(val backendUrl: String, val email: String, val password: String)

    @Volatile var cached: Credentials? = null; private set

    fun credentials(context: Context): Credentials? {
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val url = p.getString("backend_url", null).orEmpty(); val email = p.getString("email", null).orEmpty(); val pw = p.getString("password", null).orEmpty()
        return if (url.isNotBlank() && email.isNotBlank() && pw.isNotBlank()) Credentials(url, email, pw) else null
    }

    /** True when this build has no fixed backend and the TV has not been paired yet. */
    fun needsPairing(context: Context): Boolean {
        cached = credentials(context)
        if (BuildConfig.SUPABASE_URL.isNotBlank()) return false
        val serverPrefs = context.applicationContext.getSharedPreferences(SERVER_PREFS, Context.MODE_PRIVATE)
        return !serverPrefs.getBoolean("custom_enabled", false) || cached == null
    }

    fun openPairingScreen(activity: Activity) {
        activity.startActivity(Intent(activity, com.moovieflix.tv.ui.moovieflix.MoovieFlixPairActivity::class.java))
        activity.finish()
    }

    /** Broadcasts a discovery probe and collects replies for [timeoutMs]. */
    suspend fun discover(context: Context, timeoutMs: Int = 2500): List<Found> = withContext(Dispatchers.IO) {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val lock = runCatching { wifi?.createMulticastLock("moovieflix-discovery")?.apply { setReferenceCounted(false); acquire() } }.getOrNull()
        val found = linkedMapOf<String, Found>()
        try {
            DatagramSocket().use { s ->
                s.broadcast = true; s.soTimeout = 400
                val probe = PROBE.toByteArray()
                val targets = buildList {
                    add(InetAddress.getByName("255.255.255.255"))
                    subnetBroadcast(wifi)?.let(::add)
                }
                val end = System.currentTimeMillis() + timeoutMs
                var nextSend = 0L
                val buf = ByteArray(1024)
                while (System.currentTimeMillis() < end) {
                    if (System.currentTimeMillis() >= nextSend) {
                        targets.forEach { t -> runCatching { s.send(DatagramPacket(probe, probe.size, t, DISCOVERY_PORT)) } }
                        nextSend = System.currentTimeMillis() + 800
                    }
                    try {
                        val pkt = DatagramPacket(buf, buf.size); s.receive(pkt)
                        val obj = JSONObject(String(pkt.data, 0, pkt.length, Charsets.UTF_8))
                        if (obj.optString("service") == "moovieflix") {
                            val url = "http://${pkt.address.hostAddress}:${obj.optInt("port", DISCOVERY_PORT)}"
                            found[url] = Found(url, obj.optString("name", "PC"))
                        }
                    } catch (_: SocketTimeoutException) { } catch (e: Exception) { Log.d(TAG, "bad reply: ${e.message}") }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Discovery failed: ${e.message}")
        } finally { runCatching { lock?.release() } }
        found.values.toList()
    }

    private fun subnetBroadcast(wifi: WifiManager?): InetAddress? = runCatching {
        @Suppress("DEPRECATION") val dhcp = wifi?.dhcpInfo ?: return null
        if (dhcp.ipAddress == 0) return null
        val b = (dhcp.ipAddress and dhcp.netmask) or dhcp.netmask.inv()
        InetAddress.getByAddress(byteArrayOf((b and 0xff).toByte(), (b shr 8 and 0xff).toByte(), (b shr 16 and 0xff).toByte(), (b shr 24 and 0xff).toByte()))
    }.getOrNull()

    fun normalizeAddress(input: String): String {
        var s = input.trim().trimEnd('/')
        if (!s.contains("://")) s = "http://$s"
        val afterScheme = s.substringAfter("://")
        if (!afterScheme.contains(':')) s = "$s:$DISCOVERY_PORT"
        return s
    }

    /** Pairs with the PC at [serverUrl] using [code]; stores everything on success. */
    suspend fun pair(context: Context, serverUrl: String, code: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val base = normalizeAddress(serverUrl)
            val body = JSONObject().put("code", code.filter { it.isDigit() }).put("device_name", android.os.Build.MODEL ?: "TV").toString()
            val conn = (URL("$base/moovieflix/v1/pair").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"; doOutput = true; connectTimeout = 8000; readTimeout = 15000
                setRequestProperty("Content-Type", "application/json")
            }
            val text = try {
                conn.outputStream.use { it.write(body.toByteArray()) }
                val ok = conn.responseCode in 200..299
                val t = (if (ok) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (!ok) error(runCatching { JSONObject(t).optString("message") }.getOrNull()?.ifBlank { null } ?: "HTTP ${conn.responseCode}")
                t
            } finally { conn.disconnect() }
            val r = JSONObject(text)
            val backend = r.getString("backend_url").trimEnd('/')
            save(context, backend, r.getString("publishable_key"), r.getString("email"), r.getString("password"))
        }
    }

    private fun save(context: Context, backend: String, key: String, email: String, password: String) {
        val app = context.applicationContext
        check(app.getSharedPreferences(SERVER_PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean("custom_enabled", true).putString("backend_url", backend).putString("publishable_key", key)
            .putBoolean("email_password_auth", true).putBoolean("tv_login", false).putBoolean("watch_party", false)
            .putString("discovery_url", "$backend/.well-known/nuvio").commit()) { "Could not save server settings" }
        check(app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("backend_url", backend).putString("publishable_key", key).putString("email", email).putString("password", password).commit()) { "Could not save pairing" }
        cached = Credentials(backend, email, password)
    }

    /**
     * If the paired PC changed its LAN address (DHCP), find it again by broadcast and keep the
     * pairing when the server's key matches. Returns true when the address was updated.
     */
    suspend fun refreshAddressIfMoved(context: Context): Boolean = withContext(Dispatchers.IO) {
        val creds = credentials(context) ?: return@withContext false
        if (reachable(creds.backendUrl)) return@withContext false
        val key = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("publishable_key", null) ?: return@withContext false
        for (f in discover(context)) {
            val doc = runCatching { URL("${f.url}/.well-known/nuvio").readText() }.getOrNull() ?: continue
            if (runCatching { JSONObject(doc).optString("publishable_key") }.getOrNull() == key) {
                save(context, f.url, key, creds.email, creds.password)
                return@withContext true
            }
        }
        false
    }

    private fun reachable(url: String): Boolean = runCatching {
        val c = (URL("$url/health").openConnection() as HttpURLConnection).apply { connectTimeout = 3000; readTimeout = 3000 }
        try { c.responseCode in 200..299 } finally { c.disconnect() }
    }.getOrDefault(false)

    /** Restart the app process so the backend client is rebuilt with the paired server. */
    fun restartApp(context: Context) {
        val app = context.applicationContext
        val launch = app.packageManager.getLeanbackLaunchIntentForPackage(app.packageName) ?: app.packageManager.getLaunchIntentForPackage(app.packageName) ?: return
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        val pi = android.app.PendingIntent.getActivity(app, 8843, launch, android.app.PendingIntent.FLAG_CANCEL_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
        (app.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager).set(android.app.AlarmManager.ELAPSED_REALTIME, android.os.SystemClock.elapsedRealtime() + 400, pi)
        android.os.Process.killProcess(android.os.Process.myPid())
        kotlin.system.exitProcess(0)
    }
}
