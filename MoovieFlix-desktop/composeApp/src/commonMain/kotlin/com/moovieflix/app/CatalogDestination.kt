package com.moovieflix.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.moovieflix.app.features.catalog.CatalogScreen
import com.moovieflix.app.features.catalog.CatalogTarget
import com.moovieflix.app.features.home.MetaPreview
import com.moovieflix.app.features.library.toLibraryItem
import com.moovieflix.app.features.servers.ServerCatalog
import com.moovieflix.app.navigation.CatalogRoute
import com.moovieflix.app.navigation.DetailRoute
import com.moovieflix.app.navigation.MoovieFlixNavigator

internal data class CatalogLaunch(
    val title: String,
    val subtitle: String,
    val target: CatalogTarget,
)

internal object CatalogLaunchStore {
    private var nextLaunchId = 1L
    private val launches = mutableMapOf<Long, CatalogLaunch>()

    fun put(launch: CatalogLaunch): Long {
        val launchId = nextLaunchId++
        launches[launchId] = launch
        return launchId
    }

    fun get(launchId: Long): CatalogLaunch? = launches[launchId]

    fun remove(launchId: Long) {
        launches.remove(launchId)
    }
}

internal fun MoovieFlixNavigator.openPreview(meta: MetaPreview) {
    val collection = ServerCatalog.collectionTarget(meta)
    if (collection == null) {
        navigate(DetailRoute(type = meta.type, id = meta.id, title = meta.name))
        return
    }
    val subtitle = ServerCatalog.sourceLabel(collection)
    val launchId = CatalogLaunchStore.put(CatalogLaunch(title = meta.name, subtitle = subtitle, target = collection))
    navigate(CatalogRoute(launchId = launchId, title = meta.name, subtitle = subtitle))
}

@Composable
internal fun CatalogDestination(
    route: CatalogRoute,
    navController: MoovieFlixNavigator,
    onPosterLongClick: (PosterActionTarget) -> Unit,
) {
    val onBack = rememberGuardedPopBackStack(navController, route)
    val launch = remember(route.launchId) { CatalogLaunchStore.get(route.launchId) }
    if (launch == null) {
        LaunchedEffect(route.launchId) { onBack() }
        return
    }

    val target = launch.target
    CatalogScreen(
        title = launch.title,
        subtitle = launch.subtitle,
        target = target,
        onBack = onBack,
        onPosterClick = navController::openPreview,
        onPosterLongClick = { meta: MetaPreview ->
            onPosterLongClick(
                if (target is CatalogTarget.Library) {
                    PosterActionTarget(
                        preview = meta,
                        libraryItem = meta.toLibraryItem(savedAtEpochMs = 0L),
                        libraryListKey = target.sectionType,
                    )
                } else {
                    PosterActionTarget(preview = meta)
                },
            )
        },
        modifier = Modifier.fillMaxSize(),
    )
}
