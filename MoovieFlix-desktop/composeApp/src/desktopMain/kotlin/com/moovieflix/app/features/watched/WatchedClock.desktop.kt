package com.moovieflix.app.features.watched

actual object WatchedClock {
    actual fun nowEpochMs(): Long = System.currentTimeMillis()
}
