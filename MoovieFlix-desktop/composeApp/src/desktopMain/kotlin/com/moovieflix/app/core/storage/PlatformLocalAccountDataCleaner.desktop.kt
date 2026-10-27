package com.moovieflix.app.core.storage

internal actual object PlatformLocalAccountDataCleaner {
    actual fun wipe() {
        DesktopStorage.wipe()
    }
}
