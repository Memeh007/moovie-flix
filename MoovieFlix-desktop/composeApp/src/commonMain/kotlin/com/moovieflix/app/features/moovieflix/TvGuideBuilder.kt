package com.moovieflix.app.features.moovieflix

import com.moovieflix.app.features.addons.AddonRepository
import com.moovieflix.app.features.catalog.fetchCatalogPage
import com.moovieflix.app.features.details.MetaDetails
import com.moovieflix.app.features.details.MetaDetailsRepository
import com.moovieflix.app.features.home.MetaPreview
import com.moovieflix.app.features.watched.WatchedRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Moovie Flix TV Guide (cable-style, but every block is on demand).
 *
 * Mirrors the original Moovie Flix guide (apps/server/src/catalog.ts guide()): genre channels plus a
 * "Moovie Mix", blocks sized by runtime, half-hour time slots and a wall-clock "now" marker.
 * Differences requested for this app: channels are built from installed addon catalogs and the local
 * library, there are per-show channels, and series always advance in episode order starting at the
 * next unwatched episode (existing watched state). Nothing is resolved here: playback goes through the
 * app's existing stream path (addons) or the local file.
 */
data class GuideBlock(
    val title: String,
    val subtitle: String?,
    val poster: String?,
    val background: String?,
    val type: String, // "movie" | "series" | "local"
    val metaId: String,
    val videoId: String,
    val season: Int?,
    val episode: Int?,
    val episodeTitle: String?,
    val local: LocalMediaItem?,
    val runtimeMin: Int,
    val start: Long = 0,
    val end: Long = 0,
)

data class GuideChannel(val key: String, val label: String, val icon: String, val number: Int, val blocks: List<GuideBlock>)

data class GuideData(val now: Long, val from: Long, val to: Long, val channels: List<GuideChannel>)

object TvGuideBuilder {
    private const val SLOT = 30 * 60_000L
    private const val HOURS = 8
    private val GENRES = listOf(
        Triple("comedy", "Comedy", listOf("comedy")),
        Triple("scifi", "Sci-Fi", listOf("sci-fi", "science fiction", "sci-fi & fantasy", "fantasy")),
        Triple("action", "Action", listOf("action", "action & adventure", "adventure")),
        Triple("drama", "Drama", listOf("drama")),
        Triple("horror", "Horror", listOf("horror", "thriller")),
        Triple("kids", "Kids", listOf("animation", "family", "kids")),
    )
    private val ICONS = mapOf("mix" to "🎬", "movies" to "🍿", "comedy" to "😂", "scifi" to "🚀", "action" to "💥", "drama" to "🎭", "horror" to "👻", "kids" to "🧸", "local" to "📁")

    private fun hash(s: String): Int { var h = -2128831035; for (c in s) { h = (h xor c.code) * 16777619 }; return h }

    private fun parseRuntime(raw: String?): Int? {
        if (raw.isNullOrBlank()) return null
        val h = Regex("""(\d+)\s*h""").find(raw)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val m = Regex("""(\d+)\s*m""").find(raw)?.groupValues?.get(1)?.toIntOrNull()
        val plain = Regex("""^\s*(\d+)\s*$""").find(raw)?.groupValues?.get(1)?.toIntOrNull()
        val total = h * 60 + (m ?: 0)
        return (if (total > 0) total else plain)?.takeIf { it in 5..400 }
    }

    /** One show's episode list in order (specials skipped) and the index of the next unwatched one. */
    private class ShowTrack(val meta: MetaDetails, val episodes: List<GuideBlock>, var pointer: Int)

    private fun showTrack(meta: MetaDetails): ShowTrack? {
        val defaultRt = parseRuntime(meta.runtime) ?: 45
        val eps = meta.videos.filter { (it.season ?: 0) > 0 && it.episode != null }
            .sortedWith(compareBy({ it.season }, { it.episode }))
            .map { v ->
                GuideBlock(
                    title = meta.name, subtitle = "S%02dE%02d".format2(v.season ?: 0, v.episode ?: 0) + (v.title.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
                    poster = v.thumbnail ?: meta.poster, background = meta.background ?: v.thumbnail, type = "series", metaId = meta.id, videoId = v.id,
                    season = v.season, episode = v.episode, episodeTitle = v.title, local = null, runtimeMin = v.runtime?.takeIf { it in 5..400 } ?: defaultRt,
                )
            }
        if (eps.isEmpty()) return null
        val next = eps.indexOfFirst { !WatchedRepository.isWatched(meta.id, "series", it.season, it.episode) }
        return ShowTrack(meta, eps, if (next < 0) 0 else next)
    }

    private fun movieBlock(p: MetaPreview) = GuideBlock(
        title = p.name, subtitle = p.releaseInfo, poster = p.poster, background = p.banner ?: p.poster, type = "movie", metaId = p.id, videoId = p.id,
        season = null, episode = null, episodeTitle = null, local = null, runtimeMin = parseRuntime(p.runtime) ?: 110,
    )

    private fun localBlock(i: LocalMediaItem) = GuideBlock(
        title = i.show ?: i.title,
        subtitle = if (i.isEpisode) "S%02dE%02d".format2(i.season ?: 0, i.episode ?: 0) else i.year?.toString(),
        poster = null, background = null, type = "local", metaId = i.path, videoId = i.path, season = i.season, episode = i.episode,
        episodeTitle = null, local = i, runtimeMin = if (i.isEpisode) 45 else 110,
    )

    /** Fills [from, to) anchored to the wall clock: the first upcoming block is "on now" from the current half hour. */
    private fun schedule(previous: GuideBlock?, next: () -> GuideBlock?, now: Long, from: Long, to: Long): List<GuideBlock> {
        val out = mutableListOf<GuideBlock>()
        var start = (now / SLOT) * SLOT
        previous?.let { p -> val s = start - p.runtimeMin * 60_000L; if (s < start) out += p.copy(start = maxOf(s, from - SLOT), end = start) }
        while (start < to && out.size < 120) {
            val b = next() ?: break
            val end = start + maxOf(5, b.runtimeMin) * 60_000L
            out += b.copy(start = start, end = end); start = end
        }
        return out
    }

    private fun seriesChannel(track: ShowTrack, now: Long, from: Long, to: Long): List<GuideBlock> {
        var i = track.pointer
        val prev = track.episodes.getOrNull(track.pointer - 1)
        return schedule(prev, { track.episodes.getOrNull(i++) }, now, from, to)
    }

    /** Round-robin mix of movies and shows; each show occurrence airs that show's next episode. */
    private fun mixedChannel(key: String, entries: List<Any>, now: Long, from: Long, to: Long): List<GuideBlock> {
        if (entries.isEmpty()) return emptyList()
        val order = entries.sortedBy { hash(key + idOf(it)) }
        val pointers = HashMap<String, Int>()
        var idx = 0
        var guard = 0
        return schedule(null, {
            var block: GuideBlock? = null
            while (block == null && guard++ < 1000) {
                val e = order[idx % order.size]; idx++
                block = when (e) {
                    is MetaPreview -> movieBlock(e)
                    is ShowTrack -> { val p = pointers.getOrPut(e.meta.id) { e.pointer }; pointers[e.meta.id] = p + 1; e.episodes.getOrNull(p % e.episodes.size) }
                    is LocalMediaItem -> localBlock(e)
                    else -> null
                }
            }
            block
        }, now, from, to)
    }

    private fun idOf(e: Any): String = when (e) { is MetaPreview -> e.id; is ShowTrack -> e.meta.id; is LocalMediaItem -> e.path; else -> e.toString() }

    private fun matchesGenre(genres: List<String>, keys: List<String>) = genres.any { g -> keys.any { g.equals(it, ignoreCase = true) } }

    suspend fun build(): GuideData = coroutineScope {
        val now = currentTimeMs()
        val from = (now / SLOT) * SLOT - SLOT
        val to = from + HOURS * 3600_000L
        withTimeoutOrNull(15_000) { AddonRepository.awaitManifestsLoaded() }
        val addons = AddonRepository.uiState.value.addons.filter { it.isActive }
        val gate = Semaphore(6)

        // 1. Catalog pages (first page of every plain movie/series catalog, plus genre pages when offered).
        data class Req(val url: String, val type: String, val catalogId: String, val genre: String?)
        val reqs = mutableListOf<Req>()
        addons.forEach { a ->
            a.manifest!!.catalogs.filter { (it.type == "movie" || it.type == "series") && it.extra.none { x -> x.isRequired && x.name != "genre" } }.forEach { c ->
                if (c.extra.none { it.isRequired }) reqs += Req(a.manifestUrl, c.type, c.id, null)
                c.extra.firstOrNull { it.name == "genre" }?.options?.let { opts ->
                    GENRES.forEach { (_, _, keys) -> opts.firstOrNull { o -> keys.any { o.equals(it, true) } }?.let { reqs += Req(a.manifestUrl, c.type, c.id, it) } }
                }
            }
        }
        val pages = reqs.take(60).map { r ->
            async { gate.withPermit { runCatching { withTimeoutOrNull(12_000) { fetchCatalogPage(r.url, r.type, r.catalogId, genre = r.genre, maxItems = 20) } }.getOrNull()?.items.orEmpty().map { it to r.genre } } }
        }.awaitAll().flatten()
        val byId = LinkedHashMap<String, MetaPreview>()
        val genreTags = HashMap<String, MutableSet<String>>()
        pages.forEach { (p, g) -> byId.putIfAbsent(p.id, p); genreTags.getOrPut(p.id) { mutableSetOf() }.apply { addAll(p.genres); g?.let(::add) } }
        val movies = byId.values.filter { it.type == "movie" }
        val shows = byId.values.filter { it.type == "series" }.take(30)

        // 2. Episode lists for shows via the existing meta repository.
        val tracks = shows.map { s -> async { gate.withPermit { runCatching { withTimeoutOrNull(15_000) { MetaDetailsRepository.fetch("series", s.id, cacheResult = false) } }.getOrNull()?.let(::showTrack) } } }
            .awaitAll().filterNotNull().associateBy { it.meta.id }

        val local = LocalLibraryRepository.state.value.items
        val channels = mutableListOf<GuideChannel>()
        fun add(key: String, label: String, icon: String, blocks: List<GuideBlock>) { if (blocks.isNotEmpty()) channels += GuideChannel(key, label, icon, channels.size + 1, blocks) }

        val localMovies = local.filter { !it.isEpisode }
        add("mix", "Moovie Mix", ICONS["mix"]!!, mixedChannel("mix", movies.take(15) + tracks.values.take(10) + localMovies.take(10), now, from, to))
        add("movies", "Movies", ICONS["movies"]!!, mixedChannel("movies", movies.take(25), now, from, to))
        add("local", "My Movies", ICONS["local"]!!, mixedChannel("local", localMovies, now, from, to))
        GENRES.forEach { (key, label, keys) ->
            val gm = movies.filter { matchesGenre(genreTags[it.id].orEmpty().toList(), keys) }.take(12)
            val gs = tracks.values.filter { matchesGenre(genreTags[it.meta.id].orEmpty().toList() + it.meta.genres, keys) }.take(8)
            add(key, label, ICONS[key]!!, mixedChannel(key, gm + gs, now, from, to))
        }
        // Per-show channels: local shows first, then addon shows.
        local.filter { it.isEpisode }.groupBy { it.show!! }.forEach { (show, eps) ->
            val ordered = eps.sortedWith(compareBy({ it.season ?: 0 }, { it.episode ?: 0 }))
            var i = 0
            add("local:$show", show, "📺", schedule(null, { ordered.getOrNull(i++)?.let(::localBlock) }, now, from, to))
        }
        tracks.values.take(12).forEach { t -> add("show:${t.meta.id}", t.meta.name, "📺", seriesChannel(t, now, from, to)) }
        GuideData(now, from, to, channels)
    }
}

internal fun currentTimeMs(): Long = io.ktor.util.date.GMTDate().timestamp
