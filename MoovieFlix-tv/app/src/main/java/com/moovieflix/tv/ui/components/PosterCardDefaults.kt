package com.moovieflix.tv.ui.components

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import com.moovieflix.tv.ui.theme.MoovieFlixComponents

val LocalLandscapePosterMode = staticCompositionLocalOf { false }
val LocalAlwaysBackdropWithLogo = staticCompositionLocalOf { false }

@Immutable
data class PosterCardStyle(
    val width: Dp = MoovieFlixComponents.tokens.posterCard.width,
    val height: Dp = MoovieFlixComponents.tokens.posterCard.height,
    val cornerRadius: Dp = MoovieFlixComponents.tokens.posterCard.cornerRadius,
    val focusedBorderWidth: Dp = MoovieFlixComponents.tokens.posterCard.focusedBorderWidth,
    val focusedScale: Float = MoovieFlixComponents.tokens.posterCard.focusedScale
) {
    val aspectRatio: Float
        get() = width.value / height.value
}

object PosterCardDefaults {
    val Style = PosterCardStyle()
}
