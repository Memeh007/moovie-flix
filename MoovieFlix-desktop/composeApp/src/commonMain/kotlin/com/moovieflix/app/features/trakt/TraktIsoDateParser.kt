package com.moovieflix.app.features.trakt

import com.moovieflix.app.core.time.parseZonedIsoDateTimeToEpochMs

internal fun parseTraktIsoDateTimeToEpochMs(value: String): Long? =
    parseZonedIsoDateTimeToEpochMs(value)
