package com.moovieflix.app.core.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
internal expect fun MoovieFlixDesktopVerticalScrollbar(
    state: LazyListState,
    modifier: Modifier = Modifier,
    backgroundColor: Color? = null,
)

@Composable
internal expect fun MoovieFlixDesktopVerticalScrollbar(
    state: LazyGridState,
    modifier: Modifier = Modifier,
    backgroundColor: Color? = null,
)

@Composable
internal expect fun MoovieFlixDesktopVerticalScrollbar(
    state: ScrollState,
    modifier: Modifier = Modifier,
    backgroundColor: Color? = null,
)
