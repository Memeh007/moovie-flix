package com.moovieflix.app.features.collection

import com.moovieflix.app.core.storage.DesktopStorage
import com.moovieflix.app.core.storage.ProfileScopedKey

internal actual object CollectionMobileSettingsStorage {
    private val store = DesktopStorage.store("nuvio_collection_mobile_settings")

    actual fun loadPayload(): String? =
        store.getString(ProfileScopedKey.of("collection_mobile_settings"))

    actual fun savePayload(payload: String) {
        store.putString(ProfileScopedKey.of("collection_mobile_settings"), payload)
    }
}
