package com.moovieflix.app.features.profiles

import com.moovieflix.app.core.storage.DesktopStorage

internal actual object AvatarStorage {
    private val store = DesktopStorage.store("nuvio_avatars")

    actual fun loadPayload(): String? = store.getString("avatars")

    actual fun savePayload(payload: String) {
        store.putString("avatars", payload)
    }
}
