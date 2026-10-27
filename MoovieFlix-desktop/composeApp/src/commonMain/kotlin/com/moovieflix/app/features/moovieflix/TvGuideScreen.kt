package com.moovieflix.app.features.moovieflix

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moovieflix.app.features.watched.WatchedRepository
import kotlinx.coroutines.delay

/** Remembers what the guide last started so it can continue with the next block when playback ends. */
object TvGuideSession {
    var data: GuideData? = null
    var lastChannel: String? = null
    var lastBlock: GuideBlock? = null
    var autoContinue: Boolean = true
}

private const val PX_PER_MIN = 7f
private val CHANNEL_COL = 170.dp

private fun hhmm(ms: Long): String {
    val local = ms + MoovieFlixHooks.zoneOffsetMs(ms)
    val minOfDay = ((local / 60_000L) % 1440 + 1440) % 1440
    val hour = (minOfDay / 60).toInt(); val minute = (minOfDay % 60).toInt()
    val h = if (hour % 12 == 0) 12 else hour % 12
    return "$h:${minute.toString().padStart(2, '0')}${if (hour < 12) "am" else "pm"}"
}

@Composable
fun TvGuideScreen(onBack: () -> Unit) {
    var data by remember { mutableStateOf(TvGuideSession.data) }
    var loading by remember { mutableStateOf(false) }
    var reload by remember { mutableStateOf(0) }
    var now by remember { mutableStateOf(currentTimeMs()) }
    var focused by remember { mutableStateOf<GuideBlock?>(null) }

    fun play(channel: GuideChannel, block: GuideBlock) {
        TvGuideSession.lastChannel = channel.key
        TvGuideSession.lastBlock = block
        if (block.local != null) MoovieFlixHooks.playLocalFile?.invoke(block.local) else MoovieFlixHooks.playAddonItem?.invoke(block)
    }

    LaunchedEffect(reload) {
        val stale = data == null || reload > 0 || currentTimeMs() > (data!!.from + 2 * 3600_000L)
        if (stale) {
            loading = true
            LocalLibraryRepository.ensureLoaded()
            data = runCatching { TvGuideBuilder.build() }.getOrNull() ?: data
            TvGuideSession.data = data
            loading = false
        }
    }
    // Continue to the next block on the channel once the previous addon item finished (marked watched).
    LaunchedEffect(data) {
        val d = data ?: return@LaunchedEffect
        val last = TvGuideSession.lastBlock ?: return@LaunchedEffect
        val ch = d.channels.firstOrNull { it.key == TvGuideSession.lastChannel } ?: return@LaunchedEffect
        TvGuideSession.lastBlock = null
        if (!TvGuideSession.autoContinue || last.local != null) return@LaunchedEffect
        val finished = if (last.type == "series") WatchedRepository.isWatched(last.metaId, "series", last.season, last.episode)
        else WatchedRepository.isWatched(last.metaId, "movie", null, null)
        if (!finished) return@LaunchedEffect
        val i = ch.blocks.indexOfFirst { it.videoId == last.videoId }
        val next = ch.blocks.getOrNull(i + 1) ?: ch.blocks.firstOrNull { it.end > currentTimeMs() && it.videoId != last.videoId }
        if (next != null) { delay(600); play(ch, next) }
    }
    LaunchedEffect(Unit) { while (true) { delay(30_000); now = currentTimeMs() } }

    val d = data
    val live = focused ?: d?.channels?.firstOrNull()?.blocks?.firstOrNull { now in it.start until it.end }
    Column(Modifier.fillMaxSize().background(MfColors.Bg).padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MfButton("← Back", false, onBack)
            Spacer(Modifier.width(16.dp))
            Text("TV Guide", color = MfColors.Text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(16.dp))
            Text(hhmm(now), color = MfColors.Muted, fontSize = 16.sp)
            Spacer(Modifier.weight(1f))
            MfButton(if (TvGuideSession.autoContinue) "Auto-continue: On" else "Auto-continue: Off", false) { TvGuideSession.autoContinue = !TvGuideSession.autoContinue; reload = reload }
            Spacer(Modifier.width(8.dp))
            MfButton(if (loading) "Loading…" else "⟳ Refresh", true) { if (!loading) reload++ }
        }
        Spacer(Modifier.height(16.dp))
        // Header: focused programme
        Box(Modifier.fillMaxWidth().height(130.dp).clip(RoundedCornerShape(12.dp)).background(MfColors.Card).padding(18.dp)) {
            if (live != null) Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${hhmm(live.start)} – ${hhmm(live.end)}", color = MfColors.Muted, fontSize = 13.sp)
                    if (now in live.start until live.end) {
                        Spacer(Modifier.width(10.dp)); Box(Modifier.size(8.dp).clip(CircleShape).background(MfColors.LiveDot))
                        Spacer(Modifier.width(4.dp)); Text("LIVE", color = MfColors.LiveDot, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text(live.title, color = MfColors.Text, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                live.subtitle?.let { Text(it, color = MfColors.Muted, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                Text(if (live.local != null) "From My Movies" else "Select to play now", color = MfColors.Accent, fontSize = 12.sp)
            } else Text(if (loading) "Building your guide from installed addons and My Movies…" else "No programmes yet. Install addons or add a folder in My Movies.", color = MfColors.Muted)
        }
        Spacer(Modifier.height(16.dp))
        if (d == null || d.channels.isEmpty()) return@Column
        val hScroll = rememberScrollState()
        val totalMin = ((d.to - d.from) / 60_000L).toInt()
        Column(Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)).background(MfColors.GridBg).border(1.dp, Color(0xFF23232C), RoundedCornerShape(10.dp))) {
            // Time slots row
            Row {
                Spacer(Modifier.width(CHANNEL_COL))
                Row(Modifier.horizontalScroll(hScroll)) {
                    for (i in 0 until totalMin / 30) Box(Modifier.width((30 * PX_PER_MIN).dp).height(32.dp).padding(start = 6.dp), contentAlignment = Alignment.CenterStart) {
                        Text(hhmm(d.from + i * 30 * 60_000L), color = MfColors.Muted, fontSize = 12.sp)
                    }
                }
            }
            Column(Modifier.verticalScroll(rememberScrollState())) {
                d.channels.forEach { ch ->
                    Row(Modifier.height(64.dp)) {
                        Row(Modifier.width(CHANNEL_COL).fillMaxHeight().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${ch.number}", color = MfColors.Muted, fontSize = 12.sp); Spacer(Modifier.width(8.dp))
                            Text(ch.icon, fontSize = 16.sp); Spacer(Modifier.width(6.dp))
                            Text(ch.label, color = MfColors.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Box(Modifier.horizontalScroll(hScroll).width((totalMin * PX_PER_MIN).dp).fillMaxHeight()) {
                            ch.blocks.forEach { b ->
                                val s = maxOf(b.start, d.from); val e = minOf(b.end, d.to)
                                if (e > s) {
                                    val isLive = now in b.start until b.end; val past = b.end <= now
                                    Box(Modifier.offset(x = (((s - d.from) / 60_000f) * PX_PER_MIN).dp).width((((e - s) / 60_000f) * PX_PER_MIN).dp - 3.dp).fillMaxHeight().padding(vertical = 3.dp)
                                        .alpha(if (past) 0.55f else 1f).clip(RoundedCornerShape(6.dp))
                                        .background(if (isLive) MfColors.Live else MfColors.Prog)
                                        .border(1.dp, if (isLive) MfColors.LiveBorder else MfColors.ProgBorder, RoundedCornerShape(6.dp))
                                        .clickable { focused = b; play(ch, b) }.padding(horizontal = 8.dp, vertical = 6.dp)) {
                                        Column {
                                            Text(b.title, color = MfColors.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(listOfNotNull(hhmm(b.start), b.subtitle).joinToString(" · "), color = MfColors.Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        if (isLive) {
                                            val f = ((now - b.start).toFloat() / (b.end - b.start)).coerceIn(0f, 1f)
                                            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth(f).height(3.dp).background(MfColors.Accent))
                                        }
                                    }
                                }
                            }
                            // Wall-clock "now" marker
                            Box(Modifier.offset(x = (((now - d.from) / 60_000f) * PX_PER_MIN).dp).width(2.dp).fillMaxHeight().background(MfColors.LiveDot))
                        }
                    }
                }
            }
        }
    }
}
