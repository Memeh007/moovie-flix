package com.moovieflix.app.core.sync

import com.moovieflix.app.core.storage.DesktopStorage

internal actual object SyncClientIdentityStorage {
    private val store = DesktopStorage.store("nuvio_sync_client_identity")

    actual fun loadClientId(): String? =
        store.getString("client_instance_id")

    actual fun saveClientId(clientId: String) {
        store.putString("client_instance_id", clientId)
    }
}
