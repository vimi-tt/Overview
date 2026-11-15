package io.github.vimitt.overview.ui.settings.search

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.vimitt.overview.R
import io.github.vimitt.overview.domain.model.WebSearchEngine
import io.github.vimitt.overview.ui.settings.components.SettingsDivider
import io.github.vimitt.overview.ui.settings.components.SettingsGroup
import io.github.vimitt.overview.ui.settings.components.SettingsScaffold
import io.github.vimitt.overview.ui.settings.components.SingleChoiceRow
import io.github.vimitt.overview.ui.settings.components.SwitchRow

@Composable
fun SearchSettingsScreen(
    uiState: SearchSettingsUiState,
    onAutoFocusChange: (Boolean) -> Unit,
    onMatchPackageNameChange: (Boolean) -> Unit,
    onClearOnCloseChange: (Boolean) -> Unit,
    onShowWebSuggestionChange: (Boolean) -> Unit,
    onWebEngineChange: (WebSearchEngine) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings = uiState.settings

    SettingsScaffold(
        title = stringResource(R.string.settings_hub_search),
        onBack = onBack,
        modifier = modifier,
    ) {
        item(key = "group_behavior") {
            SettingsGroup(title = stringResource(R.string.settings_section_search_behavior)) {
                SwitchRow(
                    title = stringResource(R.string.settings_search_autofocus),
                    subtitle = stringResource(R.string.settings_search_autofocus_subtitle),
                    checked = settings.autoFocusOnOpen,
                    onCheckedChange = onAutoFocusChange,
                )
                SettingsDivider()
                SwitchRow(
                    title = stringResource(R.string.settings_search_match_pkg),
                    subtitle = stringResource(R.string.settings_search_match_pkg_subtitle),
                    checked = settings.matchPackageName,
                    onCheckedChange = onMatchPackageNameChange,
                )
                SettingsDivider()
                SwitchRow(
                    title = stringResource(R.string.settings_search_clear_close),
                    subtitle = stringResource(R.string.settings_search_clear_close_subtitle),
                    checked = settings.clearOnClose,
                    onCheckedChange = onClearOnCloseChange,
                )
            }
        }

        item(key = "group_web") {
            SettingsGroup(title = stringResource(R.string.settings_section_search_web)) {
                SwitchRow(
                    title = stringResource(R.string.settings_search_web_suggestion),
                    subtitle = stringResource(R.string.settings_search_web_suggestion_subtitle),
                    checked = settings.showWebSearchSuggestion,
                    onCheckedChange = onShowWebSuggestionChange,
                )
                if (settings.showWebSearchSuggestion) {
                    SettingsDivider()
                    SingleChoiceRow(
                        title = stringResource(R.string.settings_search_engine_label),
                        options = listOf(
                            WebSearchEngine.DUCKDUCKGO to stringResource(R.string.settings_search_engine_duckduckgo),
                            WebSearchEngine.GOOGLE to stringResource(R.string.settings_search_engine_google),
                            WebSearchEngine.STARTPAGE to stringResource(R.string.settings_search_engine_startpage),
                            WebSearchEngine.ECOSIA to stringResource(R.string.settings_search_engine_ecosia),
                        ),
                        selected = settings.webSearchEngine,
                        onSelect = onWebEngineChange,
                    )
                }
            }
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
