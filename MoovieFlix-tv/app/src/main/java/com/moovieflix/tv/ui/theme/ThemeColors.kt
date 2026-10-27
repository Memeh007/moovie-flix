package com.moovieflix.tv.ui.theme

import androidx.compose.ui.graphics.Color
import com.moovieflix.tv.domain.model.AppTheme
import com.moovieflix.tv.domain.model.CustomThemeColors

data class ThemeColorPalette(
    val secondary: Color,
    val secondaryVariant: Color,
    val onSecondary: Color = MoovieFlixPrimitives.white,
    val onSecondaryVariant: Color = MoovieFlixPrimitives.white,
    val accentGradient: List<Color> = listOf(secondary),
    val focusRing: Color,
    val focusRingGradient: List<Color> = listOf(focusRing),
    val focusBackground: Color,
    val background: Color = MoovieFlixPrimitives.neutral950,
    val backgroundElevated: Color = MoovieFlixPrimitives.neutral900,
    val backgroundCard: Color = MoovieFlixPrimitives.neutral825,
    val surface: Color = MoovieFlixPrimitives.neutral875,
    val surfaceVariant: Color = MoovieFlixPrimitives.neutral800,
    val panel: Color = MoovieFlixPrimitives.neutral900,
    val overlay: Color = Color(0xD9000000),
    val field: Color = MoovieFlixPrimitives.neutral850,
    val menu: Color = MoovieFlixPrimitives.neutral875,
    val modal: Color = MoovieFlixPrimitives.neutral900,
    val playerOverlay: Color = Color(0xCC000000)
)

object ThemeColors {
    val Crimson = ThemeColorPalette(
        secondary = MoovieFlixPrimitives.red500,
        secondaryVariant = MoovieFlixPrimitives.red600,
        focusRing = MoovieFlixPrimitives.red300,
        focusBackground = Color(0xFF3D1A1A),
        backgroundCard = Color(0xFF241A1A)
    )

    val Ocean = ThemeColorPalette(
        secondary = MoovieFlixPrimitives.blue500,
        secondaryVariant = MoovieFlixPrimitives.blue700,
        focusRing = MoovieFlixPrimitives.blue300,
        focusBackground = Color(0xFF1A2D3D),
        background = Color(0xFF0D0D0F),
        backgroundElevated = Color(0xFF1A1A1E),
        backgroundCard = Color(0xFF1A1F24)
    )

    val Violet = ThemeColorPalette(
        secondary = MoovieFlixPrimitives.violet500,
        secondaryVariant = MoovieFlixPrimitives.violet700,
        focusRing = MoovieFlixPrimitives.violet300,
        focusBackground = Color(0xFF2D1A3D),
        background = Color(0xFF0D0D0F),
        backgroundElevated = Color(0xFF1A1A1E),
        backgroundCard = Color(0xFF1F1A24)
    )

    val Emerald = ThemeColorPalette(
        secondary = MoovieFlixPrimitives.green500,
        secondaryVariant = MoovieFlixPrimitives.green700,
        focusRing = MoovieFlixPrimitives.green300,
        focusBackground = Color(0xFF1A3D1E),
        backgroundCard = Color(0xFF1A241A)
    )

    val Amber = ThemeColorPalette(
        secondary = MoovieFlixPrimitives.amber500,
        secondaryVariant = MoovieFlixPrimitives.amber700,
        focusRing = MoovieFlixPrimitives.amber300,
        focusBackground = Color(0xFF3D2D1A),
        background = Color(0xFF0F0D0D),
        backgroundElevated = Color(0xFF1E1A1A),
        backgroundCard = Color(0xFF24201A)
    )

    val Rose = ThemeColorPalette(
        secondary = MoovieFlixPrimitives.rose500,
        secondaryVariant = MoovieFlixPrimitives.rose700,
        focusRing = MoovieFlixPrimitives.rose300,
        focusBackground = Color(0xFF3D1A2D),
        backgroundCard = Color(0xFF241A1F)
    )

    val White = ThemeColorPalette(
        secondary = MoovieFlixPrimitives.neutral100,
        secondaryVariant = MoovieFlixPrimitives.neutral200,
        onSecondary = MoovieFlixPrimitives.neutral925,
        onSecondaryVariant = MoovieFlixPrimitives.neutral925,
        focusRing = MoovieFlixPrimitives.white,
        focusBackground = Color(0xFF303030),
        backgroundCard = MoovieFlixPrimitives.neutral850
    )

    fun getColorPalette(
        theme: AppTheme,
        customColors: CustomThemeColors = CustomThemeColors.Default
    ): ThemeColorPalette {
        return when (theme) {
            AppTheme.CUSTOM -> customColors.toColorPalette()
            AppTheme.GOLD -> SupporterThemeColors.Gold
            AppTheme.JADE -> SupporterThemeColors.Jade
            AppTheme.ROSE_GOLD -> SupporterThemeColors.RoseGold
            AppTheme.ARCTIC_BLUE -> SupporterThemeColors.ArcticBlue
            AppTheme.GRAPHITE -> SupporterThemeColors.Graphite
            AppTheme.CRIMSON -> Crimson
            AppTheme.OCEAN -> Ocean
            AppTheme.VIOLET -> Violet
            AppTheme.EMERALD -> Emerald
            AppTheme.AMBER -> Amber
            AppTheme.ROSE -> Rose
            AppTheme.WHITE -> White
        }
    }
}
