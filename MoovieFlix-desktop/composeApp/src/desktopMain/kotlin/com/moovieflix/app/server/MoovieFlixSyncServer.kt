package com.moovieflix.app.server

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.io.File
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import java.util.UUID
import java.util.concurrent.Executors
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Moovie Flix embedded sync server.
 *
 * A small, dependency-free (JDK HttpServer + kotlinx.serialization) stand-in for the subset of
 * Supabase (GoTrue auth, PostgREST tables + RPCs, edge functions) that the Moovie Flix desktop and
 * TV apps call. It replaces the Docker backend for normal installs: every install generates its own
 * random owner account, JWT secret and keys on first start (stored in [dataDir]); nothing is
 * hardcoded. Single owner account per install; TVs pair with a short code and get their own
 * revocable device password for the same account.
 *
 * Unsupported on purpose (fail gracefully): realtime websockets (404), watch party (disabled),
 * QR/TV web login (PGRST202 "function not found"), storage uploads (404), account deletion.
 */
class MoovieFlixSyncServer(val dataDir: File) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = true }
    private val random = SecureRandom()
    private val lock = Any()
    private lateinit var secrets: JsonObject
    private val tables = linkedMapOf<String, MutableList<JsonObject>>()
    private var eventSeq = 0L
    private var server: HttpServer? = null
    @Volatile var port: Int = 0; private set

    // ---------- public API ----------
    val ownerId: String get() = secrets.str("owner_id")!!
    val ownerEmail: String get() = secrets.str("owner_email")!!
    val ownerPassword: String get() = secrets.str("owner_password")!!
    val anonKey: String get() = secrets.str("anon_key")!!
    val localUrl: String get() = "http://127.0.0.1:$port"

    fun start(preferredPorts: List<Int> = listOf(47630, 47640, 47650, 0), bindAddress: String = "0.0.0.0"): Int {
        load()
        var last: Exception? = null
        for (p in preferredPorts) {
            try {
                val s = HttpServer.create(InetSocketAddress(bindAddress, p), 64)
                s.createContext("/") { ex -> safeHandle(ex) }
                s.executor = Executors.newFixedThreadPool(8) { r -> Thread(r, "moovieflix-sync").apply { isDaemon = true } }
                s.start()
                server = s; port = s.address.port
                return port
            } catch (e: Exception) { last = e }
        }
        throw IllegalStateException("Could not start sync server", last)
    }

    fun stop() { server?.stop(0); server = null }

    // ---------- pairing ----------
    private var pairCode: String? = null
    private var pairExpires = 0L
    private var pairFailures = 0

    /** Current 6-digit TV pairing code (created on demand, valid 15 minutes). */
    fun pairingCode(): String = synchronized(lock) {
        val now = System.currentTimeMillis()
        if (pairCode == null || now > pairExpires) {
            pairCode = (100000 + random.nextInt(900000)).toString(); pairExpires = now + 15 * 60_000; pairFailures = 0
        }
        pairCode!!
    }

    fun pairedDevices(): List<String> = synchronized(lock) { devices().map { it.str("name") ?: "TV" } }

    private fun devices(): List<JsonObject> = (secrets["devices"] as? JsonArray)?.map { it.jsonObject } ?: emptyList()

    // ---------- persistence ----------
    private val secretsFile get() = File(dataDir, "server-secrets.json")
    private val dbFile get() = File(dataDir, "server-data.json")

    private fun load() = synchronized(lock) {
        dataDir.mkdirs()
        secrets = if (secretsFile.exists()) json.parseToJsonElement(secretsFile.readText()).jsonObject else newSecrets().also { writeAtomic(secretsFile, it.toString()) }
        if (dbFile.exists()) {
            val db = json.parseToJsonElement(dbFile.readText()).jsonObject
            eventSeq = db["event_seq"]?.jsonPrimitive?.longOrNull ?: 0L
            (db["tables"] as? JsonObject)?.forEach { (k, v) -> tables[k] = v.jsonArray.map { it.jsonObject }.toMutableList() }
        }
    }

    private fun newSecrets(): JsonObject {
        val jwtSecret = token(48)
        val now = Instant.now().epochSecond
        val anon = signJwt(JsonObject(mapOf("iss" to s("moovieflix-local"), "role" to s("anon"), "iat" to n(now), "exp" to n(now + 20L * 365 * 86400))), jwtSecret)
        return JsonObject(mapOf(
            "version" to n(1),
            "jwt_secret" to s(jwtSecret),
            "anon_key" to s(anon),
            "owner_id" to s(UUID.randomUUID().toString()),
            "owner_email" to s("owner-${token(6).lowercase()}@moovieflix.invalid"),
            "owner_password" to s(token(24)),
            "created_at" to s(Instant.now().toString()),
            "devices" to JsonArray(emptyList()),
            "refresh_tokens" to JsonObject(emptyMap()),
        ))
    }

    private fun persistSecrets() = writeAtomic(secretsFile, secrets.toString())

    private fun persistDb() {
        val db = JsonObject(mapOf("event_seq" to n(eventSeq), "tables" to JsonObject(tables.mapValues { JsonArray(it.value) })))
        writeAtomic(dbFile, db.toString())
    }

    private fun writeAtomic(f: File, text: String) {
        val tmp = File(f.parentFile, f.name + ".tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(f)) { f.delete(); tmp.renameTo(f) }
    }

    private fun table(name: String) = tables.getOrPut(name) { mutableListOf() }

    // ---------- JWT ----------
    private fun b64(b: ByteArray) = Base64.getUrlEncoder().withoutPadding().encodeToString(b)
    private fun hmac(data: String, secret: String): ByteArray = Mac.getInstance("HmacSHA256").run { init(SecretKeySpec(secret.toByteArray(), "HmacSHA256")); doFinal(data.toByteArray()) }
    private fun signJwt(claims: JsonObject, secret: String = secrets.str("jwt_secret")!!): String {
        val head = b64("""{"alg":"HS256","typ":"JWT"}""".toByteArray())
        val body = b64(claims.toString().toByteArray())
        return "$head.$body.${b64(hmac("$head.$body", secret))}"
    }
    private fun verifyJwt(token: String): JsonObject? = runCatching {
        val p = token.split('.'); if (p.size != 3) return null
        val expected = b64(hmac("${p[0]}.${p[1]}", secrets.str("jwt_secret")!!))
        if (!MessageDigest.isEqual(expected.toByteArray(), p[2].toByteArray())) return null
        val c = json.parseToJsonElement(String(Base64.getUrlDecoder().decode(p[1]))).jsonObject
        val exp = c["exp"]?.jsonPrimitive?.longOrNull ?: 0
        if (exp < Instant.now().epochSecond) null else c
    }.getOrNull()

    private fun token(bytes: Int) = ByteArray(bytes).also(random::nextBytes).let { b64(it) }.replace("-", "x").replace("_", "y")
    private fun sha(s: String) = MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }

    // ---------- HTTP plumbing ----------
    private class Req(val ex: HttpExchange, val method: String, val path: String, val query: List<Pair<String, String>>, val body: String) {
        fun header(n: String): String? = ex.requestHeaders.getFirst(n)
        fun q(n: String) = query.firstOrNull { it.first == n }?.second
    }
    private class HttpError(val status: Int, val payload: JsonElement) : Exception(payload.toString())

    private fun pgError(status: Int, code: String, message: String) = HttpError(status, JsonObject(mapOf("code" to s(code), "details" to JsonNull, "hint" to JsonNull, "message" to s(message))))
    private fun authError(status: Int, code: String, message: String) = HttpError(status, JsonObject(mapOf("code" to n(status), "error_code" to s(code), "msg" to s(message), "error" to s(code), "error_description" to s(message))))

    private fun safeHandle(ex: HttpExchange) {
        try {
            ex.responseHeaders.add("Access-Control-Allow-Origin", "*")
            ex.responseHeaders.add("Access-Control-Allow-Headers", "*")
            ex.responseHeaders.add("Access-Control-Allow-Methods", "GET,POST,PATCH,PUT,DELETE,OPTIONS,HEAD")
            val raw = ex.requestURI.rawQuery.orEmpty()
            val query = raw.split('&').filter { it.isNotEmpty() }.map { kv ->
                val i = kv.indexOf('='); if (i < 0) dec(kv) to "" else dec(kv.substring(0, i)) to dec(kv.substring(i + 1))
            }
            val body = ex.requestBody.readBytes().toString(Charsets.UTF_8)
            val req = Req(ex, ex.requestMethod.uppercase(), ex.requestURI.path, query, body)
            if (req.method == "OPTIONS") return send(ex, 204, null)
            val (status, out) = route(req)
            send(ex, status, out)
        } catch (e: HttpError) {
            send(ex, e.status, e.payload)
        } catch (e: Exception) {
            send(ex, 500, JsonObject(mapOf("code" to s("XX000"), "message" to s(e.message ?: e.toString()), "details" to JsonNull, "hint" to JsonNull)))
        } finally { ex.close() }
    }

    private fun dec(s: String) = URLDecoder.decode(s, "UTF-8")

    private fun send(ex: HttpExchange, status: Int, body: JsonElement?, contentRange: String? = null) {
        if (body == null || status == 204) { ex.sendResponseHeaders(if (status == 200) 204 else status, -1); return }
        val bytes = body.toString().toByteArray()
        ex.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
        if (body is JsonArray) ex.responseHeaders.set("Content-Range", if (body.isEmpty()) "*/0" else "0-${body.size - 1}/${body.size}")
        ex.sendResponseHeaders(status, bytes.size.toLong())
        ex.responseBody.use { it.write(bytes) }
    }

    private fun route(r: Req): Pair<Int, JsonElement?> {
        val p = r.path.trimEnd('/')
        return when {
            p == "" || p == "/health" || p == "/moovieflix/v1/health" -> 200 to JsonObject(mapOf("ok" to JsonPrimitive(true), "service" to s("moovieflix")))
            p == "/.well-known/nuvio" -> 200 to discoveryDoc(r)
            p == "/moovieflix/v1/pair" && r.method == "POST" -> 200 to pair(r)
            p.startsWith("/auth/v1/") -> auth(r, p.removePrefix("/auth/v1/"))
            p.startsWith("/rest/v1/rpc/") -> rpc(r, p.removePrefix("/rest/v1/rpc/"))
            p.startsWith("/rest/v1/") -> rest(r, p.removePrefix("/rest/v1/"))
            p.startsWith("/functions/v1/") -> throw HttpError(400, JsonObject(mapOf("error" to s("This function is not available on the Moovie Flix local server."))))
            else -> throw HttpError(404, JsonObject(mapOf("message" to s("Not found"))))
        }
    }

    private fun baseUrlFor(r: Req): String {
        val host = r.header("Host")?.takeIf { it.isNotBlank() } ?: "127.0.0.1:$port"
        return "http://$host"
    }

    private fun discoveryDoc(r: Req) = JsonObject(mapOf(
        "version" to n(1), "service" to s("nuvio"), "self_hosted" to JsonPrimitive(true),
        "backend_url" to s(baseUrlFor(r)), "publishable_key" to s(anonKey),
        "moovieflix_pairing" to JsonPrimitive(true),
        "capabilities" to JsonObject(mapOf("email_password_auth" to JsonPrimitive(true), "tv_login" to JsonPrimitive(false), "watch_party" to JsonPrimitive(false))),
    ))

    private fun pair(r: Req): JsonElement = synchronized(lock) {
        val body = parseObj(r.body)
        val code = body.str("code")?.filter { it.isDigit() }.orEmpty()
        val current = pairCode
        if (current == null || System.currentTimeMillis() > pairExpires) throw pgError(400, "MF001", "No pairing code is active. Open Moovie Flix on the PC to show a code.")
        if (!MessageDigest.isEqual(code.toByteArray(), current.toByteArray())) {
            pairFailures++
            if (pairFailures >= 8) { pairCode = null }
            throw pgError(400, "MF002", "Wrong pairing code")
        }
        pairCode = null
        val deviceId = UUID.randomUUID().toString()
        val password = token(24)
        val name = body.str("device_name")?.take(80)?.ifBlank { null } ?: "TV"
        val device = JsonObject(mapOf("id" to s(deviceId), "name" to s(name), "password_sha" to s(sha(password)), "created_at" to s(Instant.now().toString())))
        secrets = JsonObject(secrets + ("devices" to JsonArray(devices() + device)))
        persistSecrets()
        table("linked_devices").add(JsonObject(mapOf("id" to s(UUID.randomUUID().toString()), "owner_id" to s(ownerId), "device_user_id" to s(deviceId), "device_name" to s(name), "linked_at" to s(Instant.now().toString()))))
        persistDb()
        JsonObject(mapOf("backend_url" to s(baseUrlFor(r)), "publishable_key" to s(anonKey), "email" to s(ownerEmail), "password" to s(password), "device_id" to s(deviceId)))
    }

    // ---------- GoTrue subset ----------
    private fun userJson(): JsonObject {
        val created = secrets.str("created_at") ?: Instant.now().toString()
        return JsonObject(mapOf(
            "id" to s(ownerId), "aud" to s("authenticated"), "role" to s("authenticated"), "email" to s(ownerEmail),
            "email_confirmed_at" to s(created), "confirmed_at" to s(created), "phone" to s(""), "last_sign_in_at" to s(Instant.now().toString()),
            "app_metadata" to JsonObject(mapOf("provider" to s("email"), "providers" to JsonArray(listOf(s("email"))))),
            "user_metadata" to JsonObject(emptyMap()), "identities" to JsonArray(emptyList()),
            "created_at" to s(created), "updated_at" to s(created), "is_anonymous" to JsonPrimitive(false),
        ))
    }

    private fun session(sessionId: String, refresh: String): JsonObject {
        val now = Instant.now().epochSecond
        val exp = now + 3600
        val access = signJwt(JsonObject(mapOf(
            "iss" to s("moovieflix-local/auth/v1"), "sub" to s(ownerId), "aud" to s("authenticated"), "exp" to n(exp), "iat" to n(now),
            "email" to s(ownerEmail), "phone" to s(""), "role" to s("authenticated"), "aal" to s("aal1"), "session_id" to s(sessionId), "is_anonymous" to JsonPrimitive(false),
            "app_metadata" to JsonObject(mapOf("provider" to s("email"))), "user_metadata" to JsonObject(emptyMap()),
        )))
        return JsonObject(mapOf("access_token" to s(access), "token_type" to s("bearer"), "expires_in" to n(3600), "expires_at" to n(exp), "refresh_token" to s(refresh), "user" to userJson()))
    }

    private fun refreshTokens(): JsonObject = (secrets["refresh_tokens"] as? JsonObject) ?: JsonObject(emptyMap())

    private fun passwordValid(pw: String): Boolean =
        MessageDigest.isEqual(pw.toByteArray(), ownerPassword.toByteArray()) || devices().any { it.str("password_sha") == sha(pw) }

    private fun deviceForPassword(pw: String): String? = devices().firstOrNull { it.str("password_sha") == sha(pw) }?.str("id")

    private fun auth(r: Req, sub: String): Pair<Int, JsonElement?> = synchronized(lock) {
        when (sub) {
            "token" -> {
                val body = parseObj(r.body)
                when (r.q("grant_type")) {
                    "password" -> {
                        val email = body.str("email")?.trim()?.lowercase()
                        val pw = body.str("password").orEmpty()
                        if (email != ownerEmail.lowercase() || !passwordValid(pw)) throw authError(400, "invalid_credentials", "Invalid login credentials")
                        val sid = UUID.randomUUID().toString(); val rt = token(32)
                        secrets = JsonObject(secrets + ("refresh_tokens" to JsonObject(refreshTokens() + (rt to JsonObject(mapOf("session_id" to s(sid), "device_id" to (deviceForPassword(pw)?.let(::s) ?: JsonNull), "created_at" to s(Instant.now().toString())))))))
                        persistSecrets()
                        200 to session(sid, rt)
                    }
                    "refresh_token" -> {
                        val rt = body.str("refresh_token").orEmpty()
                        val entry = refreshTokens()[rt]?.jsonObject ?: throw authError(400, "refresh_token_not_found", "Invalid Refresh Token: Refresh Token Not Found")
                        val dev = entry.str("device_id")
                        if (dev != null && devices().none { it.str("id") == dev }) throw authError(400, "refresh_token_not_found", "Device was unlinked")
                        200 to session(entry.str("session_id")!!, rt)
                    }
                    else -> throw authError(400, "unsupported_grant_type", "Unsupported grant type")
                }
            }
            "user" -> { requireUser(r); 200 to userJson() }
            "logout" -> {
                val c = requireUser(r)
                val sid = c.str("session_id")
                secrets = JsonObject(secrets + ("refresh_tokens" to JsonObject(refreshTokens().filterValues { it.jsonObject.str("session_id") != sid })))
                persistSecrets(); 204 to null
            }
            "settings" -> 200 to JsonObject(mapOf("disable_signup" to JsonPrimitive(true), "external" to JsonObject(mapOf("email" to JsonPrimitive(true))), "mailer_autoconfirm" to JsonPrimitive(true)))
            "health" -> 200 to JsonObject(mapOf("name" to s("moovieflix-auth")))
            "signup" -> throw authError(422, "signup_disabled", "Signups are disabled on this Moovie Flix server")
            else -> throw authError(404, "not_found", "Not supported by the Moovie Flix local server")
        }
    }

    private fun bearer(r: Req) = r.header("Authorization")?.removePrefix("Bearer ")?.removePrefix("bearer ")?.trim()

    private fun requireUser(r: Req): JsonObject {
        val c = bearer(r)?.let(::verifyJwt) ?: throw HttpError(401, JsonObject(mapOf("code" to s("PGRST301"), "message" to s("JWT expired or invalid"), "details" to JsonNull, "hint" to JsonNull)))
        if (c.str("role") != "authenticated" || c.str("sub") != ownerId) throw pgError(401, "42501", "Authentication required")
        return c
    }

    private fun requireApiKey(r: Req) {
        val key = r.header("apikey") ?: bearer(r)
        if (key == null || verifyJwt(key) == null) throw pgError(401, "PGRST301", "Invalid API key")
    }

    // ---------- PostgREST tables ----------
    private val keyColumns = mapOf(
        "addons" to listOf("user_id", "profile_id", "url"),
        "plugins" to listOf("user_id", "profile_id", "url"),
        "moovieflix_library_sources" to listOf("user_id"),
        "profiles" to listOf("user_id", "profile_index"),
        "library_items" to listOf("user_id", "profile_id", "content_id", "content_type"),
        "watch_progress" to listOf("user_id", "profile_id", "progress_key"),
        "collections" to listOf("user_id", "profile_id"),
        "media_servers" to listOf("user_id", "profile_id"),
        "provider_credentials" to listOf("user_id", "profile_id", "provider"),
        "profile_settings_blobs" to listOf("user_id", "profile_id", "platform"),
        "home_catalog_settings" to listOf("user_id", "profile_id", "platform"),
    )
    private val allowedTables = keyColumns.keys + setOf("linked_devices", "watched_items", "avatar_catalog", "profile_tracker_settings", "user_tracker_tokens", "user_session_devices")

    private fun rest(r: Req, tableName: String): Pair<Int, JsonElement?> = synchronized(lock) {
        requireApiKey(r)
        if (tableName !in allowedTables) throw pgError(404, "42P01", "relation \"public.$tableName\" does not exist")
        if (tableName == "avatar_catalog") return 200 to JsonArray(emptyList())
        requireUser(r)
        val rows = table(tableName)
        val filters = r.query.filter { it.first !in setOf("select", "order", "limit", "offset", "on_conflict", "columns") }
        val single = r.header("Accept")?.contains("vnd.pgrst.object") == true
        val wantRepr = r.header("Prefer")?.contains("return=representation") == true
        when (r.method) {
            "GET", "HEAD" -> {
                var out = rows.filter { row -> filters.all { (c, f) -> matches(row, c, f) } }
                r.q("order")?.let { out = order(out, it) }
                val off = r.q("offset")?.toIntOrNull() ?: 0
                val lim = r.q("limit")?.toIntOrNull()
                out = out.drop(off).let { if (lim != null) it.take(lim) else it }
                val projected = project(out, r.q("select"))
                if (single) { if (projected.size != 1) throw pgError(406, "PGRST116", "JSON object requested, multiple (or no) rows returned"); return 200 to projected[0] }
                200 to JsonArray(projected)
            }
            "POST" -> {
                val incoming = parseRows(r.body).map { withDefaults(tableName, it) }
                val upsert = r.header("Prefer")?.contains("resolution=") == true
                val ignore = r.header("Prefer")?.contains("resolution=ignore-duplicates") == true
                val keys = r.q("on_conflict")?.split(',')?.map { it.trim() } ?: keyColumns[tableName] ?: listOf("id")
                val saved = mutableListOf<JsonObject>()
                for (row in incoming) {
                    val idx = rows.indexOfFirst { ex -> keys.all { k -> ex[k].norm() == row[k].norm() } }
                    if (idx >= 0) {
                        if (!upsert) throw pgError(409, "23505", "duplicate key value violates unique constraint")
                        if (ignore) continue
                        val merged = JsonObject(rows[idx] + row + ("id" to (rows[idx]["id"] ?: row["id"] ?: s(UUID.randomUUID().toString()))) + ("updated_at" to s(Instant.now().toString())))
                        rows[idx] = merged; saved += merged
                    } else { rows += row; saved += row }
                }
                persistDb()
                if (wantRepr) 201 to (if (single) saved.firstOrNull() ?: JsonNull else JsonArray(saved)) else 201 to null
            }
            "PATCH" -> {
                val patch = parseObj(r.body)
                val changed = mutableListOf<JsonObject>()
                rows.indices.forEach { i -> if (filters.all { (c, f) -> matches(rows[i], c, f) }) { rows[i] = JsonObject(rows[i] + patch); changed += rows[i] } }
                persistDb()
                if (wantRepr) 200 to JsonArray(changed) else 204 to null
            }
            "DELETE" -> {
                if (filters.isEmpty()) throw pgError(400, "21000", "DELETE requires a WHERE clause")
                val removed = rows.filter { row -> filters.all { (c, f) -> matches(row, c, f) } }
                rows.removeAll(removed.toSet())
                if (tableName == "linked_devices") removed.forEach { removeDevice(it.str("device_user_id")) }
                persistDb()
                if (wantRepr) 200 to JsonArray(removed) else 204 to null
            }
            else -> throw pgError(405, "PGRST000", "Method not allowed")
        }
    }

    private fun withDefaults(t: String, row: JsonObject): JsonObject {
        val m = row.toMutableMap()
        val now = s(Instant.now().toString())
        if (t != "moovieflix_library_sources" && t != "provider_credentials" && t != "home_catalog_settings" && "id" !in m) m["id"] = s(UUID.randomUUID().toString())
        if (t == "linked_devices") { if ("owner_id" !in m) m["owner_id"] = s(ownerId) } else if ("user_id" !in m) m["user_id"] = s(ownerId)
        if ("updated_at" !in m) m["updated_at"] = now
        if (t in setOf("addons", "plugins", "profiles", "library_items", "watch_progress") && "profile_id" !in m) m["profile_id"] = n(1)
        return JsonObject(m)
    }

    private fun JsonElement?.norm(): String? = when (this) { null, JsonNull -> null; is JsonPrimitive -> this.contentOrNull; else -> this.toString() }

    private fun matches(row: JsonObject, col: String, filter: String): Boolean {
        val negate = filter.startsWith("not.")
        val f = filter.removePrefix("not.")
        val dot = f.indexOf('.'); if (dot < 0) return true
        val op = f.substring(0, dot); val v = f.substring(dot + 1)
        val cell = row[col].norm()
        val res = when (op) {
            "eq" -> cell == v || (cell != null && v.toDoubleOrNull() != null && cell.toDoubleOrNull() == v.toDoubleOrNull())
            "neq" -> cell != v
            "gt", "gte", "lt", "lte" -> {
                val a = cell?.toDoubleOrNull(); val b = v.toDoubleOrNull()
                val cmp = if (a != null && b != null) a.compareTo(b) else (cell ?: "").compareTo(v)
                when (op) { "gt" -> cmp > 0; "gte" -> cmp >= 0; "lt" -> cmp < 0; else -> cmp <= 0 }
            }
            "in" -> v.trim('(', ')').split(',').map { it.trim().trim('"') }.contains(cell)
            "is" -> if (v == "null") cell == null else cell == v
            "like", "ilike" -> cell != null && Regex(Regex.escape(v).replace("*", "\\E.*\\Q").replace("%", "\\E.*\\Q"), if (op == "ilike") setOf(RegexOption.IGNORE_CASE) else emptySet()).matches(cell)
            else -> true
        }
        return if (negate) !res else res
    }

    private fun order(rows: List<JsonObject>, spec: String): List<JsonObject> {
        var out = rows
        spec.split(',').reversed().forEach { part ->
            val bits = part.split('.'); val col = bits[0]; val desc = bits.getOrNull(1) == "desc"
            val cmp = Comparator<JsonObject> { a, b ->
                val x = a[col].norm(); val y = b[col].norm()
                val xd = x?.toDoubleOrNull(); val yd = y?.toDoubleOrNull()
                if (xd != null && yd != null) xd.compareTo(yd) else compareValues(x, y)
            }
            out = out.sortedWith(if (desc) cmp.reversed() else cmp)
        }
        return out
    }

    private fun project(rows: List<JsonObject>, select: String?): List<JsonObject> {
        if (select == null || select == "*" || select.contains('(')) return rows
        val cols = select.split(',').map { it.trim().substringAfter(':') }.filter { it.isNotEmpty() }
        return rows.map { row -> JsonObject(cols.associateWith { row[it] ?: JsonNull }) }
    }

    // ---------- RPCs ----------
    private fun rpc(r: Req, fn: String): Pair<Int, JsonElement?> = synchronized(lock) {
        requireApiKey(r)
        val a: JsonObject = if (r.method == "GET") JsonObject(r.query.associate { it.first to s(it.second) }) else parseObj(r.body)
        when (fn) {
            "health_ping" -> return 200 to JsonPrimitive(true)
            "watch_party_enabled" -> return 200 to JsonPrimitive(false)
            "watch_party_clock" -> return 200 to n(System.currentTimeMillis())
            "get_avatar_catalog", "get_member_profile_avatar_catalog", "get_member_profile_background_catalog", "get_my_member_access", "get_my_membership_overview" -> return 200 to JsonArray(emptyList())
        }
        requireUser(r)
        val pid = a.int("p_profile_id")
        val result: JsonElement? = when (fn) {
            "get_sync_owner" -> s(ownerId)
            "sync_set_origin_client_id", "record_activity_event" -> null
            "register_current_device", "register_current_session" -> JsonPrimitive(true)
            "list_my_sessions" -> JsonArray(emptyList())
            "get_sync_overview" -> syncOverview()
            "sync_push_addons" -> { replaceUrlSet("addons", a.arr("p_addons"), pid ?: 1, withRepo = false); null }
            "sync_push_plugins" -> { replaceUrlSet("plugins", a.arr("p_plugins"), pid ?: 1, withRepo = true); null }
            "sync_push_library_items" -> { pushLibrary(a.arr("p_items"), pid ?: 1, replace = false); null }
            "sync_push_library" -> { pushLibrary(a.arr("p_items"), pid ?: 1, replace = true); null }
            "sync_pull_library" -> JsonArray(ownRows("library_items", pid ?: 1).sortedByDescending { it.long("added_at") }.drop(a.int("p_offset") ?: 0).take(a.int("p_limit") ?: 500))
            "sync_delete_library_items" -> { deleteLibrary(a.arr("p_keys"), pid ?: 1); null }
            "sync_get_library_delta_cursor" -> n(cursor("library_item_events", pid ?: 1))
            "sync_pull_library_delta" -> delta("library_item_events", pid ?: 1, a)
            "sync_push_watch_progress" -> { pushProgress(a.arr("p_entries"), pid ?: 1); null }
            "sync_pull_watch_progress" -> {
                val since = a.long("p_since_last_watched"); val lim = a.int("p_limit")?.takeIf { it > 0 }
                JsonArray(ownRows("watch_progress", pid ?: 1).filter { since == null || (it.long("last_watched") ?: 0L) > since }.sortedByDescending { it.long("last_watched") }.let { if (lim != null) it.take(lim) else it })
            }
            "sync_delete_watch_progress" -> { deleteProgress(a, pid ?: 1); null }
            "sync_get_watch_progress_delta_cursor" -> n(cursor("watch_progress_events", pid ?: 1))
            "sync_pull_watch_progress_delta" -> delta("watch_progress_events", pid ?: 1, a)
            "sync_push_watched_items" -> { pushWatched(a.arr("p_items"), pid ?: 1); null }
            "sync_pull_watched_items" -> {
                val size = a.int("p_page_size") ?: 100000; val page = a.int("p_page") ?: 1
                JsonArray(ownRows("watched_items", pid ?: 1).sortedByDescending { it.long("watched_at") }.drop((page - 1).coerceAtLeast(0) * size).take(size))
            }
            "sync_delete_watched_items" -> { deleteWatched(a.arr("p_keys"), pid ?: 0); null }
            "sync_get_watched_items_delta_cursor" -> n(cursor("watched_item_events", pid ?: 1))
            "sync_pull_watched_items_delta" -> delta("watched_item_events", pid ?: 1, a)
            "sync_push_profiles" -> { pushProfiles(a.arr("p_profiles")); null }
            "sync_pull_profiles" -> JsonArray(table("profiles").sortedBy { it.int("profile_index") }.map { publicProfile(it) })
            "sync_pull_profile_locks" -> JsonArray(table("profiles").sortedBy { it.int("profile_index") }.map { JsonObject(mapOf("profile_index" to (it["profile_index"] ?: n(1)), "pin_enabled" to (it["pin_enabled"] ?: JsonPrimitive(false)), "pin_locked_until" to (it["pin_locked_until"] ?: JsonNull))) })
            "set_profile_pin" -> { setPin(a); null }
            "clear_profile_pin" -> { clearPin(pid ?: 0, a.str("p_current_pin"), null); null }
            "clear_profile_pin_with_account_password" -> { clearPin(pid ?: 0, null, a.str("p_account_password").orEmpty()); null }
            "verify_profile_pin" -> verifyPin(pid ?: 0, a.str("p_pin").orEmpty())
            "sync_delete_profile_data" -> { deleteProfileData(pid ?: 0); null }
            "sync_push_profile_settings_blob" -> { pushBlob("profile_settings_blobs", pid ?: 1, a.str("p_platform") ?: "tv", a["p_settings_json"] ?: JsonObject(emptyMap())); null }
            "sync_push_profile_settings_blob_guarded" -> pushBlobGuarded(a)
            "sync_pull_profile_settings_blob" -> JsonArray(ownRows("profile_settings_blobs", pid ?: 1).filter { it.str("platform") == (a.str("p_platform") ?: "tv") }.map { JsonObject(mapOf("profile_id" to it["profile_id"]!!, "settings_json" to it["settings_json"]!!, "updated_at" to it["updated_at"]!!)) })
            "sync_push_home_catalog_settings" -> { pushBlob("home_catalog_settings", pid ?: 1, a.str("p_platform") ?: "tv", a["p_settings_json"] ?: JsonObject(emptyMap())); null }
            "sync_pull_home_catalog_settings" -> JsonArray(ownRows("home_catalog_settings", pid ?: 1).filter { it.str("platform") == (a.str("p_platform") ?: "tv") })
            "sync_push_collections" -> { upsertDoc("collections", pid ?: 1, "collections_json", a["p_collections_json"] ?: JsonArray(emptyList())); null }
            "sync_pull_collections" -> JsonArray(ownRows("collections", pid ?: 1).map { JsonObject(mapOf("profile_id" to it["profile_id"]!!, "collections_json" to it["collections_json"]!!, "updated_at" to it["updated_at"]!!)) })
            "sync_push_media_servers" -> s(upsertDoc("media_servers", pid ?: 1, "servers_json", a["p_servers"] ?: JsonArray(emptyList())))
            "sync_pull_media_servers" -> JsonArray(ownRows("media_servers", pid ?: 1).map { JsonObject(mapOf("servers_json" to it["servers_json"]!!, "updated_at" to it["updated_at"]!!)) })
            "sync_push_provider_credentials" -> { pushCredentials(pid ?: 1, a.arr("p_credentials"), overwrite = true); null }
            "sync_seed_provider_credentials" -> { pushCredentials(pid ?: 1, a.arr("p_credentials"), overwrite = false); null }
            "sync_pull_provider_credentials" -> JsonArray(ownRows("provider_credentials", pid ?: 1).sortedBy { it.str("provider") }.map { JsonObject(mapOf("provider" to it["provider"]!!, "credential_json" to it["credential_json"]!!, "updated_at" to it["updated_at"]!!)) })
            "sync_delete_provider_credentials" -> { table("provider_credentials").removeAll { it.int("profile_id") == pid && it.str("provider") == a.str("p_provider")?.lowercase() }; persistDb(); null }
            "sync_copy_profile_setup" -> copyProfileSetup(a)
            "get_profile_tracker_settings" -> JsonArray(ownRows("profile_tracker_settings", pid ?: 1))
            "upsert_profile_tracker_settings" -> { upsertRow("profile_tracker_settings", listOf("profile_id", "tracker"), a, mapOf("profile_id" to "p_profile_id", "tracker" to "p_tracker", "enabled_statuses" to "p_enabled_statuses", "row_order" to "p_row_order", "send_progress" to "p_send_progress")); null }
            "get_tracker_tokens" -> JsonArray(ownRows("user_tracker_tokens", pid ?: 1))
            "upsert_tracker_tokens" -> { upsertRow("user_tracker_tokens", listOf("profile_id", "tracker"), a, mapOf("profile_id" to "p_profile_id", "tracker" to "p_tracker", "access_token" to "p_access_token", "refresh_token" to "p_refresh_token", "expires_in_seconds" to "p_expires_in_seconds", "tracker_user_id" to "p_tracker_user_id", "username" to "p_username")); null }
            "clear_tracker_tokens" -> { table("user_tracker_tokens").removeAll { it.int("profile_id") == pid && it.str("tracker") == a.str("p_tracker") }; persistDb(); null }
            "generate_sync_code", "get_sync_code" -> JsonArray(listOf(JsonObject(mapOf("code" to s(pairingCode())))))
            "claim_sync_code" -> JsonArray(listOf(JsonObject(mapOf("result_owner_id" to s(ownerId), "success" to JsonPrimitive(false), "message" to s("This device already uses this Moovie Flix server. Pair TVs with the code shown on the PC.")))))
            "unlink_device" -> { val d = a.str("p_device_user_id"); table("linked_devices").removeAll { it.str("device_user_id") == d }; removeDevice(d); persistDb(); null }
            "sync_export_account_backup" -> JsonObject(mapOf("version" to n(1), "tables" to JsonObject(tables.mapValues { JsonArray(it.value) })))
            else -> throw pgError(404, "PGRST202", "Could not find the function public.$fn in the schema cache (not supported by the Moovie Flix local server)")
        }
        return if (result == null) 204 to null else 200 to result
    }

    private fun removeDevice(deviceId: String?) {
        if (deviceId == null) return
        secrets = JsonObject(secrets + ("devices" to JsonArray(devices().filter { it.str("id") != deviceId })) +
            ("refresh_tokens" to JsonObject(refreshTokens().filterValues { it.jsonObject.str("device_id") != deviceId })))
        persistSecrets()
    }

    private fun ownRows(t: String, pid: Int) = table(t).filter { it.int("profile_id") == pid }
    private fun nowS() = s(Instant.now().toString())

    private fun syncOverview(): JsonObject {
        fun counts(t: String, distinct: String? = null) = JsonObject(table(t).groupBy { it.int("profile_id") ?: 1 }.mapKeys { it.key.toString() }
            .mapValues { (_, v) -> n((if (distinct != null) v.map { it.str(distinct) }.toSet().size else v.size).toLong()) })
        return JsonObject(mapOf(
            "addons" to counts("addons"), "plugins" to counts("plugins"), "library_items" to counts("library_items"),
            "watch_progress" to counts("watch_progress", "content_id"), "watched_items" to counts("watched_items", "content_id"),
            "profiles" to JsonObject(table("profiles").associate { (it.int("profile_index") ?: 1).toString() to JsonObject(mapOf("name" to (it["name"] ?: s("")), "color" to (it["avatar_color_hex"] ?: s("#1E88E5")))) }),
        ))
    }

    private fun replaceUrlSet(t: String, items: List<JsonObject>, pid: Int, withRepo: Boolean) {
        val rows = table(t)
        val old = rows.filter { it.int("profile_id") == pid }.associateBy { it.str("url") }
        rows.removeAll { it.int("profile_id") == pid }
        val seen = HashSet<String>()
        items.forEach { e ->
            val url = e.str("url") ?: return@forEach
            if (!seen.add(url)) return@forEach
            val prev = old[url]
            val m = mutableMapOf<String, JsonElement>(
                "id" to (prev?.get("id") ?: s(UUID.randomUUID().toString())), "user_id" to s(ownerId), "url" to s(url),
                "name" to (e["name"] ?: JsonNull), "enabled" to JsonPrimitive(e.bool("enabled") ?: true), "sort_order" to n((e.int("sort_order") ?: 0).toLong()),
                "created_at" to (prev?.get("created_at") ?: nowS()), "updated_at" to nowS(), "profile_id" to n(pid.toLong()),
            )
            if (withRepo) m["repo_type"] = e["repo_type"] ?: JsonNull
            rows += JsonObject(m)
        }
        persistDb()
    }

    private val libraryCols = listOf("name", "poster", "poster_shape", "background", "logo", "description", "release_info", "imdb_rating", "genres", "addon_base_url", "added_at")

    private fun event(t: String, pid: Int, op: String, row: JsonObject, cols: List<String>) {
        eventSeq++
        table(t).add(JsonObject(mapOf("event_id" to n(eventSeq), "profile_id" to n(pid.toLong()), "operation" to s(op)) + cols.associateWith { row[it] ?: JsonNull }))
        val ev = table(t); if (ev.size > 20000) ev.subList(0, ev.size - 20000).clear()
    }

    private fun cursor(t: String, pid: Int) = table(t).filter { it.int("profile_id") == pid }.maxOfOrNull { it.long("event_id") ?: 0L } ?: eventSeq
    private fun delta(t: String, pid: Int, a: JsonObject): JsonArray {
        val since = a.long("p_since_event_id") ?: 0; val lim = a.int("p_limit") ?: 1000
        return JsonArray(table(t).filter { it.int("profile_id") == pid && (it.long("event_id") ?: 0L) > since }.sortedBy { it.long("event_id") ?: 0L }.take(lim)
            .map { JsonObject(it - "profile_id") })
    }

    private fun pushLibrary(items: List<JsonObject>, pid: Int, replace: Boolean) {
        val rows = table("library_items")
        val incoming = items.filter { !it.str("content_id").isNullOrBlank() && !it.str("content_type").isNullOrBlank() }.associateBy { it.str("content_id") + "\u0000" + it.str("content_type") }
        if (replace) {
            val gone = rows.filter { it.int("profile_id") == pid && (it.str("content_id") + "\u0000" + it.str("content_type")) !in incoming.keys }
            gone.forEach { event("library_item_events", pid, "delete", it, listOf("content_id", "content_type") + libraryCols) }
            rows.removeAll(gone.toSet())
        }
        incoming.values.forEach { e ->
            val idx = rows.indexOfFirst { it.int("profile_id") == pid && it.str("content_id") == e.str("content_id") && it.str("content_type") == e.str("content_type") }
            val prev = if (idx >= 0) rows[idx] else null
            val logo = e.str("logo")?.takeIf { it.isNotBlank() }?.let(::s) ?: prev?.get("logo") ?: JsonNull
            val row = JsonObject(mapOf(
                "id" to (prev?.get("id") ?: s(UUID.randomUUID().toString())), "user_id" to s(ownerId), "content_id" to e["content_id"]!!, "content_type" to e["content_type"]!!,
                "name" to (e["name"]?.takeIf { it !is JsonNull } ?: s("")), "poster" to (e["poster"] ?: JsonNull), "poster_shape" to (e["poster_shape"]?.takeIf { it !is JsonNull } ?: s("POSTER")),
                "background" to (e["background"] ?: JsonNull), "logo" to logo, "description" to (e["description"] ?: JsonNull), "release_info" to (e["release_info"] ?: JsonNull),
                "imdb_rating" to (e["imdb_rating"]?.let { el -> (el as? JsonPrimitive)?.contentOrNull?.toDoubleOrNull()?.let { JsonPrimitive(it) } } ?: JsonNull),
                "genres" to ((e["genres"] as? JsonArray) ?: JsonArray(emptyList())), "addon_base_url" to (e["addon_base_url"] ?: JsonNull),
                "added_at" to n(e.long("added_at") ?: 0), "created_at" to (prev?.get("created_at") ?: nowS()), "updated_at" to nowS(), "profile_id" to n(pid.toLong()),
            ))
            if (prev != null && libraryCols.all { prev[it] == row[it] }) return@forEach
            if (idx >= 0) rows[idx] = row else rows += row
            event("library_item_events", pid, "upsert", row, listOf("content_id", "content_type") + libraryCols)
        }
        persistDb()
    }

    private fun deleteLibrary(keys: List<JsonObject>, pid: Int) {
        val rows = table("library_items")
        val gone = rows.filter { r -> r.int("profile_id") == pid && keys.any { it.str("content_id") == r.str("content_id") && it.str("content_type") == r.str("content_type") } }
        gone.forEach { event("library_item_events", pid, "delete", it, listOf("content_id", "content_type") + libraryCols) }
        rows.removeAll(gone.toSet()); persistDb()
    }

    private val progressCols = listOf("progress_key", "content_id", "content_type", "video_id", "season", "episode", "position", "duration", "last_watched")

    private fun pushProgress(entries: List<JsonObject>, pid: Int) {
        val rows = table("watch_progress")
        entries.filter { !it.str("content_id").isNullOrBlank() }.forEach { e ->
            val cid0 = e.str("content_id")!!; val type = e.str("content_type") ?: ""; val vid = e.str("video_id") ?: ""
            val season = e.int("season"); val ep = e.int("episode")
            val cid = if ((cid0 == "tmdb" || cid0 == "kitsu") && type == "movie" && vid.all { it.isDigit() } && vid.isNotEmpty()) "$cid0:$vid" else cid0
            val key = e.str("progress_key")?.takeIf { it.isNotBlank() } ?: if (season != null && ep != null) "${cid}_s${season}e$ep" else cid
            val lw = e.long("last_watched") ?: 0
            val idx = rows.indexOfFirst { it.int("profile_id") == pid && it.str("progress_key") == key }
            if (idx >= 0 && (rows[idx].long("last_watched") ?: 0L) > lw) return@forEach
            val row = JsonObject(mapOf(
                "id" to (if (idx >= 0) rows[idx]["id"]!! else s(UUID.randomUUID().toString())), "user_id" to s(ownerId), "content_id" to s(cid), "content_type" to s(type),
                "video_id" to s(vid), "season" to (season?.let { n(it.toLong()) } ?: JsonNull), "episode" to (ep?.let { n(it.toLong()) } ?: JsonNull),
                "position" to n(e.long("position") ?: 0), "duration" to n(e.long("duration") ?: 0), "last_watched" to n(lw), "progress_key" to s(key), "profile_id" to n(pid.toLong()),
            ))
            if (idx >= 0) rows[idx] = row else rows += row
            event("watch_progress_events", pid, "upsert", row, progressCols)
        }
        persistDb()
    }

    private fun deleteProgress(a: JsonObject, pid: Int) {
        val keys = mutableSetOf<String>()
        a.str("p_progress_key")?.let(keys::add)
        (a["p_keys"] as? JsonArray)?.forEach { k -> when (k) { is JsonPrimitive -> k.contentOrNull?.let(keys::add); is JsonObject -> k.str("progress_key")?.let(keys::add); else -> {} } }
        val rows = table("watch_progress")
        val gone = rows.filter { it.int("profile_id") == pid && it.str("progress_key") in keys }
        gone.forEach { event("watch_progress_events", pid, "delete", it, progressCols) }
        rows.removeAll(gone.toSet()); persistDb()
    }

    private val watchedCols = listOf("content_id", "content_type", "title", "season", "episode", "watched_at")
    private fun wkey(o: JsonObject) = "${o.str("content_id")}|${o.int("season") ?: -1}|${o.int("episode") ?: -1}"

    private fun pushWatched(items: List<JsonObject>, pid: Int) {
        val rows = table("watched_items")
        items.filter { !it.str("content_id").isNullOrBlank() }.forEach { e ->
            val idx = rows.indexOfFirst { it.int("profile_id") == pid && wkey(it) == wkey(e) }
            val row = JsonObject(mapOf(
                "id" to (if (idx >= 0) rows[idx]["id"]!! else s(UUID.randomUUID().toString())), "user_id" to s(ownerId), "content_id" to e["content_id"]!!,
                "content_type" to s(e.str("content_type") ?: "movie"), "title" to s(e.str("title") ?: ""), "season" to (e.int("season")?.let { n(it.toLong()) } ?: JsonNull),
                "episode" to (e.int("episode")?.let { n(it.toLong()) } ?: JsonNull), "watched_at" to n(e.long("watched_at") ?: 0),
                "created_at" to (if (idx >= 0) rows[idx]["created_at"] ?: nowS() else nowS()), "profile_id" to n(pid.toLong()),
            ))
            if (idx >= 0 && watchedCols.all { rows[idx][it] == row[it] }) return@forEach
            if (idx >= 0) rows[idx] = row else rows += row
            event("watched_item_events", pid, "upsert", row, watchedCols)
        }
        persistDb()
    }

    private fun deleteWatched(keys: List<JsonObject>, pid: Int) {
        val ks = keys.filter { !it.str("content_id").isNullOrBlank() }.map(::wkey).toSet()
        val rows = table("watched_items")
        val gone = rows.filter { it.int("profile_id") == pid && wkey(it) in ks }
        gone.forEach { event("watched_item_events", pid, "delete", it, watchedCols) }
        rows.removeAll(gone.toSet()); persistDb()
    }

    private fun publicProfile(p: JsonObject) = JsonObject(listOf("id", "user_id", "profile_index", "name", "avatar_color_hex", "uses_primary_addons", "uses_primary_plugins", "avatar_id", "avatar_url", "pin_enabled", "pin_locked_until", "created_at", "updated_at", "profile_background_id", "profile_background_url").associateWith { p[it] ?: JsonNull })

    private fun pushProfiles(items: List<JsonObject>) {
        val rows = table("profiles")
        items.associateBy { it.int("profile_index") ?: -1 }.filterKeys { it in 1..6 }.forEach { (idx, e) ->
            val i = rows.indexOfFirst { it.int("profile_index") == idx }
            val prev = if (i >= 0) rows[i] else null
            val avatarUrl = e.str("avatar_url")?.trim()?.ifBlank { null }
            val avatarId = e.str("avatar_id")?.trim()?.ifBlank { null }
            val m = (prev ?: JsonObject(mapOf("id" to s(UUID.randomUUID().toString()), "user_id" to s(ownerId), "created_at" to nowS(), "pin_enabled" to JsonPrimitive(false), "failed_pin_attempts" to n(0), "pin_locked_until" to JsonNull, "pin_hash" to JsonNull))).toMutableMap()
            m["profile_index"] = n(idx.toLong()); m["profile_id"] = n(idx.toLong())
            m["name"] = s(e.str("name") ?: ""); m["avatar_color_hex"] = s(e.str("avatar_color_hex") ?: "#1E88E5")
            m["uses_primary_addons"] = JsonPrimitive(idx != 1 && (e.bool("uses_primary_addons") ?: false))
            m["uses_primary_plugins"] = JsonPrimitive(idx != 1 && (e.bool("uses_primary_plugins") ?: false))
            when {
                avatarUrl != null -> { m["avatar_url"] = s(avatarUrl); m["avatar_id"] = JsonNull }
                "avatar_url" in e -> { m["avatar_url"] = JsonNull; m["avatar_id"] = avatarId?.let(::s) ?: JsonNull }
                else -> { m["avatar_id"] = avatarId?.let(::s) ?: prev?.get("avatar_id") ?: JsonNull; if ("avatar_url" !in m) m["avatar_url"] = JsonNull }
            }
            listOf("profile_background_id", "profile_background_url").forEach { k -> if (k in e) m[k] = e[k]!! }
            m["updated_at"] = nowS()
            if (i >= 0) rows[i] = JsonObject(m) else rows += JsonObject(m)
        }
        persistDb()
    }

    private fun profileIdx(pid: Int): Int { if (pid !in 1..6) throw pgError(400, "P0001", "Invalid profile id"); return table("profiles").indexOfFirst { it.int("profile_index") == pid }.also { if (it < 0) throw pgError(400, "P0001", "Profile not found for current user") } }
    private fun pinHash(pin: String, salt: String = token(8)) = "$salt$" + sha("$salt:$pin")
    private fun pinOk(pin: String, hash: String?) = hash != null && hash.contains('$') && MessageDigest.isEqual(pinHash(pin, hash.substringBefore('$')).toByteArray(), hash.toByteArray())

    private fun setPin(a: JsonObject) {
        val pid = a.int("p_profile_id") ?: 0; val pin = a.str("p_pin").orEmpty()
        if (!Regex("^[0-9]{4}$").matches(pin)) throw pgError(400, "P0001", "PIN must be exactly 4 digits")
        val rows = table("profiles"); val i = profileIdx(pid); val p = rows[i]
        if (p.bool("pin_enabled") == true && p.str("pin_hash") != null && !pinOk(a.str("p_current_pin").orEmpty(), p.str("pin_hash"))) throw pgError(400, "P0001", "Current PIN is required to change PIN")
        rows[i] = JsonObject(p + mapOf("pin_enabled" to JsonPrimitive(true), "pin_hash" to s(pinHash(pin)), "pin_updated_at" to nowS(), "failed_pin_attempts" to n(0), "pin_locked_until" to JsonNull, "updated_at" to nowS()))
        persistDb()
    }

    private fun clearPin(pid: Int, currentPin: String?, accountPassword: String?) {
        val rows = table("profiles"); val i = profileIdx(pid); val p = rows[i]
        if (accountPassword != null) { if (!passwordValid(accountPassword)) throw pgError(400, "P0001", "Incorrect account password") }
        else if (p.bool("pin_enabled") == true && p.str("pin_hash") != null && !pinOk(currentPin.orEmpty(), p.str("pin_hash"))) throw pgError(400, "P0001", "Current PIN is required")
        rows[i] = JsonObject(p + mapOf("pin_enabled" to JsonPrimitive(false), "pin_hash" to JsonNull, "failed_pin_attempts" to n(0), "pin_locked_until" to JsonNull, "updated_at" to nowS()))
        persistDb()
    }

    private fun verifyPin(pid: Int, pin: String): JsonArray {
        fun res(ok: Boolean, retry: Int) = JsonArray(listOf(JsonObject(mapOf("unlocked" to JsonPrimitive(ok), "retry_after_seconds" to n(retry.toLong())))))
        if (pid !in 1..6) return res(false, 0)
        val rows = table("profiles"); val i = rows.indexOfFirst { it.int("profile_index") == pid }; if (i < 0) return res(false, 0)
        val p = rows[i]
        if (p.bool("pin_enabled") != true || p.str("pin_hash") == null) return res(true, 0)
        val locked = p.str("pin_locked_until")?.let { runCatching { Instant.parse(it) }.getOrNull() }
        if (locked != null && locked.isAfter(Instant.now())) return res(false, (locked.epochSecond - Instant.now().epochSecond).toInt().coerceAtLeast(0))
        if (Regex("^[0-9]{4}$").matches(pin) && pinOk(pin, p.str("pin_hash"))) { rows[i] = JsonObject(p + mapOf("failed_pin_attempts" to n(0), "pin_locked_until" to JsonNull)); persistDb(); return res(true, 0) }
        val fails = (p.int("failed_pin_attempts") ?: 0) + 1
        return if (fails >= 5) { rows[i] = JsonObject(p + mapOf("failed_pin_attempts" to n(0), "pin_locked_until" to s(Instant.now().plusSeconds(300).toString()))); persistDb(); res(false, 300) }
        else { rows[i] = JsonObject(p + mapOf("failed_pin_attempts" to n(fails.toLong()))); persistDb(); res(false, 0) }
    }

    private fun deleteProfileData(pid: Int) {
        if (pid !in 1..6) throw pgError(400, "P0001", "Invalid profile id")
        listOf("addons", "plugins", "library_items", "watch_progress", "watched_items", "profile_settings_blobs", "home_catalog_settings", "collections", "media_servers", "provider_credentials", "profile_tracker_settings", "user_tracker_tokens")
            .forEach { t -> table(t).removeAll { it.int("profile_id") == pid } }
        if (pid > 1) table("profiles").removeAll { it.int("profile_index") == pid }
        persistDb()
    }

    private fun pushBlob(t: String, pid: Int, platform: String, value: JsonElement): String {
        val rows = table(t)
        val i = rows.indexOfFirst { it.int("profile_id") == pid && it.str("platform") == platform }
        if (i >= 0 && rows[i]["settings_json"] == value) return rows[i].str("updated_at")!!
        val ts = Instant.now().toString()
        val row = JsonObject(mapOf("id" to (if (i >= 0) rows[i]["id"]!! else s(UUID.randomUUID().toString())), "user_id" to s(ownerId), "profile_id" to n(pid.toLong()), "platform" to s(platform), "settings_json" to value, "created_at" to (if (i >= 0) rows[i]["created_at"] ?: s(ts) else s(ts)), "updated_at" to s(ts)))
        if (i >= 0) rows[i] = row else rows += row
        persistDb(); return ts
    }

    private fun pushBlobGuarded(a: JsonObject): JsonElement {
        val pid = a.int("p_profile_id") ?: 1; val platform = a.str("p_platform") ?: "tv"
        if (pid !in 1..6) throw pgError(400, "22023", "Invalid profile id")
        val cur = table("profile_settings_blobs").firstOrNull { it.int("profile_id") == pid && it.str("platform") == platform }
        val expected = a.str("p_expected_updated_at")
        val conflict = if (cur != null) expected == null || runCatching { Instant.parse(expected) != Instant.parse(cur.str("updated_at")) }.getOrDefault(true) else expected != null
        if (conflict) throw pgError(409, "PT409", "Settings changed on another device. Reload and try again.")
        return s(pushBlob("profile_settings_blobs", pid, platform, a["p_settings_json"] ?: JsonObject(emptyMap())))
    }

    private fun upsertDoc(t: String, pid: Int, col: String, value: JsonElement): String {
        if (pid !in 1..6) throw pgError(400, "22023", "Invalid profile id")
        val rows = table(t)
        val i = rows.indexOfFirst { it.int("profile_id") == pid }
        if (i >= 0 && rows[i][col] == value) return rows[i].str("updated_at")!!
        val ts = Instant.now().toString()
        val row = JsonObject(mapOf("id" to (if (i >= 0) rows[i]["id"] ?: s(UUID.randomUUID().toString()) else s(UUID.randomUUID().toString())), "user_id" to s(ownerId), "profile_id" to n(pid.toLong()), col to value, "created_at" to (if (i >= 0) rows[i]["created_at"] ?: s(ts) else s(ts)), "updated_at" to s(ts)))
        if (i >= 0) rows[i] = row else rows += row
        persistDb(); return ts
    }

    private fun pushCredentials(pid: Int, items: List<JsonObject>, overwrite: Boolean) {
        if (pid !in 1..6) throw pgError(400, "22023", "Invalid profile id")
        if (items.size > 16) throw pgError(400, "22023", "Too many provider credentials")
        val rows = table("provider_credentials")
        val seen = HashSet<String>()
        items.forEach { e ->
            val provider = e.str("provider")?.trim()?.lowercase() ?: throw pgError(400, "22023", "Invalid provider credential entry")
            if (!seen.add(provider)) throw pgError(400, "22023", "Duplicate provider credential: $provider")
            val cred = e["credential_json"] ?: throw pgError(400, "22023", "Invalid provider credential entry")
            val i = rows.indexOfFirst { it.int("profile_id") == pid && it.str("provider") == provider }
            if (i >= 0 && (!overwrite || rows[i]["credential_json"] == cred)) return@forEach
            val row = JsonObject(mapOf("user_id" to s(ownerId), "profile_id" to n(pid.toLong()), "provider" to s(provider), "credential_json" to cred, "updated_at" to nowS()))
            if (i >= 0) rows[i] = row else rows += row
        }
        persistDb()
    }

    private fun copyProfileSetup(a: JsonObject): JsonArray {
        val src = a.int("p_source_profile_id") ?: 1; val dst = a.int("p_target_profile_id") ?: 1
        if (src !in 1..6 || dst !in 1..6 || src == dst) throw pgError(400, "22023", "Invalid profile id")
        fun copyPlatform(flag: String, platform: String): String {
            if (a.bool(flag) != true) return "skipped"
            val blob = table("profile_settings_blobs").firstOrNull { it.int("profile_id") == src && it.str("platform") == platform } ?: return "missing"
            val before = table("profile_settings_blobs").firstOrNull { it.int("profile_id") == dst && it.str("platform") == platform }?.get("settings_json")
            pushBlob("profile_settings_blobs", dst, platform, blob["settings_json"]!!)
            return if (before == blob["settings_json"]) "unchanged" else "copied"
        }
        val tv = copyPlatform("p_copy_tv", "tv"); val mobile = copyPlatform("p_copy_mobile", "mobile"); val desktop = copyPlatform("p_copy_desktop", "desktop")
        var credStatus = "skipped"
        if (a.bool("p_copy_provider_credentials") == true) {
            val srcCreds = table("provider_credentials").filter { it.int("profile_id") == src }
            if (srcCreds.isEmpty()) credStatus = "missing" else {
                pushCredentials(dst, srcCreds.map { JsonObject(mapOf("provider" to it["provider"]!!, "credential_json" to it["credential_json"]!!)) }, overwrite = a.bool("p_replace_provider_credentials") == true)
                credStatus = "copied"
            }
        }
        return JsonArray(listOf(JsonObject(mapOf("source_profile_id" to n(src.toLong()), "target_profile_id" to n(dst.toLong()), "tv_status" to s(tv), "mobile_status" to s(mobile), "desktop_status" to s(desktop), "provider_credentials_status" to s(credStatus)))))
    }

    private fun upsertRow(t: String, keys: List<String>, a: JsonObject, mapping: Map<String, String>) {
        val row = JsonObject(mapping.mapValues { a[it.value] ?: JsonNull } + mapOf("user_id" to s(ownerId), "updated_at" to nowS()))
        val rows = table(t)
        val i = rows.indexOfFirst { r -> keys.all { r[it].norm() == row[it].norm() } }
        if (i >= 0) rows[i] = JsonObject(rows[i] + row) else rows += row
        persistDb()
    }

    // ---------- json helpers ----------
    private fun s(v: String) = JsonPrimitive(v)
    private fun n(v: Long) = JsonPrimitive(v)
    private fun n(v: Int) = JsonPrimitive(v)
    private fun parseObj(body: String): JsonObject = if (body.isBlank()) JsonObject(emptyMap()) else (json.parseToJsonElement(body) as? JsonObject) ?: JsonObject(emptyMap())
    private fun parseRows(body: String): List<JsonObject> = when (val e = if (body.isBlank()) JsonArray(emptyList()) else json.parseToJsonElement(body)) { is JsonArray -> e.map { it.jsonObject }; is JsonObject -> listOf(e); else -> emptyList() }
    private fun JsonObject.str(k: String): String? = (this[k] as? JsonPrimitive)?.contentOrNull
    private fun JsonObject.int(k: String): Int? = (this[k] as? JsonPrimitive)?.let { it.longOrNull?.toInt() ?: it.doubleOrNull?.toInt() ?: it.contentOrNull?.toIntOrNull() }
    private fun JsonObject.long(k: String): Long? = (this[k] as? JsonPrimitive)?.let { it.longOrNull ?: it.doubleOrNull?.toLong() ?: it.contentOrNull?.toLongOrNull() }
    private fun JsonObject.bool(k: String): Boolean? = (this[k] as? JsonPrimitive)?.let { it.booleanOrNull ?: it.contentOrNull?.toBooleanStrictOrNull() }
    private fun JsonObject.arr(k: String): List<JsonObject> = when (val v = this[k]) {
        is JsonArray -> v.mapNotNull { it as? JsonObject }
        is JsonPrimitive -> v.contentOrNull?.let { runCatching { json.parseToJsonElement(it).jsonArray.mapNotNull { e -> e as? JsonObject } }.getOrNull() } ?: emptyList()
        else -> emptyList()
    }
}
