package com.moovieflix.app.core.storage

import com.moovieflix.app.core.build.AppFeaturePolicy
import com.moovieflix.app.core.sync.SyncManager
import com.moovieflix.app.core.sync.ProfileSettingsSync
import com.moovieflix.app.core.tracking.ensureTrackingProvidersRegistered
import com.moovieflix.app.features.addons.AddonRepository
import com.moovieflix.app.features.catalog.CatalogRepository
import com.moovieflix.app.features.collection.CollectionMobileSettingsRepository
import com.moovieflix.app.features.collection.CollectionRepository
import com.moovieflix.app.features.details.MetaDetailsRepository
import com.moovieflix.app.features.details.MetaScreenSettingsRepository
import com.moovieflix.app.features.home.HomeCatalogSettingsRepository
import com.moovieflix.app.features.home.HomeRepository
import com.moovieflix.app.features.library.LibraryRepository
import com.moovieflix.app.features.membership.MemberAccessRepository
import com.moovieflix.app.features.library.LibraryDisplaySettingsRepository
import com.moovieflix.app.features.notifications.EpisodeReleaseNotificationsRepository
import com.moovieflix.app.features.player.PlayerLaunchStore
import com.moovieflix.app.features.player.PlayerSettingsRepository
import com.moovieflix.app.features.p2p.P2pSettingsRepository
import com.moovieflix.app.features.plugins.PluginRepository
import com.moovieflix.app.features.player.SubtitleRepository
import com.moovieflix.app.features.profiles.ProfileRepository
import com.moovieflix.app.features.profiles.MAX_PROFILES
import com.moovieflix.app.features.search.SearchRepository
import com.moovieflix.app.features.servers.ServerRepository
import com.moovieflix.app.features.settings.ThemeSettingsRepository
import com.moovieflix.app.features.streams.StreamContextStore
import com.moovieflix.app.features.streams.StreamBadgeSettingsRepository
import com.moovieflix.app.features.streams.StreamLaunchStore
import com.moovieflix.app.features.streams.StreamsRepository
import com.moovieflix.app.features.tracking.TrackingProviderRegistry
import com.moovieflix.app.features.tracking.TrackingSettingsRepository
import com.moovieflix.app.core.ui.CardDepthStyleRepository
import com.moovieflix.app.core.ui.PosterCardStyleRepository
import com.moovieflix.app.features.watchprogress.ContinueWatchingPreferencesRepository
import com.moovieflix.app.features.watchprogress.ContinueWatchingEnrichmentCache
import com.moovieflix.app.features.watchprogress.WatchProgressRepository
import com.moovieflix.app.features.watchprogress.WatchProgressSourceCoordinator
import com.moovieflix.app.features.watched.WatchedRepository

internal object LocalAccountDataCleaner {
    fun wipe() {
        ensureTrackingProvidersRegistered()
        TrackingProviderRegistry.removeStoredProfiles(1..MAX_PROFILES)
        SyncManager.cancelAccountSync()
        WatchProgressSourceCoordinator.clearLocalState()
        ProfileSettingsSync.clearAccountState()
        ContinueWatchingEnrichmentCache.clearLocalState()
        WatchProgressRepository.clearLocalState()
        WatchedRepository.clearLocalState()
        LibraryRepository.runAccountStorageWipe {
            wipePlatformStorage()
        }

        ProfileRepository.clearInMemory()
        MemberAccessRepository.clearLocalState()
        AddonRepository.clearLocalState()
        ServerRepository.clearLocalState()
        if (AppFeaturePolicy.pluginsEnabled) {
            PluginRepository.clearLocalState()
        }
        HomeRepository.clear()
        HomeCatalogSettingsRepository.clearLocalState()
        MetaScreenSettingsRepository.clearLocalState()
        com.moovieflix.app.features.shuffle.EpisodeShuffleRepository.clearLocalState()
        LibraryRepository.clearLocalState()
        LibraryDisplaySettingsRepository.clearLocalState()
        ContinueWatchingPreferencesRepository.clearLocalState()
        EpisodeReleaseNotificationsRepository.clearLocalState()
        CollectionMobileSettingsRepository.clearLocalState()
        CollectionRepository.clearLocalState()
        ThemeSettingsRepository.clearLocalState()
        PosterCardStyleRepository.clearLocalState()
        CardDepthStyleRepository.clearLocalState()
        TrackingProviderRegistry.clearLocalState()
        TrackingSettingsRepository.clearLocalState()
        PlayerSettingsRepository.clearLocalState()
        StreamBadgeSettingsRepository.clearLocalState()
        P2pSettingsRepository.clearLocalState()
        CatalogRepository.clear()
        StreamsRepository.clear()
        MetaDetailsRepository.clear()
        SearchRepository.reset()
        SubtitleRepository.clear()
        PlayerLaunchStore.clear()
        StreamLaunchStore.clear()
        StreamContextStore.clear()
    }

    internal fun wipePlatformStorage(wipeStorage: () -> Unit = PlatformLocalAccountDataCleaner::wipe) {
        try {
            wipeStorage()
        } finally {
            ContinueWatchingEnrichmentCache.clearLocalState()
        }
    }
}

internal expect object PlatformLocalAccountDataCleaner {
    fun wipe()
}
