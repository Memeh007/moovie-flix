package com.moovieflix.tv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import com.moovieflix.tv.domain.model.AppFont
import com.moovieflix.tv.domain.model.AppTheme
import com.moovieflix.tv.domain.model.CustomThemeColors
import com.moovieflix.tv.domain.model.SettingsUiStyle

data class MoovieFlixExtendedColors(
    val backgroundElevated: Color,
    val backgroundCard: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val focusRing: Color,
    val focusBackground: Color,
    val rating: Color
)

val LocalNuvioColors = staticCompositionLocalOf {
    MoovieFlixColorScheme(ThemeColors.Ocean)
}

val LocalNuvioExtendedColors = staticCompositionLocalOf {
    MoovieFlixExtendedColors(
        backgroundElevated = Color(0xFF1A1A1A),
        backgroundCard = Color(0xFF242424),
        textSecondary = Color(0xFFB3B3B3),
        textTertiary = Color(0xFF808080),
        focusRing = ThemeColors.Ocean.focusRing,
        focusBackground = ThemeColors.Ocean.focusBackground,
        rating = Color(0xFFFFD700)
    )
}

val LocalNuvioTextStyles = staticCompositionLocalOf { MoovieFlixTextStyles }

val LocalAppTheme = staticCompositionLocalOf { AppTheme.WHITE }

val LocalThemePalette = staticCompositionLocalOf { ThemeColors.White }

val LocalSettingsUiStyle = staticCompositionLocalOf { SettingsUiStyle.CLASSIC }

val LocalNuvioFocusRingStyle = staticCompositionLocalOf {
    createFocusRingStyle(ThemeColors.Ocean)
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MoovieFlixTheme(
    appTheme: AppTheme = AppTheme.WHITE,
    appFont: AppFont = AppFont.INTER,
    amoledMode: Boolean = false,
    amoledSurfacesMode: Boolean = false,
    settingsUiStyle: SettingsUiStyle = SettingsUiStyle.CLASSIC,
    customThemeColors: CustomThemeColors = CustomThemeColors.Default,
    content: @Composable () -> Unit
) {
    val palette = androidx.compose.runtime.remember(appTheme, customThemeColors) {
        ThemeColors.getColorPalette(appTheme, customThemeColors)
    }
    val focusRingStyle = createFocusRingStyle(palette)
    val colorScheme = MoovieFlixColorScheme(
        palette = palette,
        amoledMode = amoledMode,
        amoledSurfacesMode = amoledSurfacesMode
    )
    val typography = buildNuvioTypography(getFontFamily(appFont))
    val textStyles = buildNuvioTextStyles(typography)

    val materialColorScheme = darkColorScheme(
        primary = colorScheme.Primary,
        onPrimary = colorScheme.OnPrimary,
        secondary = colorScheme.Secondary,
        onSecondary = colorScheme.OnSecondary,
        background = colorScheme.Background,
        surface = colorScheme.Surface,
        surfaceVariant = colorScheme.SurfaceVariant,
        onBackground = colorScheme.TextPrimary,
        onSurface = colorScheme.TextPrimary,
        onSurfaceVariant = colorScheme.TextSecondary,
        error = colorScheme.Error
    )

    val extendedColors = MoovieFlixExtendedColors(
        backgroundElevated = colorScheme.BackgroundElevated,
        backgroundCard = colorScheme.BackgroundCard,
        textSecondary = colorScheme.TextSecondary,
        textTertiary = colorScheme.TextTertiary,
        focusRing = colorScheme.FocusRing,
        focusBackground = colorScheme.FocusBackground,
        rating = colorScheme.Rating
    )

    CompositionLocalProvider(
        LocalNuvioColors provides colorScheme,
        LocalNuvioExtendedColors provides extendedColors,
        LocalNuvioTextStyles provides textStyles,
        LocalAppTheme provides appTheme,
        LocalThemePalette provides palette,
        LocalSettingsUiStyle provides settingsUiStyle,
        LocalNuvioFocusRingStyle provides focusRingStyle
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = typography,
            content = content
        )
    }
}

object MoovieFlixTheme {
    val palette: ThemeColorPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalThemePalette.current

    val colors: MoovieFlixColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalNuvioColors.current

    val extendedColors: MoovieFlixExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalNuvioExtendedColors.current

    val textStyles: MoovieFlixTextStyleTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalNuvioTextStyles.current

    val spacing: MoovieFlixSpacingTokens
        get() = MoovieFlixSpacing.tokens

    val radii: MoovieFlixRadiusTokens
        get() = MoovieFlixRadii.tokens

    val shapes: MoovieFlixShapeTokens
        get() = MoovieFlixShapes.tokens

    val sizes: MoovieFlixSizeTokens
        get() = MoovieFlixSizes.tokens

    val strokes: MoovieFlixStrokeTokens
        get() = MoovieFlixStrokes.tokens

    val elevations: MoovieFlixElevationTokens
        get() = MoovieFlixElevations.tokens

    val effects: MoovieFlixEffectTokens
        get() = MoovieFlixEffects.tokens

    val motion: MoovieFlixMotionTokens
        get() = MoovieFlixMotion.tokens

    val focus: MoovieFlixFocusTokens
        get() = MoovieFlixFocus.tokens

    val focusRing: MoovieFlixFocusRingStyle
        @Composable
        @ReadOnlyComposable
        get() = LocalNuvioFocusRingStyle.current

    val layout: MoovieFlixLayoutTokens
        get() = MoovieFlixLayout.tokens

    val media: MoovieFlixMediaTokens
        get() = MoovieFlixMedia.tokens

    val components: MoovieFlixComponentTokens
        get() = MoovieFlixComponents.tokens

    val currentTheme: AppTheme
        @Composable
        @ReadOnlyComposable
        get() = LocalAppTheme.current

    val settingsUiStyle: SettingsUiStyle
        @Composable
        @ReadOnlyComposable
        get() = LocalSettingsUiStyle.current
}
