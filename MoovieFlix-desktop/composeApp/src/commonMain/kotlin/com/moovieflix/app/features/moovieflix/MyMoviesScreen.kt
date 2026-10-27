package com.moovieflix.app.features.moovieflix

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Moovie Flix palette (apps/web): background, accent, card. */
internal object MfColors {
    val Bg = Color(0xFF0B0B0F)
    val Accent = Color(0xFFF5B83D)
    val Card = Color(0xFF16161D)
    val Prog = Color(0xFF1C1C26)
    val ProgBorder = Color(0xFF2C2C38)
    val Live = Color(0xFF2A2416)
    val LiveBorder = Color(0xFF5A4720)
    val GridBg = Color(0xFF0F0F15)
    val Muted = Color(0xFF9A97A6)
    val Text = Color(0xFFECEAF2)
    val LiveDot = Color(0xFFFF5A4A)
}

@Composable
internal fun MfButton(label: String, primary: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier.clip8().background(if (primary) MfColors.Accent else MfColors.Card)
            .border(1.dp, if (primary) MfColors.Accent else MfColors.ProgBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 9.dp),
    ) { Text(label, color = if (primary) MfColors.Bg else MfColors.Text, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
}

internal fun Modifier.clip8(): Modifier = this.clip(RoundedCornerShape(8.dp))

@Composable
fun MyMoviesScreen(onBack: () -> Unit) {
    LaunchedEffect(Unit) { LocalLibraryRepository.ensureLoaded() }
    val state by LocalLibraryRepository.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().background(MfColors.Bg).padding(horizontal = 32.dp, vertical = 24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MfButton("‹ Back", primary = false, onClick = onBack)
            Spacer(Modifier.width(16.dp))
            Text("My Movies", color = MfColors.Text, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            if (state.supported) {
                MfButton("+ Add folder") { LocalLibraryRepository.addFolders() }
                Spacer(Modifier.width(10.dp))
                MfButton(if (state.scanning) "Scanning…" else "⟳ Rescan library", primary = false) { LocalLibraryRepository.rescan() }
                MoovieFlixHooks.mediaShare?.let { share ->
                    var shareOn by remember { mutableStateOf(share.isEnabled()) }
                    Spacer(Modifier.width(12.dp))
                    MfButton(if (shareOn) "\u25CF Share to TV: On" else "Share to TV: Off", primary = false) { shareOn = !shareOn; share.setEnabled(shareOn) }
                }
            }
        }
        MoovieFlixHooks.tvPairing?.let { pairing ->
            var showPair by remember { mutableStateOf(false) }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                MfButton(if (showPair) "Hide TV pairing" else "Pair a TV", primary = false) { showPair = !showPair }
                if (showPair) {
                    Spacer(Modifier.width(16.dp))
                    val code = pairing.pairingCode().orEmpty()
                    Text("Code: " + code.chunked(3).joinToString(" "), color = MfColors.Accent, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(16.dp))
                    Text("Open Moovie Flix on the TV and enter this code. Address (if asked): " + pairing.serverAddress().orEmpty() +
                        pairing.pairedDevices().takeIf { it.isNotEmpty() }?.let { "  ·  Paired: " + it.joinToString() }.orEmpty(), color = MfColors.Muted, fontSize = 13.sp)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        if (!state.supported) {
            Text("Local folders are available in the Moovie Flix desktop app.", color = MfColors.Muted)
            return@Column
        }
        Text("Library folders", color = MfColors.Accent, fontWeight = FontWeight.Bold)
        if (state.folders.isEmpty()) {
            Text("No folders yet. Add a folder of movies or TV you own and they become playable here and in the TV Guide.", color = MfColors.Muted)
        }
        state.folders.forEach { f ->
            Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(f, color = MfColors.Text, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Remove", color = MfColors.Muted, modifier = Modifier.clickable { LocalLibraryRepository.removeFolder(f) }.padding(6.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("${state.items.size} files" + if (state.scanning) " · scanning…" else "", color = MfColors.Muted, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        LazyVerticalGrid(columns = GridCells.Adaptive(220.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(state.items, key = { it.path }) { item ->
                Column(
                    Modifier.clip8().background(MfColors.Card).border(1.dp, MfColors.ProgBorder, RoundedCornerShape(8.dp))
                        .clickable { MoovieFlixHooks.playLocalFile?.invoke(item) }.padding(14.dp),
                ) {
                    Text(item.show ?: item.title, color = MfColors.Text, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    val sub = if (item.isEpisode) "S%02dE%02d".format2(item.season ?: 0, item.episode ?: 0) else item.year?.toString() ?: "Movie"
                    Text("▶ $sub", color = MfColors.Accent, fontSize = 13.sp)
                }
            }
        }
    }
}

internal fun String.format2(a: Int, b: Int): String =
    "S" + a.toString().padStart(2, '0') + "E" + b.toString().padStart(2, '0')


/** Library tab shortcuts: My Movies and TV Guide (shown only when the hooks are registered). */
@Composable
fun MoovieFlixLibraryShortcuts() {
    val myMovies = MoovieFlixHooks.openMyMovies
    val guide = MoovieFlixHooks.openTvGuide
    if (myMovies == null && guide == null) return
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (myMovies != null && MoovieFlixHooks.localLibrary != null) { MfButton("My Movies", primary = false, onClick = myMovies); Spacer(Modifier.width(8.dp)) }
        if (guide != null) { MfButton("TV Guide", primary = true, onClick = guide); Spacer(Modifier.width(8.dp)) }
    }
}
