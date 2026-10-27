package com.moovieflix.tv.core.di

import com.moovieflix.tv.data.repository.AddonRepositoryImpl
import com.moovieflix.tv.data.repository.CatalogRepositoryImpl
import com.moovieflix.tv.data.repository.LibraryRepositoryImpl
import com.moovieflix.tv.data.repository.MetaRepositoryImpl
import com.moovieflix.tv.data.repository.StreamRepositoryImpl
import com.moovieflix.tv.data.repository.SubtitleRepositoryImpl
import com.moovieflix.tv.data.repository.SyncRepositoryImpl
import com.moovieflix.tv.data.repository.WatchProgressRepositoryImpl
import com.moovieflix.tv.domain.repository.AddonRepository
import com.moovieflix.tv.domain.repository.CatalogRepository
import com.moovieflix.tv.domain.repository.LibraryRepository
import com.moovieflix.tv.domain.repository.MetaRepository
import com.moovieflix.tv.domain.repository.StreamRepository
import com.moovieflix.tv.domain.repository.SubtitleRepository
import com.moovieflix.tv.domain.repository.SyncRepository
import com.moovieflix.tv.domain.repository.WatchProgressRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAddonRepository(impl: AddonRepositoryImpl): AddonRepository

    @Binds
    @Singleton
    abstract fun bindCatalogRepository(impl: CatalogRepositoryImpl): CatalogRepository

    @Binds
    @Singleton
    abstract fun bindLibraryRepository(impl: LibraryRepositoryImpl): LibraryRepository

    @Binds
    @Singleton
    abstract fun bindMetaRepository(impl: MetaRepositoryImpl): MetaRepository

    @Binds
    @Singleton
    abstract fun bindStreamRepository(impl: StreamRepositoryImpl): StreamRepository

    @Binds
    @Singleton
    abstract fun bindSubtitleRepository(impl: SubtitleRepositoryImpl): SubtitleRepository

    @Binds
    @Singleton
    abstract fun bindSyncRepository(impl: SyncRepositoryImpl): SyncRepository

    @Binds
    @Singleton
    abstract fun bindWatchProgressRepository(impl: WatchProgressRepositoryImpl): WatchProgressRepository
}
