package com.moovieflix.app.features.downloads

internal expect object DownloadsClock {
    fun nowEpochMs(): Long
}
