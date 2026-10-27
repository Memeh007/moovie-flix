package com.moovieflix.app.core.tracking

import com.moovieflix.app.features.simkl.SimklAuthRepository
import com.moovieflix.app.features.simkl.SimklMutationRepository
import com.moovieflix.app.features.simkl.SimklLibraryRepository
import com.moovieflix.app.features.simkl.SimklProgressRepository
import com.moovieflix.app.features.simkl.SimklTrackingLibraryProvider
import com.moovieflix.app.features.simkl.SimklTrackingProgressProvider
import com.moovieflix.app.features.simkl.SimklWatchedSyncAdapter
import com.moovieflix.app.features.simkl.SimklSyncRepository
import com.moovieflix.app.features.tracking.TrackingProviderRegistry
import com.moovieflix.app.features.mdblist.MdbListTracker
import com.moovieflix.app.features.trakt.TraktAuthRepository
import com.moovieflix.app.features.trakt.TraktScrobbleRepository
import com.moovieflix.app.features.trakt.TraktTrackingLibraryProvider
import com.moovieflix.app.features.trakt.TraktTrackingProgressProvider
import com.moovieflix.app.features.watching.sync.TraktWatchedSyncAdapter

fun ensureTrackingProvidersRegistered() {
    MdbListTracker.register()
    TraktAuthRepository.descriptor
    TraktScrobbleRepository.ensureRegistered()
    SimklAuthRepository.descriptor
    SimklSyncRepository.state
    SimklLibraryRepository.uiState
    SimklProgressRepository.uiState
    SimklMutationRepository.ensureRegistered()
    TrackingProviderRegistry.registerLibraryProvider(TraktTrackingLibraryProvider)
    TrackingProviderRegistry.registerLibraryProvider(SimklTrackingLibraryProvider)
    TrackingProviderRegistry.registerWatchedProvider(TraktWatchedSyncAdapter)
    TrackingProviderRegistry.registerWatchedProvider(SimklWatchedSyncAdapter)
    TrackingProviderRegistry.registerProgressProvider(TraktTrackingProgressProvider)
    TrackingProviderRegistry.registerProgressProvider(SimklTrackingProgressProvider)
}
