package com.moovieflix.tv.locallibrary

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.moovieflix.tv.R
import kotlinx.coroutines.launch

/** Moovie Flix brand colours (from the Moovie Flix web app). */
private val MfPanel = Color(0xFF16161D)
private val MfAccent = Color(0xFFF5B83D)
private val MfMuted = Color(0xFF9B98A6)

/**
 * A compact "My Movies" row shown above the Library screen. Movies are local files from
 * folders the user added in [LocalLibraryScreen].
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LocalLibraryRow(
    onManage: () -> Unit,
    onPlay: (LocalMovie) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { LocalLibraryRepository.load(context) }
    val movies by LocalLibraryRepository.movies.collectAsState()

    Column(modifier.fillMaxWidth().padding(start = 48.dp, end = 48.dp, top = 16.dp, bottom = 4.dp)) {
        Text(
            text = stringResource(R.string.local_library_title),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(end = 24.dp)) {
            item {
                Card(onClick = onManage, modifier = Modifier.size(width = 150.dp, height = 84.dp)) {
                    Box(Modifier.fillMaxSize().background(MfPanel), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (movies.isEmpty()) stringResource(R.string.local_library_add_folders)
                            else stringResource(R.string.local_library_manage),
                            color = MfAccent, fontSize = 14.sp, modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
            items(movies, key = { it.uri }) { movie ->
                LocalMovieCard(movie) { onPlay(movie) }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LocalMovieCard(movie: LocalMovie, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.size(width = 200.dp, height = 84.dp)) {
        Column(
            Modifier.fillMaxSize().background(MfPanel).padding(10.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(movie.title, color = Color.White, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            movie.year?.let { Text(it.toString(), color = MfMuted, fontSize = 12.sp) }
        }
    }
}

/** Manage folder sources and rescan. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LocalLibraryScreen(onPlay: (LocalMovie) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { LocalLibraryRepository.load(context) }
    val sources by LocalLibraryRepository.sources.collectAsState()
    val movies by LocalLibraryRepository.movies.collectAsState()
    val scanning by LocalLibraryRepository.scanning.collectAsState()
    var httpUrl by remember { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            LocalLibraryRepository.addTreeUri(context, uri)
            scope.launch { LocalLibraryRepository.rescan(context) }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(stringResource(R.string.local_library_title), style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Text(stringResource(R.string.local_library_description), color = MfMuted, fontSize = 14.sp)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { runCatching { picker.launch(null) } }) {
                    Text(stringResource(R.string.local_library_add_folder))
                }
                Button(onClick = { scope.launch { LocalLibraryRepository.rescan(context) } }, enabled = !scanning) {
                    Text(if (scanning) stringResource(R.string.local_library_scanning) else stringResource(R.string.local_library_rescan))
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BasicTextField(
                    value = httpUrl,
                    onValueChange = { httpUrl = it },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                    cursorBrush = SolidColor(MfAccent),
                    modifier = Modifier.width(420.dp)
                        .background(MfPanel, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    decorationBox = { inner ->
                        if (httpUrl.isEmpty()) Text(stringResource(R.string.local_library_http_hint), color = MfMuted, fontSize = 16.sp)
                        inner()
                    }
                )
                Button(onClick = {
                    val url = httpUrl.trim()
                    if (url.startsWith("http://") || url.startsWith("https://")) {
                        LocalLibraryRepository.addSource(context, url)
                        httpUrl = ""
                        scope.launch { LocalLibraryRepository.rescan(context) }
                    }
                }) { Text(stringResource(R.string.local_library_add_http)) }
            }
        }
        item { Text(stringResource(R.string.local_library_sources, sources.size), color = MfAccent) }
        items(sources, key = { "src:$it" }) { src ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(Uri.decode(src), color = Color.White, fontSize = 13.sp, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Button(onClick = { LocalLibraryRepository.removeSource(context, src) }) {
                    Text(stringResource(R.string.local_library_remove))
                }
            }
        }
        item { Text(stringResource(R.string.local_library_movies, movies.size), color = MfAccent) }
        items(movies, key = { "mv:${it.uri}" }) { movie ->
            Card(onClick = { onPlay(movie) }, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().background(MfPanel).padding(12.dp)) {
                    Text(
                        movie.title + (movie.year?.let { " ($it)" } ?: ""),
                        color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f)
                    )
                    Text(movie.fileName, color = MfMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(360.dp))
                }
            }
        }
    }
}
