package io.github.vimitt.overview.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.github.vimitt.overview.R
import io.github.vimitt.overview.ui.theme.OverviewTheme

/**
 * Barra de busca no estilo Adwaita para filtrar aplicativos no Overview.
 *
 * Mantém o [TextFieldValue] localmente de forma síncrona para preservar a posição
 * do cursor e a composição do teclado (IME) sem saltos durante a filtragem assrona.
 */
@Composable
fun OverviewSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onOpenSettings: (() -> Unit)? = null,
    onOpenQuickSettings: (() -> Unit)? = null,
) {
    val colors = OverviewTheme.colors
    val motion = OverviewTheme.motion
    val isExpressive = OverviewTheme.components.isExpressive
    var isFocused by remember { mutableStateOf(false) }
    var fieldValue by remember {
        mutableStateOf(TextFieldValue(text = query, selection = TextRange(query.length)))
    }

    // Sincroniza apenas quando a busca é limpa externamente (ex.: botão Limpar ou Home).
    LaunchedEffect(query) {
        if (query.isEmpty() && fieldValue.text.isNotEmpty()) {
            fieldValue = TextFieldValue(text = "", selection = TextRange.Zero)
        }
    }

    val shape = OverviewTheme.shapes.pill
    val backgroundColor by animateColorAsState(
        targetValue = if (isExpressive) {
            lerp(colors.surfaceContainerHigh, colors.accentContainer, if (isFocused) 0.22f else 0.10f)
        } else {
            colors.searchFieldBackground
        },
        animationSpec = motion.effectSpec(durationMillis = 150),
        label = "searchBarBg",
    )
    val borderColor = when {
        isFocused -> colors.accent
        isExpressive -> colors.outlineVariant
        else -> colors.hairlineBorder
    }
    val borderWidth = if (isFocused) 1.5.dp else 1.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .shadow(
                elevation = if (isFocused) 6.dp else 3.dp,
                shape = shape,
                clip = false,
            )
            .clip(shape)
            .background(backgroundColor)
            .border(width = borderWidth, color = borderColor, shape = shape)
            .padding(start = 10.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isFocused -> colors.accentContainer
                        isExpressive -> colors.surfaceElevated
                        else -> colors.accent.copy(alpha = 0.12f)
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = stringResource(R.string.search_apps_content_description),
                tint = when {
                    isFocused -> colors.onAccentContainer
                    isExpressive -> colors.onSurfaceMuted
                    else -> colors.accent
                },
                modifier = Modifier.size(18.dp),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (fieldValue.text.isEmpty()) {
                Text(
                    text = stringResource(R.string.search_apps_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurfaceMuted,
                )
            }
            BasicTextField(
                value = fieldValue,
                onValueChange = { updated ->
                    val previousText = fieldValue.text
                    fieldValue = updated
                    if (updated.text != previousText) {
                        onQueryChange(updated.text)
                    }
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = colors.onSurface,
                ),
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (focusRequester != null) {
                            Modifier.focusRequester(focusRequester)
                        } else {
                            Modifier
                        },
                    )
                    .onFocusChanged { focusState -> isFocused = focusState.isFocused },
            )
        }

        if (fieldValue.text.isNotEmpty()) {
            IconButton(
                onClick = {
                    fieldValue = TextFieldValue(text = "", selection = TextRange.Zero)
                    onClear()
                },
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.clear_search_content_description),
                    tint = colors.onSurfaceMuted,
                    modifier = Modifier.size(18.dp),
                )
            }
        } else if (onOpenQuickSettings != null || onOpenSettings != null) {
            if (onOpenQuickSettings != null) {
                IconButton(onClick = onOpenQuickSettings) {
                    val tint = colors.onSurfaceMuted
                    Canvas(modifier = Modifier.size(18.dp)) {
                        val w = size.width
                        val h = size.height
                        val stroke = (h * 0.10f).coerceAtLeast(1.8f)
                        // Duas barras de sliders estilo GNOME Quick Settings
                        drawLine(
                            color = tint,
                            start = Offset(w * 0.14f, h * 0.32f),
                            end = Offset(w * 0.86f, h * 0.32f),
                            strokeWidth = stroke,
                            cap = StrokeCap.Round,
                        )
                        drawCircle(
                            color = tint,
                            radius = h * 0.13f,
                            center = Offset(w * 0.36f, h * 0.32f),
                        )
                        drawLine(
                            color = tint,
                            start = Offset(w * 0.14f, h * 0.68f),
                            end = Offset(w * 0.86f, h * 0.68f),
                            strokeWidth = stroke,
                            cap = StrokeCap.Round,
                        )
                        drawCircle(
                            color = tint,
                            radius = h * 0.13f,
                            center = Offset(w * 0.66f, h * 0.68f),
                        )
                    }
                }
            }
            if (onOpenSettings != null) {
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = stringResource(R.string.open_settings_content_description),
                        tint = colors.onSurfaceMuted,
                        modifier = Modifier.size(19.dp),
                    )
                }
            }
        }
    }
}
