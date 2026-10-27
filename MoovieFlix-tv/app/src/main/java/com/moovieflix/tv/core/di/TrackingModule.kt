package com.moovieflix.tv.core.di

import com.moovieflix.tv.data.simkl.AndroidSimklAuthStorage
import com.moovieflix.tv.data.simkl.AndroidSimklSyncStorage
import com.moovieflix.tv.data.simkl.SimklAuthStorage
import com.moovieflix.tv.data.simkl.SimklApiSyncRemote
import com.moovieflix.tv.data.simkl.SimklSyncRemote
import com.moovieflix.tv.data.simkl.SimklSyncStorage
import com.moovieflix.tv.core.tracking.TrackingLibraryProvider
import com.moovieflix.tv.core.tracking.TrackingProvider
import com.moovieflix.tv.data.repository.TraktTrackingLibraryProvider
import com.moovieflix.tv.data.repository.TraktTrackingProvider
import com.moovieflix.tv.data.simkl.SimklLibraryService
import com.moovieflix.tv.core.tracking.TrackingHistoryWriter
import com.moovieflix.tv.core.tracking.TrackingProgressProvider
import com.moovieflix.tv.data.repository.TraktTrackingHistoryWriter
import com.moovieflix.tv.data.repository.TraktTrackingProgressProvider
import com.moovieflix.tv.data.simkl.SimklTrackingHistoryWriter
import com.moovieflix.tv.data.simkl.SimklTrackingProgressProvider
import com.moovieflix.tv.data.simkl.SimklTrackingProvider
import com.moovieflix.tv.core.profile.ProfileScopedCredentialStore
import com.moovieflix.tv.data.mdblist.MdbListTrackingHistoryWriter
import com.moovieflix.tv.data.mdblist.MdbListTrackingProgressProvider
import com.moovieflix.tv.data.mdblist.MdbListTrackingProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TrackingModule {
    @Binds
    @IntoSet
    abstract fun bindMdbListLibrary(provider: com.moovieflix.tv.data.mdblist.MdbListTrackingLibraryProvider): TrackingLibraryProvider

    @Binds
    @IntoSet
    abstract fun bindMdbListProvider(provider: MdbListTrackingProvider): TrackingProvider

    @Binds
    @IntoSet
    abstract fun bindMdbListProgress(provider: MdbListTrackingProgressProvider): TrackingProgressProvider

    @Binds
    @IntoSet
    abstract fun bindMdbListHistory(writer: MdbListTrackingHistoryWriter): TrackingHistoryWriter

    @Binds
    @Singleton
    abstract fun bindSimklAuthStorage(storage: AndroidSimklAuthStorage): SimklAuthStorage

    @Binds
    @IntoSet
    abstract fun bindSimklProfileScopedCredentialStore(
        storage: AndroidSimklAuthStorage
    ): ProfileScopedCredentialStore

    @Binds
    @Singleton
    abstract fun bindSimklSyncStorage(storage: AndroidSimklSyncStorage): SimklSyncStorage

    @Binds
    @Singleton
    abstract fun bindSimklSyncRemote(remote: SimklApiSyncRemote): SimklSyncRemote

    @Binds
    @IntoSet
    abstract fun bindTraktTrackingLibraryProvider(
        provider: TraktTrackingLibraryProvider
    ): TrackingLibraryProvider

    @Binds
    @IntoSet
    abstract fun bindSimklTrackingLibraryProvider(
        provider: SimklLibraryService
    ): TrackingLibraryProvider

    @Binds
    @IntoSet
    abstract fun bindTraktTrackingProgressProvider(
        provider: TraktTrackingProgressProvider
    ): TrackingProgressProvider

    @Binds
    @IntoSet
    abstract fun bindSimklTrackingProgressProvider(
        provider: SimklTrackingProgressProvider
    ): TrackingProgressProvider

    @Binds
    @IntoSet
    abstract fun bindTraktTrackingHistoryWriter(
        writer: TraktTrackingHistoryWriter
    ): TrackingHistoryWriter

    @Binds
    @IntoSet
    abstract fun bindSimklTrackingHistoryWriter(
        writer: SimklTrackingHistoryWriter
    ): TrackingHistoryWriter

    @Binds
    @IntoSet
    abstract fun bindTraktTrackingProvider(
        provider: TraktTrackingProvider
    ): TrackingProvider

    @Binds
    @IntoSet
    abstract fun bindSimklTrackingProvider(
        provider: SimklTrackingProvider
    ): TrackingProvider
}
