package com.moovieflix.app

import android.content.Intent
import android.content.res.Configuration
import com.moovieflix.app.features.autosync.AutoSyncPreferencesAndroid
import android.os.Bundle
import androidx.activity.compose.setContent

import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.moovieflix.app.core.auth.AuthStorage
import com.moovieflix.app.core.network.ServerConfigurationStorage
import com.moovieflix.app.core.diagnostics.SentryInitializer
import com.moovieflix.app.core.deeplink.handleAppUrl
import com.moovieflix.app.core.storage.PlatformLocalAccountDataCleaner
import com.moovieflix.app.core.sync.SyncClientIdentityStorage
import com.moovieflix.app.features.addons.AddonHttpClientProvider
import com.moovieflix.app.features.addons.AddonStorage
import com.moovieflix.app.features.collection.CollectionMobileSettingsStorage
import com.moovieflix.app.features.collection.CollectionStorage
import com.moovieflix.app.features.debrid.DebridSettingsStorage
import com.moovieflix.app.features.downloads.DownloadsLiveStatusPlatform
import com.moovieflix.app.features.downloads.DownloadsPlatformDownloader
import com.moovieflix.app.features.downloads.DownloadsStorage
import com.moovieflix.app.features.library.LibraryDisplaySettingsStorage
import com.moovieflix.app.features.membership.MemberAssetStorage
import com.moovieflix.app.features.library.LibraryStorage
import com.moovieflix.app.features.details.MetaScreenSettingsStorage
import com.moovieflix.app.features.home.HomeCatalogSettingsStorage
import com.moovieflix.app.features.mdblist.MdbListSettingsStorage
import com.moovieflix.app.features.notifications.EpisodeReleaseNotificationPlatform
import com.moovieflix.app.features.notifications.EpisodeReleaseNotificationsStorage
import com.moovieflix.app.features.player.PlayerSettingsStorage
import com.moovieflix.app.features.player.PlayerTrackPreferenceStorage
import com.moovieflix.app.features.player.ExternalPlayerPlatform
import com.moovieflix.app.features.player.SubtitleFileCache
import com.moovieflix.app.features.player.PlayerPictureInPictureManager
import com.moovieflix.app.features.player.PipRemoteActionReceiver
import com.moovieflix.app.features.p2p.P2pSettingsStorage
import com.moovieflix.app.features.p2p.P2pStreamingEngine
import com.moovieflix.app.features.plugins.PluginStorage
import com.moovieflix.app.features.profiles.AvatarStorage
import com.moovieflix.app.features.profiles.ProfilePinCacheStorage
import com.moovieflix.app.features.profiles.ProfileStorage
import com.moovieflix.app.features.details.SeasonViewModeStorage
import com.moovieflix.app.features.search.DiscoverSelectionStorage
import com.moovieflix.app.features.search.SearchHistoryStorage
import com.moovieflix.app.features.settings.SentrySettingsStorage
import com.moovieflix.app.features.settings.AppIconPlatform
import com.moovieflix.app.features.settings.ThemeSettingsStorage
import com.moovieflix.app.features.trakt.TraktAuthStorage
import com.moovieflix.app.features.trakt.TraktCommentsStorage
import com.moovieflix.app.features.trakt.TraktLibraryStorage
import com.moovieflix.app.features.trakt.TraktSettingsStorage
import com.moovieflix.app.features.mdblist.PlatformMdbListAuthPersistence
import com.moovieflix.app.features.mdblist.PlatformMdbListSyncStorage
import com.moovieflix.app.features.servers.ServerStorage
import com.moovieflix.app.features.simkl.SimklAuthStorage
import com.moovieflix.app.features.simkl.SimklSyncStorage
import com.moovieflix.app.features.tmdb.TmdbSettingsStorage
import com.moovieflix.app.features.updater.AndroidAppUpdaterPlatform
import com.moovieflix.app.core.ui.CardDepthStyleStorage
import com.moovieflix.app.core.ui.PosterCardStyleStorage
import com.moovieflix.app.core.poster.CustomPosterUrlStorage
import com.moovieflix.app.features.watched.WatchedStorage
import com.moovieflix.app.features.streams.StreamLinkCacheStorage
import com.moovieflix.app.features.streams.StreamBadgeSettingsStorage
import com.moovieflix.app.features.streams.BingeGroupCacheStorage
import com.moovieflix.app.features.watchprogress.ContinueWatchingEnrichmentStorage
import com.moovieflix.app.features.watchprogress.ContinueWatchingPreferencesStorage
import com.moovieflix.app.features.watchprogress.ResumePromptStorage
import com.moovieflix.app.features.watchprogress.WatchProgressStorage

open class MainActivity : AppCompatActivity() {
    private var pipRemoteActionReceiver: PipRemoteActionReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.dark(
                scrim = 0xFF020404.toInt(),
            ),
        )
        ThemeSettingsStorage.initialize(applicationContext)
        AppIconPlatform.initialize(applicationContext)
        SentrySettingsStorage.initialize(applicationContext)
        SentryInitializer.start(application)
        super.onCreate(savedInstanceState)
        AutoSyncPreferencesAndroid.initialize(applicationContext) // AutoSync hook
        window.setBackgroundDrawableResource(R.color.nuvio_background)
        pipRemoteActionReceiver = PipRemoteActionReceiver.register(this)
        SyncClientIdentityStorage.initialize(applicationContext)
        AddonHttpClientProvider.initialize(applicationContext)
        AddonStorage.initialize(applicationContext)
        AuthStorage.initialize(applicationContext)
        ServerConfigurationStorage.initialize(applicationContext)
        LibraryStorage.initialize(applicationContext)
        WatchedStorage.initialize(applicationContext)
        MetaScreenSettingsStorage.initialize(applicationContext)
        com.moovieflix.app.features.shuffle.EpisodeShuffleStorage.initialize(applicationContext)
        HomeCatalogSettingsStorage.initialize(applicationContext)
        PlayerSettingsStorage.initialize(applicationContext)
        PlayerTrackPreferenceStorage.initialize(applicationContext)
        P2pSettingsStorage.initialize(applicationContext)
        P2pStreamingEngine.initialize(applicationContext)
        ExternalPlayerPlatform.initialize(applicationContext)
        SubtitleFileCache.initialize(applicationContext)
        ProfileStorage.initialize(applicationContext)
        AvatarStorage.initialize(applicationContext)
        ProfilePinCacheStorage.initialize(applicationContext)
        MemberAssetStorage.initialize(applicationContext)
        DiscoverSelectionStorage.initialize(applicationContext)
        SearchHistoryStorage.initialize(applicationContext)
        SeasonViewModeStorage.initialize(applicationContext)
        PosterCardStyleStorage.initialize(applicationContext)
        CustomPosterUrlStorage.initialize(applicationContext)
        CardDepthStyleStorage.initialize(applicationContext)
        DebridSettingsStorage.initialize(applicationContext)
        TmdbSettingsStorage.initialize(applicationContext)
        MdbListSettingsStorage.initialize(applicationContext)
        TraktAuthStorage.initialize(applicationContext)
        TraktCommentsStorage.initialize(applicationContext)
        TraktLibraryStorage.initialize(applicationContext)
        TraktSettingsStorage.initialize(applicationContext)
        PlatformMdbListAuthPersistence.initialize(applicationContext)
        ServerStorage.initialize(applicationContext)
        PlatformMdbListSyncStorage.initialize(applicationContext)
        SimklAuthStorage.initialize(applicationContext)
        SimklSyncStorage.initialize(applicationContext)
        LibraryDisplaySettingsStorage.initialize(applicationContext)
        ContinueWatchingPreferencesStorage.initialize(applicationContext)
        ResumePromptStorage.initialize(applicationContext)
        ContinueWatchingEnrichmentStorage.initialize(applicationContext)
        EpisodeReleaseNotificationsStorage.initialize(applicationContext)
        WatchProgressStorage.initialize(applicationContext)
        StreamLinkCacheStorage.initialize(applicationContext)
        StreamBadgeSettingsStorage.initialize(applicationContext)
        BingeGroupCacheStorage.initialize(applicationContext)
        PluginStorage.initialize(applicationContext)
        CollectionMobileSettingsStorage.initialize(applicationContext)
        CollectionStorage.initialize(applicationContext)
        DownloadsStorage.initialize(applicationContext)
        DownloadsPlatformDownloader.initialize(applicationContext)
        DownloadsLiveStatusPlatform.initialize(applicationContext)
        AndroidAppUpdaterPlatform.initialize(applicationContext)
        PlatformLocalAccountDataCleaner.initialize(applicationContext)
        EpisodeReleaseNotificationPlatform.initialize(applicationContext)
        EpisodeReleaseNotificationPlatform.bindActivity(this)
        handleIncomingAppIntent(intent)

        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingAppIntent(intent)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        PlayerPictureInPictureManager.onUserLeaveHint(this)
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        PlayerPictureInPictureManager.onPictureInPictureModeChanged(this, isInPictureInPictureMode)
    }

    override fun onDestroy() {
        EpisodeReleaseNotificationPlatform.unbindActivity(this)
        val receiver = pipRemoteActionReceiver
        if (receiver != null) {
            runCatching { unregisterReceiver(receiver) }
            pipRemoteActionReceiver = null
        }
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        if (EpisodeReleaseNotificationPlatform.handlePermissionRequestResult(requestCode, grantResults)) {
            return
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun handleIncomingAppIntent(intent: Intent?) {
        val appUrl = (intent?.dataString ?: intent?.getStringExtra(LinkActivity.EXTRA_LINK))?.trim().orEmpty()
        if (appUrl.isBlank()) return
        handleAppUrl(appUrl)
    }
}
