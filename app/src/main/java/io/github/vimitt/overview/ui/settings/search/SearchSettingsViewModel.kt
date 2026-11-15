package io.github.vimitt.overview.ui.settings.search

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.vimitt.overview.data.SettingsRepository
import io.github.vimitt.overview.domain.model.SearchSettings
import io.github.vimitt.overview.domain.model.WebSearchEngine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class SearchSettingsUiState(
    val settings: SearchSettings = SearchSettings(),
)

/**
 * ViewModel da subtela Busca.
 */
class SearchSettingsViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<SearchSettingsUiState> = settingsRepository.searchSettings
        .map { SearchSettingsUiState(settings = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SearchSettingsUiState(),
        )

    fun onAutoFocusChange(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSearchAutoFocusOnOpen(enabled)
        }
    }

    fun onMatchPackageNameChange(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSearchMatchPackageName(enabled)
        }
    }

    fun onClearOnCloseChange(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSearchClearOnClose(enabled)
        }
    }

    fun onShowWebSuggestionChange(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSearchShowWebSuggestion(enabled)
        }
    }

    fun onWebEngineChange(engine: WebSearchEngine) {
        viewModelScope.launch {
            settingsRepository.setSearchWebEngine(engine)
        }
    }

    class Factory(
        private val settingsRepository: SettingsRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SearchSettingsViewModel(settingsRepository = settingsRepository) as T
        }
    }
}
