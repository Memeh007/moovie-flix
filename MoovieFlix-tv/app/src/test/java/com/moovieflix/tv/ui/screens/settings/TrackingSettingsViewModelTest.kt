package com.moovieflix.tv.ui.screens.settings

import com.moovieflix.tv.MainDispatcherRule
import com.moovieflix.tv.core.profile.ProfileManager
import com.moovieflix.tv.core.tracking.TrackingProviderId
import com.moovieflix.tv.core.tracking.TrackingSourceController
import com.moovieflix.tv.core.tracking.TrackingSourceSelection
import com.moovieflix.tv.data.local.TraktAuthDataStore
import com.moovieflix.tv.data.local.TraktAuthState
import com.moovieflix.tv.data.local.TraktSettingsDataStore
import com.moovieflix.tv.data.local.WatchProgressSource
import com.moovieflix.tv.data.mdblist.MdbListTestHarness
import com.moovieflix.tv.data.simkl.SimklAnimeIdPreference
import com.moovieflix.tv.data.simkl.SimklAuthRepository
import com.moovieflix.tv.data.simkl.SimklAuthState
import com.moovieflix.tv.domain.model.LibrarySourceMode
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TrackingSettingsViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    @Test
    fun `source reconciliation waits for credentials of the selected profile`() = runTest {
        val harness = MdbListTestHarness()
        harness.store.selectProfile(2)
        harness.connected()
        harness.store.selectProfile(1)
        harness.connected()
        val activeProfile = MutableStateFlow(1)
        val profiles = mockk<ProfileManager> { every { activeProfileId } returns activeProfile }
        val reconciliations = mutableListOf<Set<TrackingProviderId>>()
        val controller = mockk<TrackingSourceController> {
            every { watchProgressSource } returns MutableStateFlow(WatchProgressSource.MDBLIST)
            every { librarySourceMode } returns MutableStateFlow(LibrarySourceMode.LOCAL)
            coEvery { reconcileConnectedProviders(any()) } coAnswers {
                reconciliations += firstArg<Set<TrackingProviderId>>()
                TrackingSourceSelection(WatchProgressSource.MDBLIST, LibrarySourceMode.LOCAL)
            }
        }
        val settings = mockk<TraktSettingsDataStore> {
            every { simklAnimeIdPreference } returns MutableStateFlow(SimklAnimeIdPreference.DEFAULT)
        }
        val trakt = mockk<TraktAuthDataStore> { every { state } returns flowOf(TraktAuthState()) }
        val simkl = mockk<SimklAuthRepository> { every { state } returns MutableStateFlow(SimklAuthState()) }
        val viewModel = TrackingSettingsViewModel(controller, settings, mockk(), trakt, simkl, harness.store, profiles)
        runCurrent()
        assertEquals(setOf(TrackingProviderId.MDBLIST), viewModel.uiState.value.connectedProviderIds)
        val previousCount = reconciliations.size
        activeProfile.value = 2
        runCurrent()
        assertFalse(viewModel.uiState.value.isReady)
        assertEquals(previousCount, reconciliations.size)
        harness.store.selectProfile(2)
        runCurrent()
        assertTrue(viewModel.uiState.value.isReady)
        assertEquals(WatchProgressSource.MDBLIST, viewModel.uiState.value.watchProgressSource)
        assertTrue(reconciliations.all { TrackingProviderId.MDBLIST in it })
    }
}
