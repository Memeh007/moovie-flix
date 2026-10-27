package com.moovieflix.tv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

@Immutable
data class MoovieFlixSurfaceColors(
    val background: Color,
    val raised: Color,
    val card: Color,
    val default: Color,
    val variant: Color,
    val panel: Color,
    val overlay: Color,
    val field: Color,
    val menu: Color,
    val modal: Color,
    val playerOverlay: Color,
    val divider: Color
)

@Immutable
data class MoovieFlixTextColors(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val disabled: Color,
    val inverse: Color,
    val onAccent: Color,
    val onOverlay: Color,
    val metadata: Color
)

@Immutable
data class MoovieFlixFocusColors(
    val ring: Color,
    val background: Color,
    val content: Color,
    val accent: Color,
    val scrim: Color
)

@Immutable
data class MoovieFlixSelectionColors(
    val background: Color,
    val foreground: Color,
    val border: Color,
    val mutedBackground: Color,
    val mutedForeground: Color
)

@Immutable
data class MoovieFlixMediaColors(
    val heroScrim: Color,
    val imageScrim: Color,
    val posterFallback: Color,
    val videoControlsScrim: Color,
    val glassPanelTop: Color,
    val glassPanelMiddle: Color,
    val glassPanelBottom: Color,
    val glow: Color
)

@Immutable
data class MoovieFlixStatusColors(
    val rating: Color,
    val error: Color,
    val warning: Color,
    val success: Color,
    val info: Color,
    val watched: Color,
    val unwatched: Color,
    val cached: Color,
    val torrent: Color,
    val premium: Color
)

@Immutable
data class MoovieFlixDisabledColors(
    val container: Color,
    val content: Color,
    val border: Color,
    val overlay: Color
)

@Immutable
data class MoovieFlixSourceColors(
    val trakt: Color,
    val tmdb: Color,
    val imdb: Color,
    val mdblist: Color
)

@Immutable
data class MoovieFlixContrastPair(
    val foreground: Color,
    val background: Color
)

class MoovieFlixColorScheme(
    palette: ThemeColorPalette,
    amoledMode: Boolean = false,
    amoledSurfacesMode: Boolean = false
) {
    private val pureBlack = MoovieFlixPrimitives.black
    private val pureBlackSurfaces = amoledMode && amoledSurfacesMode

    val Background = if (amoledMode) pureBlack else palette.background
    val BackgroundElevated = if (pureBlackSurfaces) pureBlack else palette.backgroundElevated
    val BackgroundCard = if (pureBlackSurfaces) pureBlack else palette.backgroundCard
    val Surface = if (pureBlackSurfaces) pureBlack else palette.surface
    val SurfaceVariant = if (pureBlackSurfaces) pureBlack else palette.surfaceVariant
    val Panel = if (pureBlackSurfaces) pureBlack else palette.panel
    val Overlay = palette.overlay
    val Field = if (pureBlackSurfaces) pureBlack else palette.field
    val Menu = if (pureBlackSurfaces) pureBlack else palette.menu
    val Modal = if (pureBlackSurfaces) pureBlack else palette.modal
    val PlayerOverlay = palette.playerOverlay
    val Divider = MoovieFlixPrimitives.neutral750

    val Primary = MoovieFlixPrimitives.neutral500
    val PrimaryVariant = MoovieFlixPrimitives.neutral650
    val OnPrimary = MoovieFlixPrimitives.white
    val Secondary = palette.secondary
    val SecondaryVariant = palette.secondaryVariant
    val OnSecondary = palette.onSecondary
    val OnSecondaryVariant = palette.onSecondaryVariant

    val TextPrimary = MoovieFlixPrimitives.white
    val TextSecondary = MoovieFlixPrimitives.neutral400
    val TextTertiary = MoovieFlixPrimitives.neutral600
    val TextDisabled = MoovieFlixPrimitives.neutral700
    val TextInverse = MoovieFlixPrimitives.neutral925

    val FocusRing = palette.focusRing
    val FocusBackground = palette.focusBackground
    val FocusContent = MoovieFlixPrimitives.white
    val FocusScrim = MoovieFlixPrimitives.black.copy(alpha = 0.32f)

    val Rating = MoovieFlixPrimitives.rating
    val Error = MoovieFlixPrimitives.error
    val Warning = MoovieFlixPrimitives.warning
    val Success = MoovieFlixPrimitives.success
    val Info = MoovieFlixPrimitives.info
    val Watched = MoovieFlixPrimitives.success
    val Unwatched = MoovieFlixPrimitives.neutral600
    val Cached = MoovieFlixPrimitives.blue300
    val Torrent = MoovieFlixPrimitives.torrent
    val Premium = MoovieFlixPrimitives.premium

    val Border = MoovieFlixPrimitives.neutral750
    val BorderFocused = FocusRing
    val BorderMuted = MoovieFlixPrimitives.neutral750.copy(alpha = 0.58f)

    val Scrim = MoovieFlixPrimitives.black.copy(alpha = 0.62f)
    val ImageScrim = MoovieFlixPrimitives.black.copy(alpha = 0.58f)
    val VideoControlsScrim = MoovieFlixPrimitives.black.copy(alpha = 0.72f)
    val PosterFallback = BackgroundCard

    val DisabledContainer = SurfaceVariant.copy(alpha = 0.42f)
    val DisabledContent = TextDisabled
    val DisabledBorder = Border.copy(alpha = 0.48f)
    val DisabledOverlay = MoovieFlixPrimitives.black.copy(alpha = 0.42f)

    val surfaces = MoovieFlixSurfaceColors(
        background = Background,
        raised = BackgroundElevated,
        card = BackgroundCard,
        default = Surface,
        variant = SurfaceVariant,
        panel = Panel,
        overlay = Overlay,
        field = Field,
        menu = Menu,
        modal = Modal,
        playerOverlay = PlayerOverlay,
        divider = Divider
    )

    val text = MoovieFlixTextColors(
        primary = TextPrimary,
        secondary = TextSecondary,
        tertiary = TextTertiary,
        disabled = TextDisabled,
        inverse = TextInverse,
        onAccent = OnSecondary,
        onOverlay = MoovieFlixPrimitives.white,
        metadata = TextSecondary
    )

    val focus = MoovieFlixFocusColors(
        ring = FocusRing,
        background = FocusBackground,
        content = FocusContent,
        accent = Secondary,
        scrim = FocusScrim
    )

    val selection = MoovieFlixSelectionColors(
        background = Secondary,
        foreground = OnSecondary,
        border = SecondaryVariant,
        mutedBackground = FocusBackground,
        mutedForeground = TextPrimary
    )

    val media = MoovieFlixMediaColors(
        heroScrim = Scrim,
        imageScrim = ImageScrim,
        posterFallback = PosterFallback,
        videoControlsScrim = VideoControlsScrim,
        glassPanelTop = Color(0xD64A4F59),
        glassPanelMiddle = Color(0xCC3F454F),
        glassPanelBottom = Color(0xC640474F),
        glow = FocusRing.copy(alpha = 0.32f)
    )

    val status = MoovieFlixStatusColors(
        rating = Rating,
        error = Error,
        warning = Warning,
        success = Success,
        info = Info,
        watched = Watched,
        unwatched = Unwatched,
        cached = Cached,
        torrent = Torrent,
        premium = Premium
    )

    val disabled = MoovieFlixDisabledColors(
        container = DisabledContainer,
        content = DisabledContent,
        border = DisabledBorder,
        overlay = DisabledOverlay
    )

    val source = MoovieFlixSourceColors(
        trakt = MoovieFlixPrimitives.trakt,
        tmdb = MoovieFlixPrimitives.tmdb,
        imdb = MoovieFlixPrimitives.imdb,
        mdblist = MoovieFlixPrimitives.mdblist
    )

    val contrastPairs = listOf(
        MoovieFlixContrastPair(TextPrimary, Background),
        MoovieFlixContrastPair(TextPrimary, BackgroundCard),
        MoovieFlixContrastPair(TextSecondary, Background),
        MoovieFlixContrastPair(OnSecondary, Secondary),
        MoovieFlixContrastPair(FocusContent, FocusBackground),
        MoovieFlixContrastPair(MoovieFlixPrimitives.white, PlayerOverlay)
    )
}

object MoovieFlixColors {
    val Background: Color
        @Composable
        @ReadOnlyComposable
        get() = MoovieFlixTheme.colors.Background

    val BackgroundElevated: Color
        @Composable
        @ReadOnlyComposable
        get() = MoovieFlixTheme.colors.BackgroundElevated

    val BackgroundCard: Color
        @Composable
        @ReadOnlyComposable
        get() = MoovieFlixTheme.colors.BackgroundCard

    val Surface: Color
        @Composable
        @ReadOnlyComposable
        get() = MoovieFlixTheme.colors.Surface

    val SurfaceVariant: Color
        @Composable
        @ReadOnlyComposable
        get() = MoovieFlixTheme.colors.SurfaceVariant

    val Primary = MoovieFlixPrimitives.neutral500
    val PrimaryVariant = MoovieFlixPrimitives.neutral650
    val OnPrimary = MoovieFlixPrimitives.white
    val TextPrimary = MoovieFlixPrimitives.white
    val TextSecondary = MoovieFlixPrimitives.neutral400
    val TextTertiary = MoovieFlixPrimitives.neutral600
    val TextDisabled = MoovieFlixPrimitives.neutral700
    val Rating = MoovieFlixPrimitives.rating
    val Error = MoovieFlixPrimitives.error
    val Success = MoovieFlixPrimitives.success
    val Warning = MoovieFlixPrimitives.warning
    val Info = MoovieFlixPrimitives.info
    val Border = MoovieFlixPrimitives.neutral750

    val Secondary: Color
        @Composable
        @ReadOnlyComposable
        get() = MoovieFlixTheme.colors.Secondary

    val SecondaryVariant: Color
        @Composable
        @ReadOnlyComposable
        get() = MoovieFlixTheme.colors.SecondaryVariant

    val OnSecondary: Color
        @Composable
        @ReadOnlyComposable
        get() = MoovieFlixTheme.colors.OnSecondary

    val OnSecondaryVariant: Color
        @Composable
        @ReadOnlyComposable
        get() = MoovieFlixTheme.colors.OnSecondaryVariant

    val FocusRing: Color
        @Composable
        @ReadOnlyComposable
        get() = MoovieFlixTheme.colors.FocusRing

    val FocusBackground: Color
        @Composable
        @ReadOnlyComposable
        get() = MoovieFlixTheme.colors.FocusBackground

    val BorderFocused: Color
        @Composable
        @ReadOnlyComposable
        get() = MoovieFlixTheme.colors.BorderFocused
}
