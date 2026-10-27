package com.moovieflix.app.features.watchprogress

internal expect object WatchProgressClock {
    fun nowEpochMs(): Long
}
