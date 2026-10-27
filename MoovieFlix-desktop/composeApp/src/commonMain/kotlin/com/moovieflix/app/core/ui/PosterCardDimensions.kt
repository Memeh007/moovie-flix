package com.moovieflix.app.core.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.moovieflix.app.isDesktop
import kotlin.math.roundToInt

internal const val PosterLandscapeAspectRatio = 1.77f
internal const val MoovieFlixDesktopCatalogPosterScale = 1.4f
private const val PosterLandscapeWidthScale = 180f / 110f

internal fun catalogPosterBaseWidthDp(
    basePosterWidthDp: Int,
    useDesktopSizing: Boolean = isDesktop,
): Int =
    if (useDesktopSizing) {
        (basePosterWidthDp * MoovieFlixDesktopCatalogPosterScale).roundToInt()
    } else {
        basePosterWidthDp
    }

internal fun landscapePosterWidth(basePosterWidthDp: Int): Dp =
    (basePosterWidthDp * PosterLandscapeWidthScale).dp

internal fun landscapePosterHeightForWidth(width: Dp): Dp =
    (width.value / PosterLandscapeAspectRatio).dp
