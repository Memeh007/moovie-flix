package com.moovieflix.app.features.details

import com.moovieflix.app.core.storage.DesktopStorage
import com.moovieflix.app.core.storage.ProfileScopedKey

internal actual object MetaScreenSettingsStorage {
    private val store = DesktopStorage.store("nuvio_meta_screen_settings")

    actual fun loadPayload(): String? =
        store.getString(ProfileScopedKey.of("meta_screen_settings"))

    actual fun savePayload(payload: String) {
        store.putString(ProfileScopedKey.of("meta_screen_settings"), payload)
    }
}
