package com.moovieflix.tv.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moovieflix.tv.data.local.AmbientModeDataStore
import com.moovieflix.tv.domain.model.AmbientModeSettings
import com.moovieflix.tv.domain.model.enabledAddons
import com.moovieflix.tv.domain.repository.AddonRepository
import com.moovieflix.tv.ui.screens.ambient.AmbientCatalog
import com.moovieflix.tv.ui.screens.ambient.ambientCatalogs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AmbientModeSettingsUiState(
    val settings: AmbientModeSettings = AmbientModeSettings(),
    val catalogs: List<AmbientCatalog> = emptyList()
)

@HiltViewModel
class AmbientModeSettingsViewModel @Inject constructor(
    private val dataStore: AmbientModeDataStore,
    addonRepository: AddonRepository
) : ViewModel() {

    val uiState: StateFlow<AmbientModeSettingsUiState> = combine(
        dataStore.settings,
        addonRepository.getInstalledAddons().map { it.enabledAddons().ambientCatalogs() }
    ) { settings, catalogs ->
        AmbientModeSettingsUiState(settings = settings, catalogs = catalogs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AmbientModeSettingsUiState())

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch { dataStore.setEnabled(enabled) }
    }

    fun setTimeoutMinutes(minutes: Int) {
        viewModelScope.launch { dataStore.setTimeoutMinutes(minutes) }
    }

    fun setCatalogKeys(keys: List<String>) {
        viewModelScope.launch { dataStore.setCatalogKeys(keys) }
    }
}
