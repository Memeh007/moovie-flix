package com.moovieflix.tv.locallibrary

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder

data class LocalMovie(
    val uri: String,
    val fileName: String,
    val title: String,
    val year: Int?,
    val sizeBytes: Long?
)

/**
 * Folder sources the user owns:
 *  - SAF tree URIs (device storage, USB drives, or any SMB/NAS provider exposed through the
 *    Android folder picker)
 *  - plain HTTP(S) directory listings (e.g. a NAS, `python -m http.server`, nginx autoindex)
 *
 * Kept in its own SharedPreferences file; it does not touch the addon system.
 */
object LocalLibraryRepository {
    private const val PREFS = "moovie_flix_local_library"
    private const val KEY_SOURCES = "sources"
    private const val KEY_MOVIES = "movies"
    private const val MAX_DEPTH = 6

    private val _movies = MutableStateFlow<List<LocalMovie>>(emptyList())
    val movies: StateFlow<List<LocalMovie>> = _movies
    private val _sources = MutableStateFlow<List<String>>(emptyList())
    val sources: StateFlow<List<String>> = _sources
    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning
    private var loaded = false

    fun load(context: Context) {
        if (loaded) return
        loaded = true
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _sources.value = runCatching {
            val arr = JSONArray(prefs.getString(KEY_SOURCES, "[]"))
            List(arr.length()) { arr.getString(it) }
        }.getOrDefault(emptyList())
        _movies.value = runCatching {
            val arr = JSONArray(prefs.getString(KEY_MOVIES, "[]"))
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                LocalMovie(
                    uri = o.getString("uri"),
                    fileName = o.optString("file"),
                    title = o.optString("title"),
                    year = o.optInt("year", 0).takeIf { it > 0 },
                    sizeBytes = o.optLong("size", -1L).takeIf { it >= 0 }
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun save(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val movies = JSONArray()
        _movies.value.forEach {
            movies.put(JSONObject().apply {
                put("uri", it.uri); put("file", it.fileName); put("title", it.title)
                put("year", it.year ?: 0); put("size", it.sizeBytes ?: -1L)
            })
        }
        prefs.edit()
            .putString(KEY_SOURCES, JSONArray(_sources.value).toString())
            .putString(KEY_MOVIES, movies.toString())
            .apply()
    }

    fun addTreeUri(context: Context, uri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        addSource(context, uri.toString())
    }

    fun addSource(context: Context, source: String) {
        val s = source.trim()
        if (s.isEmpty() || s in _sources.value) return
        _sources.value = _sources.value + s
        save(context)
    }

    fun removeSource(context: Context, source: String) {
        _sources.value = _sources.value - source
        if (source.startsWith("content://")) {
            runCatching {
                context.contentResolver.releasePersistableUriPermission(
                    Uri.parse(source), Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
        save(context)
    }

    suspend fun rescan(context: Context) {
        if (_scanning.value) return
        _scanning.value = true
        try {
            val found = withContext(Dispatchers.IO) {
                val out = mutableListOf<LocalMovie>()
                for (source in _sources.value) {
                    runCatching {
                        when {
                            source.startsWith("content://") -> scanTree(context, Uri.parse(source), out)
                            source.startsWith("http://") || source.startsWith("https://") ->
                                scanHttp(source.let { if (it.endsWith("/")) it else "$it/" }, 0, out, mutableSetOf())
                        }
                    }
                }
                out.distinctBy { it.uri }.sortedWith(compareBy({ it.title.lowercase() }, { it.year ?: 0 }))
            }
            _movies.value = found
            save(context)
        } finally {
            _scanning.value = false
        }
    }

    private fun movieOf(uri: String, name: String, size: Long?): LocalMovie {
        val parsed = LocalMediaParser.parse(name)
        return LocalMovie(uri, name, parsed.title, parsed.year, size)
    }

    private fun scanTree(context: Context, tree: Uri, out: MutableList<LocalMovie>) {
        val rootId = DocumentsContract.getTreeDocumentId(tree)
        walkDocument(context, tree, rootId, 0, out)
    }

    private fun walkDocument(context: Context, tree: Uri, docId: String, depth: Int, out: MutableList<LocalMovie>) {
        if (depth > MAX_DEPTH) return
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, docId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE
        )
        context.contentResolver.query(children, projection, null, null, null)?.use { c ->
            while (c.moveToNext()) {
                val id = c.getString(0) ?: continue
                val name = c.getString(1) ?: continue
                val mime = c.getString(2) ?: ""
                val size = if (c.isNull(3)) null else c.getLong(3)
                if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                    walkDocument(context, tree, id, depth + 1, out)
                } else if (mime.startsWith("video/") || LocalMediaParser.isVideo(name)) {
                    if (name.contains("sample", ignoreCase = true) && (size ?: Long.MAX_VALUE) < 200L * 1024 * 1024) continue
                    val docUri = DocumentsContract.buildDocumentUriUsingTree(tree, id)
                    out += movieOf(docUri.toString(), name, size)
                }
            }
        }
    }

    private val hrefRegex = Regex("""href\s*=\s*["']([^"'#?]+)["']""", RegexOption.IGNORE_CASE)

    private fun scanHttp(dirUrl: String, depth: Int, out: MutableList<LocalMovie>, visited: MutableSet<String>) {
        if (depth > MAX_DEPTH || !visited.add(dirUrl)) return
        val conn = (URL(dirUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000; readTimeout = 15000
        }
        val html = try {
            if (conn.responseCode !in 200..299) return
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
        val base = URL(dirUrl)
        for (m in hrefRegex.findAll(html)) {
            val href = m.groupValues[1]
            if (href.startsWith("..") || href == "/" || href.startsWith("mailto:")) continue
            val abs = runCatching { URL(base, href).toString() }.getOrNull() ?: continue
            if (!abs.startsWith(dirUrl)) continue // stay inside the chosen folder
            if (abs.endsWith("/")) {
                scanHttp(abs, depth + 1, out, visited)
            } else {
                val name = URLDecoder.decode(abs.substringAfterLast('/'), "UTF-8")
                if (LocalMediaParser.isVideo(name)) out += movieOf(abs, name, null)
            }
        }
    }
}
