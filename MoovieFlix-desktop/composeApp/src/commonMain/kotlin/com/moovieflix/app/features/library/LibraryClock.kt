package com.moovieflix.app.features.library

internal expect object LibraryClock {
    fun nowEpochMs(): Long
}
