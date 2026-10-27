package com.moovieflix.app.features.watched

expect object WatchedClock {
    fun nowEpochMs(): Long
}

