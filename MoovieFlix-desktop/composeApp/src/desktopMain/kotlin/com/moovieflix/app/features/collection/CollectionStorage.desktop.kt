package com.moovieflix.app.features.collection

import com.moovieflix.app.core.storage.DesktopStorage
import com.moovieflix.app.core.storage.ProfileScopedKey

internal actual object CollectionStorage {
    private val store = DesktopStorage.store("nuvio_collections")

    actual fun loadPayload(): String? =
        store.getString(ProfileScopedKey.of("collections"))

    actual fun savePayload(payload: String) {
        store.putString(ProfileScopedKey.of("collections"), payload)
    }
}
