package io.github.vimitt.overview.ui.home

import androidx.compose.runtime.Immutable
import io.github.vimitt.overview.domain.model.AppInfo

/**
 * Estado imutável da tela inicial (Home).
 */
@Immutable
data class HomeUiState(
    val dashApps: List<AppInfo> = emptyList(),
    val showClock: Boolean = true,
)
