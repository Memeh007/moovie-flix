package com.moovieflix.tv.locallibrary

import android.content.Context
import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

/**
 * Pulls the library sources the desktop "Moovie Flix Media Share" publishes for this account
 * (table public.moovieflix_library_sources) and adds them as HTTP folder sources, so nothing has to
 * be typed on the TV. Sources removed on the PC are removed here too; manually added sources
 * (USB/SAF folders, typed URLs) are never touched.
 */
object MoovieFlixSharedSources {
    private const val TAG = "MoovieFlixSharedSources"
    private const val PREFS = "moovie_flix_local_library"
    private const val KEY_SYNCED = "synced_sources"

    suspend fun sync(context: Context, client: SupabaseClient) = withContext(Dispatchers.IO) {
        runCatching {
            val userId = client.auth.currentUserOrNull()?.id ?: return@runCatching
            val body = client.postgrest.from("moovieflix_library_sources")
                .select { filter { eq("user_id", userId) } }.data
            val rows = JSONArray(body)
            val urls = mutableListOf<String>()
            for (i in 0 until rows.length()) {
                val sources = rows.getJSONObject(i).optJSONArray("sources") ?: continue
                for (j in 0 until sources.length()) sources.optJSONObject(j)?.optString("url")?.takeIf { it.startsWith("http") }?.let(urls::add)
            }
            LocalLibraryRepository.load(context)
            val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val previous = prefs.getStringSet(KEY_SYNCED, emptySet()).orEmpty()
            (previous - urls.toSet()).forEach { LocalLibraryRepository.removeSource(context, it) }
            urls.forEach { LocalLibraryRepository.addSource(context, it) }
            prefs.edit().putStringSet(KEY_SYNCED, urls.toSet()).apply()
            if (urls.toSet() != previous || (urls.isNotEmpty() && LocalLibraryRepository.movies.value.isEmpty())) LocalLibraryRepository.rescan(context)
        }.onFailure { Log.w(TAG, "Shared source sync failed: ${it.message}") }
    }
}
