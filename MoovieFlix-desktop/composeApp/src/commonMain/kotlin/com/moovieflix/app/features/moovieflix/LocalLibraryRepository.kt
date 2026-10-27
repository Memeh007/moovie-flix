package com.moovieflix.app.features.moovieflix

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LocalLibraryState(
    val supported: Boolean = false,
    val folders: List<String> = emptyList(),
    val items: List<LocalMediaItem> = emptyList(),
    val scanning: Boolean = false,
    val lastScanCount: Int? = null,
)

/** Moovie Flix "My Movies": folders the user owns media in, scanned on demand. */
object LocalLibraryRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(LocalLibraryState())
    val state: StateFlow<LocalLibraryState> = _state.asStateFlow()
    private var loaded = false

    fun ensureLoaded() {
        val platform = MoovieFlixHooks.localLibrary ?: return
        if (loaded) return
        loaded = true
        _state.value = _state.value.copy(supported = true, folders = platform.loadFolders())
        rescan()
    }

    fun addFolders() {
        val platform = MoovieFlixHooks.localLibrary ?: return
        scope.launch {
            val picked = platform.pickFolders()
            if (picked.isEmpty()) return@launch
            val folders = (_state.value.folders + picked).distinct()
            platform.saveFolders(folders)
            _state.value = _state.value.copy(folders = folders)
            rescan()
        }
    }

    fun removeFolder(folder: String) {
        val platform = MoovieFlixHooks.localLibrary ?: return
        val folders = _state.value.folders - folder
        platform.saveFolders(folders)
        _state.value = _state.value.copy(folders = folders)
        rescan()
    }

    fun rescan() {
        val platform = MoovieFlixHooks.localLibrary ?: return
        if (_state.value.scanning) return
        _state.value = _state.value.copy(scanning = true)
        scope.launch {
            val items = runCatching { platform.scan(_state.value.folders) }.getOrDefault(emptyList())
                .sortedWith(compareBy({ it.show ?: it.title.lowercase() }, { it.season ?: 0 }, { it.episode ?: 0 }))
            _state.value = _state.value.copy(items = items, scanning = false, lastScanCount = items.size)
        }
    }
}
