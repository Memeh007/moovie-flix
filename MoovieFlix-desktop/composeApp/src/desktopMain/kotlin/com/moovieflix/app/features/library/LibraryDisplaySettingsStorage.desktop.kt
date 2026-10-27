package com.moovieflix.app.features.library

import com.moovieflix.app.core.storage.DesktopStorage
import com.moovieflix.app.core.storage.ProfileScopedKey

internal actual object LibraryDisplaySettingsStorage {
    private const val payloadKey = "library_display_settings_payload"
    private val store = DesktopStorage.store("nuvio_library_display_settings")

    actual fun loadPayload(): String? =
        store.getString(ProfileScopedKey.of(payloadKey))

    actual fun savePayload(payload: String) {
        store.putString(ProfileScopedKey.of(payloadKey), payload)
    }
}
