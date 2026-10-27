package com.moovieflix.app.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

data class MoovieFlixTypeScale(
    val labelXs: TextStyle,
    val labelSm: TextStyle,
    val bodySm: TextStyle,
    val bodyMd: TextStyle,
    val bodyLg: TextStyle,
    val titleSm: TextStyle,
    val titleMd: TextStyle,
    val titleLg: TextStyle,
    val displaySm: TextStyle,
    val displayMd: TextStyle,
)

internal val LocalNuvioTypeScale = staticCompositionLocalOf {
    MoovieFlixTypeScale(
        labelXs = TextStyle(fontSize = MoovieFlixTokens.Type.labelXs, lineHeight = MoovieFlixTokens.LineHeight.labelXs, fontWeight = FontWeight.Medium),
        labelSm = TextStyle(fontSize = MoovieFlixTokens.Type.labelSm, lineHeight = MoovieFlixTokens.LineHeight.labelSm, fontWeight = FontWeight.Medium),
        bodySm = TextStyle(fontSize = MoovieFlixTokens.Type.bodySm, lineHeight = MoovieFlixTokens.LineHeight.bodySm, fontWeight = FontWeight.Normal),
        bodyMd = TextStyle(fontSize = MoovieFlixTokens.Type.bodyMd, lineHeight = MoovieFlixTokens.LineHeight.bodyMd, fontWeight = FontWeight.Normal),
        bodyLg = TextStyle(fontSize = MoovieFlixTokens.Type.bodyLg, lineHeight = MoovieFlixTokens.LineHeight.bodyLg, fontWeight = FontWeight.Medium),
        titleSm = TextStyle(fontSize = MoovieFlixTokens.Type.titleSm, lineHeight = MoovieFlixTokens.LineHeight.titleSm, fontWeight = FontWeight.Bold),
        titleMd = TextStyle(fontSize = MoovieFlixTokens.Type.titleMd, lineHeight = MoovieFlixTokens.LineHeight.titleMd, fontWeight = FontWeight.Bold),
        titleLg = TextStyle(fontSize = MoovieFlixTokens.Type.titleLg, lineHeight = MoovieFlixTokens.LineHeight.titleLg, fontWeight = FontWeight.Bold),
        displaySm = TextStyle(fontSize = MoovieFlixTokens.Type.displaySm, lineHeight = MoovieFlixTokens.LineHeight.displaySm, fontWeight = FontWeight.ExtraBold),
        displayMd = TextStyle(fontSize = MoovieFlixTokens.Type.displayMd, lineHeight = MoovieFlixTokens.LineHeight.displayMd, fontWeight = FontWeight.ExtraBold),
    )
}

val MaterialTheme.nuvioTypeScale: MoovieFlixTypeScale
    @Composable
    get() = LocalNuvioTypeScale.current
