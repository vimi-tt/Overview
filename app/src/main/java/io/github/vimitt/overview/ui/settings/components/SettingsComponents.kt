package io.github.vimitt.overview.ui.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.vimitt.overview.R
import io.github.vimitt.overview.ui.theme.OverviewCornerRadius
import io.github.vimitt.overview.ui.theme.OverviewTheme

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.windowBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SettingsTopBar(
            title = title,
            onBack = onBack,
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = 620.dp)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            content = content,
        )
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
) {
    val colors = OverviewTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 620.dp)
            .defaultMinSize(minHeight = 56.dp)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Contêiner agrupado de linhas estilo `AdwPreferencesGroup`, usando `surfaceContainer`
 * e cantos arredondados com separadores finos.
 */
@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = OverviewTheme.colors
    val shape = RoundedCornerShape(OverviewCornerRadius.SettingsGroup)

    Column(modifier = modifier.fillMaxWidth()) {
        if (!title.isNullOrBlank()) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.onSurfaceMuted,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.surfaceContainer)
                .border(width = 1.dp, color = colors.hairlineBorder, shape = shape),
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
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center,
            ) {
                icon()
            }
            Spacer(modifier = Modifier.width(12.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
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
) {
    val colors = OverviewTheme.colors
    SettingsRow(
        title = title,
        subtitle = subtitle,
        icon = icon,
        onClick = onClick,
        role = Role.Button,
        modifier = modifier,
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.onSurfaceMuted,
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
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colors.onAccent,
                    checkedTrackColor = colors.accent,
                ),
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
                    style = MaterialTheme.typography.titleMedium,
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
 */
@Composable
fun <T> SegmentedRow(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    val colors = OverviewTheme.colors
    val containerShape = RoundedCornerShape(OverviewCornerRadius.Small)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        if (!title.isNullOrBlank()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(containerShape)
                .background(colors.surfaceElevated)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            for ((optionValue, label) in options) {
                val isSelected = optionValue == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) colors.accent else Color.Transparent)
                        .clickable(role = Role.RadioButton) { onSelect(optionValue) }
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
) {
    val colors = OverviewTheme.colors
    var dialogOpen by remember { mutableStateOf(false) }
    val selectedLabel = subtitleOverride
        ?: options.firstOrNull { it.first == selected }?.second.orEmpty()

    NavigationRow(
        title = title,
        subtitle = selectedLabel,
        onClick = { dialogOpen = true },
        modifier = modifier,
    )

    if (dialogOpen) {
        AlertDialog(
            onDismissRequest = { dialogOpen = false },
            containerColor = colors.surfaceContainer,
            titleContentColor = colors.onSurface,
            textContentColor = colors.onSurface,
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    for ((value, label) in options) {
                        val isSelected = value == selected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .clip(RoundedCornerShape(OverviewCornerRadius.Small))
                                .clickable(role = Role.RadioButton) {
                                    onSelect(value)
                                    dialogOpen = false
                                }
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
                    style = MaterialTheme.typography.titleMedium,
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
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            for ((label, argb) in presets) {
                val isSelected = selectedArgb == argb
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.RadioButton) { onSelect(argb) }
                        .semantics { contentDescription = label },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(argb))
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
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
