package io.github.vimitt.overview.ui.settings

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.vimitt.overview.data.SettingsRepository
import io.github.vimitt.overview.domain.model.LauncherPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@Immutable
data class SettingsHubUiState(
    val preferences: LauncherPreferences = LauncherPreferences(),
)

/**
 * ViewModel da tela raiz de configurações responsável pelos resumos dinâmicos das categorias.
 */
class SettingsHubViewModel(
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<SettingsHubUiState> = settingsRepository.preferences
        .map { SettingsHubUiState(preferences = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsHubUiState(),
        )

    class Factory(
        private val settingsRepository: SettingsRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsHubViewModel(settingsRepository = settingsRepository) as T
        }
    }
}
