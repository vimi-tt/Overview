package io.github.vimitt.overview.ui.settings.folders

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.vimitt.overview.data.AppsRepository
import io.github.vimitt.overview.data.SettingsRepository
import io.github.vimitt.overview.domain.model.AppInfo
import io.github.vimitt.overview.domain.model.FolderPreviewStyle
import io.github.vimitt.overview.domain.model.FolderSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class FoldersSettingsUiState(
    val settings: FolderSettings = FolderSettings(),
    val allApps: List<AppInfo> = emptyList(),
)

/**
 * ViewModel da subtela Pastas (visual, comportamento e gerenciamento de grupos).
 */
class FoldersSettingsViewModel(
    appsRepository: AppsRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<FoldersSettingsUiState> = combine(
        settingsRepository.folderSettings,
        appsRepository.apps,
    ) { folderSettings, apps ->
        FoldersSettingsUiState(
            settings = folderSettings,
            allApps = apps,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FoldersSettingsUiState(),
    )

    fun onPreviewStyleChange(style: FolderPreviewStyle) {
        viewModelScope.launch {
            settingsRepository.setFolderPreviewStyle(style)
        }
    }

    fun onColumnsChange(columns: Int) {
        viewModelScope.launch {
            settingsRepository.setFolderColumns(columns)
        }
    }

    fun onSortAppsInsideChange(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setFolderSortAppsInside(enabled)
        }
    }

    fun onCloseOnAppLaunchChange(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setFolderCloseOnAppLaunch(enabled)
        }
    }

    fun saveFolder(folderId: String?, name: String, appKeys: List<String>) {
        viewModelScope.launch {
            settingsRepository.saveFolder(
                folderId = folderId,
                name = name,
                appKeys = appKeys,
            )
        }
    }

    fun deleteFolder(folderId: String) {
        viewModelScope.launch {
            settingsRepository.deleteFolder(folderId)
        }
    }

    class Factory(
        private val appsRepository: AppsRepository,
        private val settingsRepository: SettingsRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FoldersSettingsViewModel(
                appsRepository = appsRepository,
                settingsRepository = settingsRepository,
            ) as T
        }
    }
}
