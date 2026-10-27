package com.moovieflix.app.features.watchprogress

expect object CurrentDateProvider {
    fun todayIsoDate(): String
    fun localStartOfDayEpochMs(isoDate: String): Long?
}
