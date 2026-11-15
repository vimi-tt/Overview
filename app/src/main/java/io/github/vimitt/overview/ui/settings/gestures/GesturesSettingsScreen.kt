package io.github.vimitt.overview.ui.settings.gestures

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.vimitt.overview.R
import io.github.vimitt.overview.domain.model.GestureSensitivity
import io.github.vimitt.overview.domain.model.LauncherGestureAction
import io.github.vimitt.overview.ui.settings.components.SegmentedRow
import io.github.vimitt.overview.ui.settings.components.SettingsDivider
import io.github.vimitt.overview.ui.settings.components.SettingsGroup
import io.github.vimitt.overview.ui.settings.components.SettingsScaffold
import io.github.vimitt.overview.ui.settings.components.SingleChoiceRow

@Composable
fun GesturesSettingsScreen(
    uiState: GesturesSettingsUiState,
    onSwipeUpChange: (LauncherGestureAction) -> Unit,
    onSwipeDownChange: (LauncherGestureAction) -> Unit,
    onDoubleTapChange: (LauncherGestureAction) -> Unit,
    onHomeButtonChange: (LauncherGestureAction) -> Unit,
    onSensitivityChange: (GestureSensitivity) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings = uiState.settings
    val actionOptions = listOf(
        LauncherGestureAction.NONE to stringResource(R.string.settings_gesture_action_none),
        LauncherGestureAction.OPEN_OVERVIEW to stringResource(R.string.settings_gesture_action_overview),
        LauncherGestureAction.OPEN_OVERVIEW_SEARCH to stringResource(R.string.settings_gesture_action_overview_search),
        LauncherGestureAction.EXPAND_NOTIFICATIONS to stringResource(R.string.settings_gesture_action_notifications),
        LauncherGestureAction.EXPAND_QUICK_SETTINGS to stringResource(R.string.settings_gesture_action_quick_settings),
        LauncherGestureAction.OPEN_SETTINGS to stringResource(R.string.settings_gesture_action_settings),
        LauncherGestureAction.OPEN_WALLPAPER to stringResource(R.string.settings_gesture_action_wallpaper),
        LauncherGestureAction.OPEN_ASSISTANT to stringResource(R.string.settings_gesture_action_assistant),
        LauncherGestureAction.OPEN_CAMERA to stringResource(R.string.settings_gesture_action_camera),
    )

    SettingsScaffold(
        title = stringResource(R.string.settings_hub_gestures),
        onBack = onBack,
        modifier = modifier,
    ) {
        item(key = "group_gestures") {
            SettingsGroup(title = stringResource(R.string.settings_section_gestures_home)) {
                SingleChoiceRow(
                    title = stringResource(R.string.settings_gesture_swipe_up),
                    options = actionOptions,
                    selected = settings.swipeUpAction,
                    onSelect = onSwipeUpChange,
                )
                SettingsDivider()
                SingleChoiceRow(
                    title = stringResource(R.string.settings_gesture_swipe_down),
                    options = actionOptions,
                    selected = settings.swipeDownAction,
                    onSelect = onSwipeDownChange,
                )
                SettingsDivider()
                SingleChoiceRow(
                    title = stringResource(R.string.settings_gesture_double_tap),
                    options = actionOptions,
                    selected = settings.doubleTapAction,
                    onSelect = onDoubleTapChange,
                )
                SettingsDivider()
                SingleChoiceRow(
                    title = stringResource(R.string.settings_gesture_home_button),
                    options = actionOptions.filter {
                        it.first == LauncherGestureAction.NONE ||
                            it.first == LauncherGestureAction.OPEN_OVERVIEW ||
                            it.first == LauncherGestureAction.OPEN_OVERVIEW_SEARCH ||
                            it.first == LauncherGestureAction.EXPAND_NOTIFICATIONS ||
                            it.first == LauncherGestureAction.OPEN_ASSISTANT ||
                            it.first == LauncherGestureAction.OPEN_CAMERA
                    },
                    selected = settings.homeButtonAction,
                    onSelect = onHomeButtonChange,
                )
            }
        }

        item(key = "group_feel") {
            SettingsGroup(title = stringResource(R.string.settings_section_gestures_feel)) {
                SegmentedRow(
                    title = stringResource(R.string.settings_gesture_sensitivity),
                    options = listOf(
                        GestureSensitivity.LOW to stringResource(R.string.settings_gesture_sensitivity_low),
                        GestureSensitivity.MEDIUM to stringResource(R.string.settings_gesture_sensitivity_medium),
                        GestureSensitivity.HIGH to stringResource(R.string.settings_gesture_sensitivity_high),
                    ),
                    selected = settings.sensitivity,
                    onSelect = onSensitivityChange,
                )
            }
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun gestureActionLabel(action: LauncherGestureAction): String {
    return when (action) {
        LauncherGestureAction.NONE -> stringResource(R.string.settings_gesture_action_none)
        LauncherGestureAction.OPEN_OVERVIEW -> stringResource(R.string.settings_gesture_action_overview)
        LauncherGestureAction.OPEN_OVERVIEW_SEARCH -> stringResource(R.string.settings_gesture_action_overview_search)
        LauncherGestureAction.EXPAND_NOTIFICATIONS -> stringResource(R.string.settings_gesture_action_notifications)
        LauncherGestureAction.EXPAND_QUICK_SETTINGS -> stringResource(R.string.settings_gesture_action_quick_settings)
        LauncherGestureAction.OPEN_SETTINGS -> stringResource(R.string.settings_gesture_action_settings)
        LauncherGestureAction.OPEN_WALLPAPER -> stringResource(R.string.settings_gesture_action_wallpaper)
        LauncherGestureAction.OPEN_ASSISTANT -> stringResource(R.string.settings_gesture_action_assistant)
        LauncherGestureAction.OPEN_CAMERA -> stringResource(R.string.settings_gesture_action_camera)
    }
}
