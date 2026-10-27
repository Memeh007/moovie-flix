package com.moovieflix.app

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import com.moovieflix.app.features.catalog.CatalogRepository
import com.moovieflix.app.features.collection.CollectionEditorPage
import com.moovieflix.app.features.collection.CollectionEditorRepository
import com.moovieflix.app.features.collection.FolderDetailRepository
import com.moovieflix.app.features.collection.disposeCollectionEditorPage
import com.moovieflix.app.features.home.MetaPreview
import com.moovieflix.app.features.library.LibraryItem
import com.moovieflix.app.features.player.ExternalPlayerPlaybackRequest
import com.moovieflix.app.features.player.PlayerLaunch
import com.moovieflix.app.features.player.PlayerLaunchStore
import com.moovieflix.app.features.player.externalPlaybackSession
import com.moovieflix.app.features.streams.StreamLaunchStore
import com.moovieflix.app.features.streams.StreamsRepository
import com.moovieflix.app.features.watchprogress.ResumePromptRepository
import com.moovieflix.app.features.watchprogress.WatchProgressRepository
import com.moovieflix.app.navigation.*
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import com.moovieflix.app.core.build.AppFeaturePolicy

internal val navigationSavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(TabsRoute::class, TabsRoute.serializer())
            subclass(DetailRoute::class, DetailRoute.serializer())
            subclass(PersonDetailRoute::class, PersonDetailRoute.serializer())
            subclass(EntityBrowseRoute::class, EntityBrowseRoute.serializer())
            subclass(SettingsPageRoute::class, SettingsPageRoute.serializer())
            subclass(HomescreenSettingsRoute::class, HomescreenSettingsRoute.serializer())
            subclass(MetaScreenSettingsRoute::class, MetaScreenSettingsRoute.serializer())
            subclass(ContinueWatchingSettingsRoute::class, ContinueWatchingSettingsRoute.serializer())
            subclass(DownloadsRoute::class, DownloadsRoute.serializer())
            subclass(MyMoviesRoute::class, MyMoviesRoute.serializer())
            subclass(TvGuideRoute::class, TvGuideRoute.serializer())
            subclass(PartyRoute::class, PartyRoute.serializer())
            subclass(DownloadShowRoute::class, DownloadShowRoute.serializer())
            subclass(AddonsSettingsRoute::class, AddonsSettingsRoute.serializer())
            subclass(PluginsSettingsRoute::class, PluginsSettingsRoute.serializer())
            subclass(AccountSettingsRoute::class, AccountSettingsRoute.serializer())
            subclass(SupportersContributorsSettingsRoute::class, SupportersContributorsSettingsRoute.serializer())
            subclass(LicensesAttributionsSettingsRoute::class, LicensesAttributionsSettingsRoute.serializer())
            subclass(CollectionsRoute::class, CollectionsRoute.serializer())
            subclass(CollectionEditorRoute::class, CollectionEditorRoute.serializer())
            subclass(CollectionEditorPageRoute::class, CollectionEditorPageRoute.serializer())
            subclass(FolderDetailRoute::class, FolderDetailRoute.serializer())
            subclass(StreamRoute::class, StreamRoute.serializer())
            subclass(CatalogRoute::class, CatalogRoute.serializer())
            subclass(PlayerRoute::class, PlayerRoute.serializer())
        }
    }
}

internal fun disposeRouteResources(route: AppRoute) {
    when (route) {
        is StreamRoute -> {
            StreamsRepository.clear()
            StreamLaunchStore.remove(route.launchId)
        }

        is PlayerRoute -> {
            ResumePromptRepository.markPlayerExitedNormally()
            PlayerLaunchStore.remove(route.launchId)
        }

        is CatalogRoute -> {
            CatalogRepository.clear()
            CatalogLaunchStore.remove(route.launchId)
        }

        is CollectionEditorRoute -> CollectionEditorRepository.clear()
        is CollectionEditorPageRoute -> {
            runCatching { CollectionEditorPage.valueOf(route.pageName) }
                .getOrNull()
                ?.let(::disposeCollectionEditorPage)
        }
        is FolderDetailRoute -> FolderDetailRepository.clear()
        else -> Unit
    }
}

internal val externalPlayerSupported = AppFeaturePolicy.externalPlayerSupported

internal data class PosterActionTarget(
    val preview: MetaPreview,
    val libraryItem: LibraryItem? = null,
    val libraryListKey: String? = null,
)

internal fun PlayerLaunch.toExternalPlayerPlaybackRequest(): ExternalPlayerPlaybackRequest =
    ExternalPlayerPlaybackRequest(
        sourceUrl = sourceUrl,
        title = title,
        streamTitle = streamTitle,
        sourceHeaders = sourceHeaders,
        resumePositionMs = initialPositionMs,
        durationMs = WatchProgressRepository.progressForVideo(
            videoId = videoId ?: parentMetaId,
            parentMetaId = parentMetaId,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
        )?.durationMs?.takeIf { it > 0L },
        playbackSession = externalPlaybackSession(),
        season = seasonNumber,
        episode = episodeNumber,
        episodeTitle = episodeTitle,
    )
