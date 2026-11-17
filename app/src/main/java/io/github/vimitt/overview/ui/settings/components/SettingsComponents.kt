package io.github.vimitt.overview.ui.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.vimitt.overview.R
import io.github.vimitt.overview.ui.components.ExpressiveButtonGroup
import io.github.vimitt.overview.ui.theme.OverviewTheme
import io.github.vimitt.overview.ui.theme.bestOnColor

val LocalSettingsDualPane = compositionLocalOf { false }

/**
 * Estrutura base das páginas de configurações no estilo Adwaita (HeaderBar leve + lista agrupada).
 */
@Composable
fun SettingsScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit,
) {
    val colors = OverviewTheme.colors

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(colors.windowBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        val horizontalPad = if (maxWidth >= 600.dp) 24.dp else 16.dp
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SettingsTopBar(
                title = title,
                onBack = onBack,
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 720.dp)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = horizontalPad, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                content = content,
            )
        }
    }
}

/**
 * Cabeçalho limpo no estilo libadwaita HeaderBar com botão de retorno >= 48dp.
 */
@Composable
fun SettingsTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    showBackButton: Boolean = !LocalSettingsDualPane.current,
) {
    val colors = OverviewTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp)
            .defaultMinSize(minHeight = 56.dp)
            .padding(
                start = if (showBackButton) 8.dp else 24.dp,
                end = 16.dp,
                top = 6.dp,
                bottom = 6.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showBackButton) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.settings_back_content_description),
                    tint = colors.onSurface,
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = title,
            style = OverviewTheme.typography.topBarTitle,
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Contêiner agrupado de linhas estilo `AdwPreferencesGroup`, usando `surfaceContainer`
 * e cantos arredondados com separadores finos. No estilo Expressive adiciona borda tonal
 * sutil e cabeçalho com destaque tipográfico.
 */
@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = OverviewTheme.colors
    val shapes = OverviewTheme.shapes
    val components = OverviewTheme.components
    val typography = OverviewTheme.typography
    val shape = shapes.settingsGroup
    val borderColor = if (components.useTonalCardBorder) {
        colors.outlineVariant
    } else {
        colors.hairlineBorder
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (!title.isNullOrBlank()) {
            Text(
                text = title,
                style = typography.sectionHeader,
                color = colors.onSurfaceMuted,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.surfaceContainer)
                .border(width = 1.dp, color = borderColor, shape = shape),
            content = content,
        )
    }
}

@Composable
fun SettingsDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        thickness = 1.dp,
        color = OverviewTheme.colors.hairlineBorder,
    )
}

/**
 * Linha genérica de preferência estilo `AdwActionRow` com área de toque >= 48dp.
 */
@Composable
fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    role: Role? = null,
    titleColor: Color? = null,
    subtitleColor: Color? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = OverviewTheme.colors
    val clickModifier = if (onClick != null) {
        Modifier.clickable(enabled = enabled, role = role, onClick = onClick)
    } else {
        Modifier
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .then(clickModifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                icon()
            }
            Spacer(modifier = Modifier.width(12.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = OverviewTheme.typography.rowTitle,
                color = when {
                    !enabled -> colors.onSurfaceMuted
                    titleColor != null -> titleColor
                    else -> colors.onSurface
                },
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = subtitleColor ?: colors.onSurfaceMuted,
                )
            }
        }

        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(12.dp))
            trailingContent()
        }
    }
}

/**
 * Linha de navegação para uma subcategoria com seta indicativa à direita.
 */
@Composable
fun NavigationRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    selected: Boolean = false,
) {
    val colors = OverviewTheme.colors
    val selectedModifier = if (selected) {
        modifier
            .background(colors.accentContainer.copy(alpha = 0.65f))
            .semantics { this.selected = true }
    } else {
        modifier
    }
    SettingsRow(
        title = title,
        subtitle = subtitle,
        icon = icon,
        enabled = enabled,
        onClick = onClick,
        role = Role.Button,
        titleColor = if (selected) colors.onAccentContainer else null,
        subtitleColor = if (selected) colors.onAccentContainer.copy(alpha = 0.80f) else null,
        modifier = selectedModifier,
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = if (selected) colors.onAccentContainer else colors.onSurfaceMuted,
            )
        },
    )
}

/**
 * Linha com interruptor estilo `AdwSwitchRow`.
 */
@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
) {
    val colors = OverviewTheme.colors
    val useIconSwitch = OverviewTheme.components.useIconSwitch
    SettingsRow(
        title = title,
        subtitle = subtitle,
        icon = icon,
        enabled = enabled,
        role = Role.Switch,
        onClick = { onCheckedChange(!checked) },
        modifier = modifier,
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
                thumbContent = if (useIconSwitch) {
                    {
                        Icon(
                            imageVector = if (checked) Icons.Filled.Check else Icons.Filled.Close,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize),
                        )
                    }
                } else {
                    null
                },
                colors = if (useIconSwitch) {
                    SwitchDefaults.colors(
                        checkedThumbColor = colors.onAccent,
                        checkedTrackColor = colors.accent,
                        checkedIconColor = colors.accent,
                        uncheckedThumbColor = colors.onSurfaceMuted,
                        uncheckedTrackColor = colors.surfaceElevated,
                        uncheckedBorderColor = colors.hairlineBorder,
                        uncheckedIconColor = colors.surfaceElevated,
                    )
                } else {
                    SwitchDefaults.colors(
                        checkedThumbColor = colors.onAccent,
                        checkedTrackColor = colors.accent,
                    )
                },
            )
        },
    )
}

/**
 * Linha com controle deslizante e rótulo do valor atual exibido à direita.
 */
@Composable
fun SliderRow(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    steps: Int = 0,
    enabled: Boolean = true,
) {
    val colors = OverviewTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = OverviewTheme.typography.rowTitle,
                    color = if (enabled) colors.onSurface else colors.onSurfaceMuted,
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceMuted,
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (enabled) colors.accent else colors.onSurfaceMuted,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = colors.accent,
                activeTrackColor = colors.accent,
                inactiveTrackColor = colors.surfaceElevated,
            ),
        )
    }
}

/**
 * Seletor segmentado horizontal para poucas opções mutuamente exclusivas.
 * No estilo Expressive utiliza botões segmentados do Material 3 ou grupo conectado.
 */
@Composable
fun <T> SegmentedRow(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    useButtonGroup: Boolean = false,
) {
    val colors = OverviewTheme.colors
    val components = OverviewTheme.components
    val containerShape = OverviewTheme.shapes.small

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        if (!title.isNullOrBlank()) {
            Text(
                text = title,
                style = OverviewTheme.typography.rowTitle,
                color = colors.onSurface,
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceMuted,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (components.useExpressiveSegmented) {
            if (useButtonGroup) {
                ExpressiveButtonGroup(
                    options = options,
                    selected = selected,
                    onSelect = onSelect,
                )
            } else {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    options.forEachIndexed { index, (optionValue, label) ->
                        val isSelected = optionValue == selected
                        SegmentedButton(
                            selected = isSelected,
                            onClick = { onSelect(optionValue) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = options.size,
                            ),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = colors.accentContainer,
                                activeContentColor = colors.onAccentContainer,
                                activeBorderColor = colors.accent.copy(alpha = 0.55f),
                                inactiveContainerColor = colors.surfaceElevated,
                                inactiveContentColor = colors.onSurface,
                                inactiveBorderColor = colors.hairlineBorder,
                            ),
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(containerShape)
                    .background(colors.surfaceElevated)
                    .padding(4.dp)
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                for ((optionValue, label) in options) {
                    val isSelected = optionValue == selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(containerShape)
                            .background(if (isSelected) colors.accent else Color.Transparent)
                            .selectable(
                                selected = isSelected,
                                role = Role.RadioButton,
                                onClick = { onSelect(optionValue) },
                            )
                            .padding(horizontal = 6.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isSelected) colors.onAccent else colors.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Linha de seleção única que abre um diálogo Adwaita com opções em lista.
 */
@Composable
fun <T> SingleChoiceRow(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    subtitleOverride: String? = null,
    enabled: Boolean = true,
) {
    val colors = OverviewTheme.colors
    var dialogOpen by remember { mutableStateOf(false) }
    val selectedLabel = subtitleOverride
        ?: options.firstOrNull { it.first == selected }?.second.orEmpty()

    NavigationRow(
        title = title,
        subtitle = selectedLabel,
        enabled = enabled,
        onClick = { dialogOpen = true },
        modifier = modifier,
    )

    if (dialogOpen) {
        val isExpressive = OverviewTheme.components.isExpressive
        AlertDialog(
            onDismissRequest = { dialogOpen = false },
            shape = OverviewTheme.shapes.large,
            containerColor = if (isExpressive) colors.surfaceContainerHigh else colors.surfaceContainer,
            titleContentColor = colors.onSurface,
            textContentColor = colors.onSurface,
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().selectableGroup()) {
                    for ((value, label) in options) {
                        val isSelected = value == selected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .clip(OverviewTheme.shapes.small)
                                .selectable(
                                    selected = isSelected,
                                    role = Role.RadioButton,
                                    onClick = {
                                        onSelect(value)
                                        dialogOpen = false
                                    },
                                )
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = colors.accent,
                                    unselectedColor = colors.onSurfaceMuted,
                                ),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.onSurface,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { dialogOpen = false }) {
                    Text(
                        text = stringResource(R.string.settings_close_dialog),
                        color = colors.accent,
                    )
                }
            },
        )
    }
}

/**
 * Linha de seleção de cor de destaque com pré-visualização e paleta Adwaita.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickerRow(
    title: String,
    presets: List<Pair<String, Long>>,
    selectedArgb: Long,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    val colors = OverviewTheme.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = OverviewTheme.typography.rowTitle,
                    color = colors.onSurface,
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceMuted,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(colors.accent)
                    .border(1.dp, colors.hairlineBorder, CircleShape),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        FlowRow(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            for ((label, argb) in presets) {
                val isSelected = selectedArgb == argb
                val swatchColor = Color(argb)
                val checkTint = bestOnColor(swatchColor)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .selectable(
                            selected = isSelected,
                            role = Role.RadioButton,
                            onClick = { onSelect(argb) },
                        )
                        .semantics { contentDescription = label },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(swatchColor)
                            .then(
                                if (isSelected) {
                                    Modifier.border(2.5.dp, colors.onSurface, CircleShape)
                                } else {
                                    Modifier
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = checkTint,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
