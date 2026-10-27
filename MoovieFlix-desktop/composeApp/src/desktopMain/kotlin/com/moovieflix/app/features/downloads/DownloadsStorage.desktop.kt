package com.moovieflix.app.features.downloads

import com.moovieflix.app.core.storage.DesktopStorage
import com.moovieflix.app.core.storage.ProfileScopedKey

internal actual object DownloadsStorage {
    private val store = DesktopStorage.store("nuvio_downloads")

    actual fun loadPayload(): String? =
        store.getString(ProfileScopedKey.of("downloads"))

    actual fun savePayload(payload: String) {
        store.putString(ProfileScopedKey.of("downloads"), payload)
    }
}
