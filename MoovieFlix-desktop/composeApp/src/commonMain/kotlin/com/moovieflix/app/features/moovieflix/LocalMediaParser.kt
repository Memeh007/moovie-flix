package com.moovieflix.app.features.moovieflix

/**
 * Filename parser shared by the Moovie Flix local library. Movie title/year logic is the
 * Android TV app's LocalMediaParser; the SxxEyy / 1x02 / "Season N" episode rules mirror the
 * original Moovie Flix server (apps/server/src/parse.ts).
 */
object LocalMediaParser {
    val VIDEO_EXTENSIONS = setOf(
        "mkv", "mp4", "m4v", "avi", "mov", "wmv", "webm", "ts", "m2ts", "mpg", "mpeg", "flv", "3gp", "ogv"
    )

    private val yearRegex = Regex("""(?<!\d)(19\d{2}|20\d{2})(?!\d)""")
    private val junkRegex = Regex(
        """(?i)\b(2160p|1080p|720p|480p|4k|uhd|hdr10?\+?|hdr|dv|dolby\s?vision|bluray|blu-ray|brrip|bdrip|web-?dl|webrip|web|hdtv|dvdrip|remux|x264|x265|h\.?264|h\.?265|hevc|avc|aac|ac3|eac3|dts(-hd)?|truehd|atmos|10bit|8bit|proper|repack|extended|unrated|imax|multi|subs?)\b.*$"""
    )
    private val sxe = Regex("""^(.*?)[\s._-]*[Ss](\d{1,2})[\s._-]*[Ee](\d{1,3})""")
    private val nxn = Regex("""^(.*?)[\s._-]+(\d{1,2})x(\d{2,3})""")
    private val seasonDir = Regex("""(?i)^season[\s._-]*(\d+)""")

    fun isVideo(name: String): Boolean =
        name.substringAfterLast('.', "").lowercase() in VIDEO_EXTENSIONS

    data class Parsed(val title: String, val year: Int?)

    fun parse(fileName: String): Parsed {
        var base = fileName.substringBeforeLast('.')
        base = base.replace(Regex("""\[[^\]]*]"""), " ")
        val spaced = base.replace('.', ' ').replace('_', ' ').replace(Regex("""\s+"""), " ").trim()
        val match = yearRegex.findAll(spaced).lastOrNull { it.range.first > 0 }
        val year = match?.value?.toIntOrNull()
        var title = if (match != null) spaced.substring(0, match.range.first) else spaced
        title = title.replace(junkRegex, "")
        title = title.replace(Regex("""[(\[{\-\s]+$"""), "").trim()
        if (title.isBlank()) title = spaced
        return Parsed(title, year)
    }

    private fun clean(s: String): String =
        s.replace('.', ' ').replace('_', ' ').replace(junkRegex, "")
            .replace(Regex("""[\[(].*?[\])]"""), "").replace(Regex("""\s+"""), " ").trim()

    /** [dirs] are the parent directory names from the scan root down to the file's folder. */
    fun toItem(path: String, uri: String, fileName: String, dirs: List<String>): LocalMediaItem {
        val base = fileName.substringBeforeLast('.')
        val showFromDirs = dirs.lastOrNull { !seasonDir.containsMatchIn(it) }?.let(::clean)?.ifBlank { null }
        (sxe.find(base) ?: nxn.find(base))?.let { m ->
            val show = clean(m.groupValues[1]).ifBlank { showFromDirs ?: "Unknown Show" }
            return LocalMediaItem(path, uri, base, null, show, m.groupValues[2].toInt(), m.groupValues[3].toInt())
        }
        dirs.lastOrNull()?.let { seasonDir.find(it) }?.let { sm ->
            val ep = Regex("""(?:^|\D)(\d{1,3})(?:\D|$)""").find(base)?.groupValues?.get(1)?.toIntOrNull()
            val show = dirs.dropLast(1).lastOrNull { !seasonDir.containsMatchIn(it) }?.let(::clean) ?: "Unknown Show"
            return LocalMediaItem(path, uri, base, null, show, sm.groupValues[1].toInt(), ep)
        }
        val p = parse(fileName)
        return LocalMediaItem(path, uri, p.title, p.year, null, null, null)
    }
}
