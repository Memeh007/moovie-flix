package com.moovieflix.tv.locallibrary

/**
 * Moovie Flix local library: turns a video filename such as
 * "The.Matrix.1999.1080p.BluRay.x264.mkv" into a display title and year.
 */
object LocalMediaParser {
    val VIDEO_EXTENSIONS = setOf(
        "mkv", "mp4", "m4v", "avi", "mov", "wmv", "webm", "ts", "m2ts", "mpg", "mpeg", "flv", "3gp", "ogv"
    )

    private val yearRegex = Regex("""(?<!\d)(19\d{2}|20\d{2})(?!\d)""")
    private val junkRegex = Regex(
        """(?i)\b(2160p|1080p|720p|480p|4k|uhd|hdr10?\+?|hdr|dv|dolby\s?vision|bluray|blu-ray|brrip|bdrip|web-?dl|webrip|web|hdtv|dvdrip|remux|x264|x265|h\.?264|h\.?265|hevc|avc|aac|ac3|eac3|dts(-hd)?|truehd|atmos|10bit|8bit|proper|repack|extended|unrated|imax|multi|subs?)\b.*$"""
    )

    fun isVideo(name: String): Boolean =
        name.substringAfterLast('.', "").lowercase() in VIDEO_EXTENSIONS

    data class Parsed(val title: String, val year: Int?)

    fun parse(fileName: String): Parsed {
        var base = fileName.substringBeforeLast('.')
        base = base.replace(Regex("""\[[^\]]*]"""), " ")
        val spaced = base.replace('.', ' ').replace('_', ' ').replace(Regex("""\s+"""), " ").trim()
        // Use the last plausible year so titles like "2001 A Space Odyssey 1968" work.
        val match = yearRegex.findAll(spaced).lastOrNull { it.range.first > 0 }
        val year = match?.value?.toIntOrNull()
        var title = if (match != null) spaced.substring(0, match.range.first) else spaced
        title = title.replace(junkRegex, "")
        title = title.replace(Regex("""[(\[{\-\s]+$"""), "").trim()
        if (title.isBlank()) title = spaced
        return Parsed(title, year)
    }
}
