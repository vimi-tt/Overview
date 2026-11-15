package io.github.vimitt.overview.ui.settings.appearance

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.vimitt.overview.data.IconPackInfo
import io.github.vimitt.overview.data.IconPackRepository
import io.github.vimitt.overview.data.NotificationBadgeRepository
import io.github.vimitt.overview.data.SettingsRepository
import io.github.vimitt.overview.domain.model.AppearanceSettings
import io.github.vimitt.overview.domain.model.IconShape
import io.github.vimitt.overview.domain.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class AppearanceUiState(
    val settings: AppearanceSettings = AppearanceSettings(),
    val installedIconPacks: List<IconPackInfo> = emptyList(),
    val isLoadingIconPacks: Boolean = true,
    val hasNotificationAccess: Boolean = false,
)

/**
 * ViewModel da subtela Aparência.
 */
class AppearanceViewModel(
    private val settingsRepository: SettingsRepository,
    private val iconPackRepository: IconPackRepository,
    private val notificationBadgeRepository: NotificationBadgeRepository,
) : ViewModel() {

    private val installedPacks = MutableStateFlow<List<IconPackInfo>>(emptyList())
    private val loadingPacks = MutableStateFlow(true)
    private val notificationAccess = MutableStateFlow(notificationBadgeRepository.hasNotificationAccess())
    private var pendingEnableNotificationDots = false

    val uiState: StateFlow<AppearanceUiState> = combine(
        settingsRepository.appearanceSettings,
        installedPacks,
        loadingPacks,
        notificationAccess,
    ) { settings, packs, isLoading, hasAccess ->
        AppearanceUiState(
            settings = settings,
            installedIconPacks = packs,
            isLoadingIconPacks = isLoading,
            hasNotificationAccess = hasAccess,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppearanceUiState(
            hasNotificationAccess = notificationAccess.value,
        ),
    )

    init {
        viewModelScope.launch {
            installedPacks.value = iconPackRepository.loadInstalledIconPacks()
            loadingPacks.value = false
        }
    }

    fun onThemeModeChange(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun onAccentColorChange(argb: Long) {
        viewModelScope.launch {
            settingsRepository.setAccentColor(argb)
        }
    }

    fun onIconPackSelect(packageName: String?) {
        viewModelScope.launch {
            settingsRepository.setIconPackPackage(packageName)
        }
    }

    fun onIconShapeChange(shape: IconShape) {
        viewModelScope.launch {
            settingsRepository.setIconShape(shape)
        }
    }

    /**
     * Tenta ativar os pontos de notificação; retorna `true` se o usuário precisa
     * conceder permissão nas configurações do sistema antes de concluir.
     */
    fun onNotificationDotsToggle(enabled: Boolean): Boolean {
        val granted = notificationBadgeRepository.hasNotificationAccess()
        notificationAccess.value = granted
        if (enabled && !granted) {
            pendingEnableNotificationDots = true
            return true
        }
        pendingEnableNotificationDots = false
        viewModelScope.launch {
            settingsRepository.setNotificationDotsEnabled(enabled)
        }
        return false
    }

    fun refreshNotificationAccess() {
        val granted = notificationBadgeRepository.hasNotificationAccess()
        notificationAccess.value = granted
        if (pendingEnableNotificationDots && granted) {
            pendingEnableNotificationDots = false
            viewModelScope.launch {
                settingsRepository.setNotificationDotsEnabled(true)
            }
        }
    }

    class Factory(
        private val settingsRepository: SettingsRepository,
        private val iconPackRepository: IconPackRepository,
        private val notificationBadgeRepository: NotificationBadgeRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AppearanceViewModel(
                settingsRepository = settingsRepository,
                iconPackRepository = iconPackRepository,
                notificationBadgeRepository = notificationBadgeRepository,
            ) as T
        }
    }
}
