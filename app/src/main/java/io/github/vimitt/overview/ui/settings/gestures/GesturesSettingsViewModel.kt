package io.github.vimitt.overview.ui.settings.gestures

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.vimitt.overview.data.SettingsRepository
import io.github.vimitt.overview.domain.model.GestureSensitivity
import io.github.vimitt.overview.domain.model.GestureSettings
import io.github.vimitt.overview.domain.model.LauncherGestureAction
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class GesturesSettingsUiState(
    val settings: GestureSettings = GestureSettings(),
)

/**
 * ViewModel da subtela Gestos.
 */
class GesturesSettingsViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<GesturesSettingsUiState> = settingsRepository.gestureSettings
        .map { GesturesSettingsUiState(settings = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = GesturesSettingsUiState(),
        )

    fun onSwipeUpChange(action: LauncherGestureAction) {
        viewModelScope.launch {
            settingsRepository.setSwipeUpGesture(action)
        }
    }

    fun onSwipeDownChange(action: LauncherGestureAction) {
        viewModelScope.launch {
            settingsRepository.setSwipeDownGesture(action)
        }
    }

    fun onDoubleTapChange(action: LauncherGestureAction) {
        viewModelScope.launch {
            settingsRepository.setDoubleTapGesture(action)
        }
    }

    fun onHomeButtonChange(action: LauncherGestureAction) {
        viewModelScope.launch {
            settingsRepository.setHomeButtonGesture(action)
        }
    }

    fun onSensitivityChange(sensitivity: GestureSensitivity) {
        viewModelScope.launch {
            settingsRepository.setGestureSensitivity(sensitivity)
        }
    }

    class Factory(
        private val settingsRepository: SettingsRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GesturesSettingsViewModel(settingsRepository = settingsRepository) as T
        }
    }
}
