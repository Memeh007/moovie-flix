package com.moovieflix.app.core.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.Typography
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import com.moovieflix.app.isDesktop
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.jetbrains_sans_bold
import nuvio.composeapp.generated.resources.jetbrains_sans_regular
import nuvio.composeapp.generated.resources.jetbrains_sans_semibold
import org.jetbrains.compose.resources.Font

val LocalAppTheme = staticCompositionLocalOf { AppTheme.WHITE }
val LocalThemePalette = staticCompositionLocalOf { ThemeColors.White }

val MaterialTheme.themePalette: ThemeColorPalette
    @Composable
    @ReadOnlyComposable
    get() = LocalThemePalette.current

internal val LocalNuvioPlatformDensity = staticCompositionLocalOf<Density> {
    error("Platform density is unavailable outside MoovieFlixTheme")
}

val MaterialTheme.appTheme: AppTheme
    @Composable
    @ReadOnlyComposable
    get() = LocalAppTheme.current

private fun contentColorFor(background: Color): Color =
    if (background.luminance() > 0.5f) Color(0xFF111111) else Color(0xFFF5F7F8)

private fun buildColorScheme(palette: ThemeColorPalette, amoled: Boolean = false) = darkColorScheme(
    primary = palette.secondary,
    onPrimary = palette.onSecondary,
    primaryContainer = palette.focusBackground,
    onPrimaryContainer = contentColorFor(palette.focusBackground),
    secondary = palette.secondaryVariant,
    onSecondary = palette.onSecondaryVariant,
    background = if (amoled) Color.Black else palette.background,
    onBackground = Color(0xFFF5F7F8),
    surface = palette.backgroundElevated,
    onSurface = Color(0xFFF5F7F8),
    surfaceVariant = palette.backgroundCard,
    onSurfaceVariant = Color(0xFF969CA3),
    outline = Color(0xFF252A2A),
    error = Color(0xFFE36A8A),
    onError = Color(0xFFFCE5EC),
)

private val JetBrainsSans: FontFamily
    @Composable
    get() = FontFamily(
        Font(Res.font.jetbrains_sans_bold, FontWeight.Bold, FontStyle.Normal),
        Font(Res.font.jetbrains_sans_semibold, FontWeight.SemiBold, FontStyle.Normal),
        Font(Res.font.jetbrains_sans_regular, FontWeight.Normal, FontStyle.Normal),
    )

private val MoovieFlixTypography: Typography
    @Composable
    get() = Typography(
        displayLarge = TextStyle(
            fontFamily = JetBrainsSans,
            fontSize = MoovieFlixTokens.Type.pageDisplay,
            lineHeight = MoovieFlixTokens.LineHeight.pageDisplay,
            fontWeight = FontWeight.Bold,
            letterSpacing = MoovieFlixTokens.LetterSpacing.pageDisplay,
        ),
        headlineLarge = TextStyle(
            fontFamily = JetBrainsSans,
            fontSize = MoovieFlixTokens.Type.headline,
            lineHeight = MoovieFlixTokens.LineHeight.headline,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = MoovieFlixTokens.LetterSpacing.headline,
        ),
        titleLarge = TextStyle(
            fontFamily = JetBrainsSans,
            fontSize = MoovieFlixTokens.Type.titleSm,
            lineHeight = MoovieFlixTokens.LineHeight.materialTitleLarge,
            fontWeight = FontWeight.SemiBold,
        ),
        titleMedium = TextStyle(
            fontFamily = JetBrainsSans,
            fontSize = MoovieFlixTokens.Type.bodyLg,
            lineHeight = MoovieFlixTokens.LineHeight.bodyMd,
            fontWeight = FontWeight.SemiBold,
        ),
        bodyLarge = TextStyle(
            fontFamily = JetBrainsSans,
            fontSize = MoovieFlixTokens.Type.bodyApp,
            lineHeight = MoovieFlixTokens.LineHeight.bodyApp,
            fontWeight = FontWeight.Normal,
        ),
        bodyMedium = TextStyle(
            fontFamily = JetBrainsSans,
            fontSize = MoovieFlixTokens.Type.bodyMd,
            lineHeight = MoovieFlixTokens.LineHeight.bodyMd,
            fontWeight = FontWeight.Normal,
        ),
        labelLarge = TextStyle(
            fontFamily = JetBrainsSans,
            fontSize = MoovieFlixTokens.Type.bodyMd,
            lineHeight = MoovieFlixTokens.LineHeight.bodySm,
            fontWeight = FontWeight.SemiBold,
        ),
        labelMedium = TextStyle(
            fontFamily = JetBrainsSans,
            fontSize = MoovieFlixTokens.Type.labelSm,
            lineHeight = MoovieFlixTokens.LineHeight.labelXs,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = MoovieFlixTokens.LetterSpacing.label,
        ),
    )

private val MoovieFlixTypeTokens: MoovieFlixTypeScale
    @Composable
    get() = MoovieFlixTypeScale(
        labelXs = TextStyle(
            fontFamily = JetBrainsSans,
            fontSize = MoovieFlixTokens.Type.labelXs,
            lineHeight = MoovieFlixTokens.LineHeight.labelXs,
            fontWeight = FontWeight.SemiBold,
        ),
        labelSm = TextStyle(
            fontFamily = JetBrainsSans,
            fontSize = MoovieFlixTokens.Type.labelSm,
            lineHeight = MoovieFlixTokens.LineHeight.labelSm,
            fontWeight = FontWeight.SemiBold,
        ),
        bodySm = TextStyle(
            fontFamily = JetBrainsSans,
            fontSize = MoovieFlixTokens.Type.bodySm,
            lineHeight = MoovieFlixTokens.LineHeight.bodySm,
            fontWeight = FontWeight.Normal,
        ),
        bodyMd = TextStyle(
            fontFamily = JetBrainsSans,
            fontSize = MoovieFlixTokens.Type.bodyMd,
            lineHeight = MoovieFlixTokens.LineHeight.bodyMd,
            fontWeight = FontWeight.Normal,
        ),
        bodyLg = TextStyle(
            fontFamily = JetBrainsSans,
            fontSize = MoovieFlixTokens.Type.bodyLg,
            lineHeight = MoovieFlixTokens.LineHeight.bodyLg,
            fontWeight = FontWeight.Normal,
        ),
        titleSm = TextStyle(
            fontFamily = JetBrainsSans,
            fontSize = MoovieFlixTokens.Type.titleSm,
            lineHeight = MoovieFlixTokens.LineHeight.titleSm,
            fontWeight = FontWeight.SemiBold,
        ),
        titleMd = TextStyle(
            fontFamily = JetBrainsSans,
            fontSize = MoovieFlixTokens.Type.titleMd,
            lineHeight = MoovieFlixTokens.LineHeight.titleMd,
            fontWeight = FontWeight.SemiBold,
        ),
        titleLg = TextStyle(
            fontFamily = JetBrainsSans,
            fontSize = MoovieFlixTokens.Type.titleLg,
            lineHeight = MoovieFlixTokens.LineHeight.titleLg,
            fontWeight = FontWeight.SemiBold,
        ),
        displaySm = TextStyle(
            fontFamily = JetBrainsSans,
            fontSize = MoovieFlixTokens.Type.displaySm,
            lineHeight = MoovieFlixTokens.LineHeight.displaySm,
            fontWeight = FontWeight.Bold,
        ),
        displayMd = TextStyle(
            fontFamily = JetBrainsSans,
            fontSize = MoovieFlixTokens.Type.displayMd,
            lineHeight = MoovieFlixTokens.LineHeight.displayMd,
            fontWeight = FontWeight.Bold,
        ),
    )

private val MoovieFlixRippleConfiguration = RippleConfiguration(
    color = Color.Black,
)

private const val MoovieFlixDesktopFontScale = 1.08f
private const val MoovieFlixDesktopBaseWidthDp = 1280f
private const val MoovieFlixDesktopBaseHeightDp = 820f
private const val MoovieFlixDesktopMinUiScale = 1f
private const val MoovieFlixDesktopMaxUiScale = 1.18f

internal fun desktopUiScaleForWindow(widthDp: Float, heightDp: Float): Float {
    if (!isDesktop || widthDp <= 0f || heightDp <= 0f) return MoovieFlixDesktopMinUiScale

    val rawScale = minOf(
        widthDp / MoovieFlixDesktopBaseWidthDp,
        heightDp / MoovieFlixDesktopBaseHeightDp,
    )
    return rawScale.coerceIn(MoovieFlixDesktopMinUiScale, MoovieFlixDesktopMaxUiScale)
}

@Composable
fun MoovieFlixTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    appTheme: AppTheme = AppTheme.WHITE,
    amoled: Boolean = false,
    desktopUiScale: Float = MoovieFlixDesktopMinUiScale,
    customThemeColors: CustomThemeColors = CustomThemeColors.Default,
    content: @Composable () -> Unit,
) {
    val palette = remember(appTheme, customThemeColors) {
        ThemeColors.getColorPalette(appTheme, customThemeColors)
    }
    val colorScheme = buildColorScheme(palette, amoled = amoled)
    val tokens = defaultNuvioThemeTokens(palette, amoled = amoled, colorScheme = colorScheme)

    val density = LocalDensity.current
    val effectiveDesktopUiScale = if (isDesktop) {
        desktopUiScale.coerceIn(MoovieFlixDesktopMinUiScale, MoovieFlixDesktopMaxUiScale)
    } else {
        MoovieFlixDesktopMinUiScale
    }
    CompositionLocalProvider(
        LocalNuvioPlatformDensity provides density,
        LocalDensity provides Density(
            density = density.density * effectiveDesktopUiScale,
            fontScale = if (isDesktop) MoovieFlixDesktopFontScale else 1f,
        ),
        LocalNuvioThemeTokens provides tokens,
        LocalNuvioTypeScale provides MoovieFlixTypeTokens,
        LocalRippleConfiguration provides MoovieFlixRippleConfiguration,
        LocalAppTheme provides appTheme,
        LocalThemePalette provides palette,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MoovieFlixTypography,
        ) {
            SkeletonAnimationProvider(content = content)
        }
    }
}
