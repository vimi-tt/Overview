package io.github.vimitt.overview.ui.settings.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.vimitt.overview.data.SettingsRepository
import io.github.vimitt.overview.domain.model.HomeClockStyle
import io.github.vimitt.overview.domain.model.HomeSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class HomeSettingsUiState(
    val settings: HomeSettings = HomeSettings(),
)

/**
 * ViewModel da subtela Tela inicial.
 */
class HomeSettingsViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeSettingsUiState> = settingsRepository.homeSettings
        .map { HomeSettingsUiState(settings = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeSettingsUiState(),
        )

    fun onGridColumnsChange(columns: Int) {
        viewModelScope.launch {
            settingsRepository.setHomeGridColumns(columns)
        }
    }

    fun onGridRowsChange(rows: Int) {
        viewModelScope.launch {
            settingsRepository.setHomeGridRows(rows)
        }
    }

    fun onIconScaleChange(scale: Float) {
        viewModelScope.launch {
            settingsRepository.setHomeIconScale(scale)
        }
    }

    fun onShowLabelsChange(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.setHomeShowLabels(show)
        }
    }

    fun onLabelScaleChange(scale: Float) {
        viewModelScope.launch {
            settingsRepository.setHomeLabelScale(scale)
        }
    }

    fun onLockLayoutChange(locked: Boolean) {
        viewModelScope.launch {
            settingsRepository.setHomeLockLayout(locked)
        }
    }

    fun onShowClockChange(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowClock(show)
        }
    }

    fun onClockStyleChange(style: HomeClockStyle) {
        viewModelScope.launch {
            settingsRepository.setHomeClockStyle(style)
        }
    }

    class Factory(
        private val settingsRepository: SettingsRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeSettingsViewModel(settingsRepository = settingsRepository) as T
        }
    }
}
