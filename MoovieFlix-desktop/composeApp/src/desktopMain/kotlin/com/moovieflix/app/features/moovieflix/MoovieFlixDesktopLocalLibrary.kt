package com.moovieflix.app.features.moovieflix

import com.moovieflix.app.core.storage.DesktopStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.swing.JFileChooser
import javax.swing.SwingUtilities

/** Desktop implementation of the Moovie Flix "My Movies" folder picker + recursive scanner. */
object MoovieFlixDesktopLocalLibrary : MoovieFlixLocalLibraryPlatform {
    private val store by lazy { DesktopStorage.store("moovieflix_local_library") }

    fun install() {
        MoovieFlixHooks.localLibrary = this
        MoovieFlixHooks.zoneOffsetMs = { java.util.TimeZone.getDefault().getOffset(it).toLong() }
        MoovieFlixHooks.mediaShare = MoovieFlixMediaShare
        MoovieFlixMediaShare.startIfEnabled()
    }

    override suspend fun pickFolders(): List<String> = withContext(Dispatchers.IO) {
        var result: List<String> = emptyList()
        SwingUtilities.invokeAndWait {
            val chooser = JFileChooser().apply {
                fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                isMultiSelectionEnabled = true
                dialogTitle = "Moovie Flix - choose movie folders"
            }
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                result = (chooser.selectedFiles?.takeIf { it.isNotEmpty() } ?: arrayOf(chooser.selectedFile)).filterNotNull().map { it.absolutePath }
            }
        }
        result
    }

    override fun loadFolders(): List<String> = store.getStringSet("folders")?.sorted().orEmpty()

    override fun saveFolders(folders: List<String>) {
        store.putStringSet("folders", folders.toSet())
        MoovieFlixMediaShare.onFoldersChanged()
    }

    override suspend fun scan(folders: List<String>): List<LocalMediaItem> = withContext(Dispatchers.IO) {
        val out = mutableListOf<LocalMediaItem>()
        fun walk(dir: File, root: File, depth: Int) {
            if (depth > 12) return
            dir.listFiles()?.forEach { f ->
                if (f.name.startsWith(".")) return@forEach
                if (f.isDirectory) walk(f, root, depth + 1)
                else if (LocalMediaParser.isVideo(f.name)) {
                    val rel = f.parentFile.relativeTo(root).path.split(File.separatorChar).filter { it.isNotBlank() }
                    out += LocalMediaParser.toItem(f.absolutePath, f.toURI().toString(), f.name, rel)
                }
            }
        }
        folders.forEach { p -> File(p).takeIf { it.isDirectory }?.let { walk(it, it, 0) } }
        out
    }
}
