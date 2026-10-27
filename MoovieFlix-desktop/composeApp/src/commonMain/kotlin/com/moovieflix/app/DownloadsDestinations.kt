package com.moovieflix.app

import androidx.compose.runtime.Composable
import com.moovieflix.app.features.downloads.DownloadItem
import com.moovieflix.app.features.downloads.DownloadsScreen
import com.moovieflix.app.navigation.DownloadShowRoute
import com.moovieflix.app.navigation.DownloadsRoute
import com.moovieflix.app.navigation.MoovieFlixNavigator

@Composable
internal fun DownloadsDestination(
    route: DownloadsRoute,
    navController: MoovieFlixNavigator,
    useNativeNavigation: Boolean,
    onOpenDownload: (DownloadItem) -> Unit,
) {
    val onBack = rememberGuardedPopBackStack(navController, route)
    DownloadsScreen(
        onBack = onBack,
        onOpenDownload = onOpenDownload,
        onNavigateToShow = if (useNativeNavigation) {
            { showId, title -> navController.navigate(DownloadShowRoute(showId, title)) }
        } else {
            null
        },
    )
}

@Composable
internal fun DownloadShowDestination(
    route: DownloadShowRoute,
    navController: MoovieFlixNavigator,
    onOpenDownload: (DownloadItem) -> Unit,
) {
    val onBack = rememberGuardedPopBackStack(navController, route)
    DownloadsScreen(
        onBack = onBack,
        onOpenDownload = onOpenDownload,
        initialShowId = route.showId,
        onBackFromShow = onBack,
    )
}
