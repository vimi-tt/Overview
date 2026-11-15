package io.github.vimitt.overview.ui.settings.overview

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.vimitt.overview.data.SettingsRepository
import io.github.vimitt.overview.domain.model.OverviewAnimationSpeed
import io.github.vimitt.overview.domain.model.OverviewSettings
import io.github.vimitt.overview.domain.model.OverviewTransitionStyle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class OverviewSettingsUiState(
    val settings: OverviewSettings = OverviewSettings(),
)

/**
 * ViewModel da subtela Overview (desfoque, transparência e animações).
 */
class OverviewSettingsViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<OverviewSettingsUiState> = settingsRepository.overviewSettings
        .map { OverviewSettingsUiState(settings = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = OverviewSettingsUiState(),
        )

    fun onBlurRadiusChange(radiusDp: Float) {
        viewModelScope.launch {
            settingsRepository.setOverviewBlurRadius(radiusDp)
        }
    }

    fun onScrimAlphaChange(alpha: Float) {
        viewModelScope.launch {
            settingsRepository.setOverviewScrimAlpha(alpha)
        }
    }

    fun onTransitionStyleChange(style: OverviewTransitionStyle) {
        viewModelScope.launch {
            settingsRepository.setOverviewTransitionStyle(style)
        }
    }

    fun onAnimationSpeedChange(speed: OverviewAnimationSpeed) {
        viewModelScope.launch {
            settingsRepository.setOverviewAnimationSpeed(speed)
        }
    }

    fun onHomeDepthEffectChange(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setOverviewHomeDepthEffect(enabled)
        }
    }

    fun onAppLaunchAnimationChange(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setOverviewAppLaunchAnimation(enabled)
        }
    }

    class Factory(
        private val settingsRepository: SettingsRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OverviewSettingsViewModel(settingsRepository = settingsRepository) as T
        }
    }
}
