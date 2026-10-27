package com.moovieflix.app.features.profiles

import com.moovieflix.app.core.storage.DesktopStorage

internal actual object ProfileStorage {
    private val store = DesktopStorage.store("nuvio_profiles")

    actual fun loadPayload(): String? = store.getString("profiles")

    actual fun savePayload(payload: String) {
        store.putString("profiles", payload)
    }
}
