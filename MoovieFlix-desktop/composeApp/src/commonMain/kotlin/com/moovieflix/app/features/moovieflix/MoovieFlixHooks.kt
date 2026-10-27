package com.moovieflix.app.features.moovieflix

/**
 * Moovie Flix additions (local "My Movies" library, TV Guide, in-app trailers).
 *
 * Platform-neutral hooks so the new features stay additive: the desktop entry point registers
 * the folder picker / scanner, and MainAppContent registers navigation + playback launchers.
 * Nothing here changes addon install, debrid, sync or player internals.
 */
data class LocalMediaItem(
    val path: String,
    val uri: String,
    val title: String,
    val year: Int?,
    val show: String?,
    val season: Int?,
    val episode: Int?,
) {
    val isEpisode: Boolean get() = show != null
}

interface MoovieFlixLocalLibraryPlatform {
    /** Opens the native folder chooser; returns the chosen folders (empty when cancelled). */
    suspend fun pickFolders(): List<String>
    fun loadFolders(): List<String>
    fun saveFolders(folders: List<String>)
    /** Recursively scans [folders] for video files. */
    suspend fun scan(folders: List<String>): List<LocalMediaItem>
}

object MoovieFlixHooks {
    /** Local time-zone offset for guide labels (set by the platform; UTC otherwise). */
    var zoneOffsetMs: (Long) -> Long = { 0L }
    /** Set by the desktop entry point. Null on platforms without a local library. */
    var localLibrary: MoovieFlixLocalLibraryPlatform? = null

    /** Optional LAN "Moovie Flix Media Share" for the TV app (desktop only). */
    var mediaShare: MoovieFlixMediaShareControl? = null

    /** Embedded sync server TV pairing (desktop only, null when an external backend is used). */
    var tvPairing: MoovieFlixTvPairingControl? = null

    /** Set by MainAppContent. */
    var openMyMovies: (() -> Unit)? = null
    var openTvGuide: (() -> Unit)? = null
    var playLocalFile: ((LocalMediaItem) -> Unit)? = null
    var playAddonItem: ((GuideBlock) -> Unit)? = null

    /** Plays a trailer inside the app; returns false when it cannot (caller falls back to the browser). */
    var playTrailerInApp: ((youtubeUrl: String, title: String) -> Boolean)? = null
}

interface MoovieFlixTvPairingControl {
    /** Short code the TV app asks for when pairing. */
    fun pairingCode(): String?
    /** LAN address of this PC's sync server (for manual entry on the TV). */
    fun serverAddress(): String?
    fun pairedDevices(): List<String>
}

interface MoovieFlixMediaShareControl {
    fun isEnabled(): Boolean
    fun setEnabled(on: Boolean)
}
