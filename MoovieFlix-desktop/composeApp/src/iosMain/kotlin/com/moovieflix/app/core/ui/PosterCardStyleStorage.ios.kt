package com.moovieflix.app.core.ui

import com.moovieflix.app.core.storage.ProfileScopedKey
import platform.Foundation.NSUserDefaults

actual object PosterCardStyleStorage {
    private const val payloadKey = "poster_card_style_payload"

    actual fun loadPayload(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(payloadKey))

    actual fun savePayload(payload: String) {
        NSUserDefaults.standardUserDefaults.setObject(payload, forKey = ProfileScopedKey.of(payloadKey))
    }
}