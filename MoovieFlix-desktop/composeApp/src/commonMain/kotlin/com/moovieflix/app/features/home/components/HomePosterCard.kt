package com.moovieflix.app.features.home.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.moovieflix.app.core.format.formatReleaseDateForDisplay
import com.moovieflix.app.core.ui.MoovieFlixPosterCard
import com.moovieflix.app.core.ui.MoovieFlixPosterShape
import com.moovieflix.app.core.ui.desktopCatalogShelfPosterBaseWidthDp
import com.moovieflix.app.core.ui.rememberPosterCardStyleUiState
import com.moovieflix.app.features.home.MetaPreview
import com.moovieflix.app.features.home.PosterShape

@Composable
fun HomePosterCard(
    item: MetaPreview,
    modifier: Modifier = Modifier,
    useLandscapeBackdropMode: Boolean = false,
    isWatched: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    showLandscapeOverlay: Boolean = true,
) {
    val posterCardStyle = rememberPosterCardStyleUiState()
    val isLandscapeMode = useLandscapeBackdropMode || posterCardStyle.catalogLandscapeModeEnabled
    val effectiveLandscapePoster = if (posterCardStyle.alwaysShowLandscapeClearlogo) null else item.landscapePoster
    val imageUrl = if (isLandscapeMode) {
        effectiveLandscapePoster ?: item.banner ?: item.poster
    } else item.poster
    val fallbackImageUrl = if (isLandscapeMode && !effectiveLandscapePoster.isNullOrBlank()) {
        // Landscape custom poster -> fall back to original backdrop, then portrait
        item.banner ?: item.rawPosterUrl
    } else {
        item.rawPosterUrl
    }

    HomePosterHoverPreview(
        item = item,
        isWatched = isWatched,
        onClick = onClick,
        onLongClick = onLongClick,
    ) { hoverModifier ->
        MoovieFlixPosterCard(
            title = item.name,
            imageUrl = imageUrl,
            modifier = modifier.then(hoverModifier),
            fallbackImageUrl = fallbackImageUrl,
            basePosterWidthDp = desktopCatalogShelfPosterBaseWidthDp(posterCardStyle.widthDp),
            shape = if (isLandscapeMode) MoovieFlixPosterShape.Landscape else item.posterShape.toNuvioPosterShape(),
            detailLine = if (isLandscapeMode || posterCardStyle.hideLabelsEnabled) null else item.releaseInfo?.let { formatReleaseDateForDisplay(it) },
            showTitleBelow = !posterCardStyle.hideLabelsEnabled,
            bottomLeftLogoUrl = if (isLandscapeMode && showLandscapeOverlay && effectiveLandscapePoster.isNullOrBlank()) item.logo else null,
            bottomLeftText = if (isLandscapeMode && showLandscapeOverlay && effectiveLandscapePoster.isNullOrBlank() && item.logo.isNullOrBlank() && !posterCardStyle.hideLabelsEnabled) item.name else null,
            isWatched = isWatched,
            onClick = onClick,
            onLongClick = onLongClick,
        )
    }
}

private fun PosterShape.toNuvioPosterShape(): MoovieFlixPosterShape =
    when (this) {
        PosterShape.Poster -> MoovieFlixPosterShape.Poster
        PosterShape.Square -> MoovieFlixPosterShape.Square
        PosterShape.Landscape -> MoovieFlixPosterShape.Landscape
    }
