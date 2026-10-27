package com.moovieflix.app.features.watchprogress

internal actual object WatchProgressClock {
    actual fun nowEpochMs(): Long = System.currentTimeMillis()
}
