package com.moovieflix.app.features.downloads

internal actual object DownloadsClock {
    actual fun nowEpochMs(): Long = System.currentTimeMillis()
}
