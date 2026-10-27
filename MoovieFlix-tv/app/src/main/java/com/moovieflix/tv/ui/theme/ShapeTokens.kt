package com.moovieflix.tv.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class MoovieFlixRadiusTokens(
    val none: Dp,
    val xxs: Dp,
    val xs: Dp,
    val sm: Dp,
    val md: Dp,
    val lg: Dp,
    val xl: Dp,
    val xxl: Dp,
    val panel: Dp,
    val full: Dp
)

@Immutable
data class MoovieFlixShapeTokens(
    val posterCard: Shape,
    val backdropCard: Shape,
    val collectionCard: Shape,
    val button: Shape,
    val iconButton: Shape,
    val chip: Shape,
    val badge: Shape,
    val dialog: Shape,
    val sidePanel: Shape,
    val sidebar: Shape,
    val navItem: Shape,
    val progress: Shape,
    val slider: Shape,
    val field: Shape,
    val menu: Shape,
    val circle: Shape
)

object MoovieFlixRadii {
    val tokens = MoovieFlixRadiusTokens(
        none = 0.dp,
        xxs = 2.dp,
        xs = 4.dp,
        sm = 8.dp,
        md = 12.dp,
        lg = 14.dp,
        xl = 16.dp,
        xxl = 20.dp,
        panel = 28.dp,
        full = 999.dp
    )
}

object MoovieFlixShapes {
    val tokens = MoovieFlixShapeTokens(
        posterCard = RoundedCornerShape(MoovieFlixRadii.tokens.md),
        backdropCard = RoundedCornerShape(MoovieFlixRadii.tokens.xl),
        collectionCard = RoundedCornerShape(MoovieFlixRadii.tokens.xl),
        button = RoundedCornerShape(MoovieFlixRadii.tokens.md),
        iconButton = RoundedCornerShape(MoovieFlixRadii.tokens.md),
        chip = RoundedCornerShape(MoovieFlixRadii.tokens.full),
        badge = RoundedCornerShape(MoovieFlixRadii.tokens.xs),
        dialog = RoundedCornerShape(MoovieFlixRadii.tokens.xl),
        sidePanel = RoundedCornerShape(MoovieFlixRadii.tokens.xxl),
        sidebar = RoundedCornerShape(30.dp),
        navItem = RoundedCornerShape(MoovieFlixRadii.tokens.full),
        progress = RoundedCornerShape(MoovieFlixRadii.tokens.xxs),
        slider = RoundedCornerShape(MoovieFlixRadii.tokens.full),
        field = RoundedCornerShape(MoovieFlixRadii.tokens.md),
        menu = RoundedCornerShape(MoovieFlixRadii.tokens.lg),
        circle = CircleShape
    )
}
