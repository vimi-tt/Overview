package io.github.vimitt.overview.ui.home.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.vimitt.overview.R
import io.github.vimitt.overview.domain.model.QuickNoteItem
import io.github.vimitt.overview.domain.model.WeatherLocation
import io.github.vimitt.overview.domain.model.WeatherProvider
import io.github.vimitt.overview.ui.components.OverviewPopupSurface
import io.github.vimitt.overview.ui.theme.OverviewTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Diálogo leve para criar ou editar uma nota rápida direto sobre a tela inicial.
 */
@Composable
fun QuickNoteDialog(
    initialNote: QuickNoteItem?,
    onSave: (content: String, noteId: String?) -> Unit,
    onDelete: (noteId: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = OverviewTheme.colors
    var text by remember(initialNote) { mutableStateOf(initialNote?.content.orEmpty()) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(80)
        runCatching { focusRequester.requestFocus() }
    }

    OverviewPopupSurface(
        onDismiss = onDismiss,
        bottomSheet = false,
        enableImePadding = true,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.native_widget_notes_dialog_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.onSurface,
            )

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(stringResource(R.string.native_widget_notes_placeholder)) },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (initialNote != null) {
                    TextButton(
                        onClick = {
                            onDelete(initialNote.id)
                            onDismiss()
                        },
                    ) {
                        Text(text = stringResource(R.string.native_widget_notes_delete))
                    }
                } else {
                    Spacer(modifier = Modifier.height(1.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(R.string.settings_cancel))
                    }
                    Button(
                        enabled = text.isNotBlank(),
                        onClick = {
                            onSave(text, initialNote?.id)
                            onDismiss()
                        },
                    ) {
                        Text(text = stringResource(R.string.native_widget_notes_save))
                    }
                }
            }
        }
    }
}

/**
 * Diálogo leve para adicionar uma nova tarefa direto da tela inicial.
 */
@Composable
fun QuickTaskDialog(
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = OverviewTheme.colors
    var title by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(80)
        runCatching { focusRequester.requestFocus() }
    }

    val submitTask = {
        if (title.isNotBlank()) {
            onAdd(title)
            onDismiss()
        }
    }

    OverviewPopupSurface(
        onDismiss = onDismiss,
        bottomSheet = false,
        enableImePadding = true,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.native_widget_tasks_dialog_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.onSurface,
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text(stringResource(R.string.native_widget_tasks_placeholder)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submitTask() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.settings_cancel))
                }
                Button(
                    enabled = title.isNotBlank(),
                    onClick = submitTask,
                ) {
                    Text(text = stringResource(R.string.native_widget_tasks_add))
                }
            }
        }
    }
}

/**
 * Diálogo de busca e escolha de cidade e seleção do provedor aberto de clima.
 */
@Composable
fun WeatherCityPickerDialog(
    selectedProvider: WeatherProvider = WeatherProvider.AUTO,
    onSelectProvider: (WeatherProvider) -> Unit = {},
    onSearchCities: suspend (String) -> Result<List<WeatherLocation>>,
    onSelectLocation: (WeatherLocation) -> Unit,
    onUseGpsLocation: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = OverviewTheme.colors
    val shapes = OverviewTheme.shapes
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<WeatherLocation>>(emptyList()) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(80)
        runCatching { focusRequester.requestFocus() }
    }

    val runSearch = {
        if (!searching && query.isNotBlank()) {
            scope.launch {
                searching = true
                hasSearched = true
                results = onSearchCities(query).getOrDefault(emptyList())
                searching = false
            }
        }
    }

    OverviewPopupSurface(
        onDismiss = onDismiss,
        bottomSheet = false,
        enableImePadding = true,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.native_widget_weather_city_dialog_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.onSurface,
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.weather_provider_dialog_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceMuted,
                )
                val providerChips = listOf(
                    WeatherProvider.AUTO to stringResource(R.string.weather_provider_auto),
                    WeatherProvider.MET_NORWAY to "MET Norway",
                    WeatherProvider.OPEN_METEO to "Open-Meteo",
                    WeatherProvider.WTTR_IN to "wttr.in",
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    for ((provider, label) in providerChips) {
                        val selected = provider == selectedProvider
                        Box(
                            modifier = Modifier
                                .clip(shapes.pill)
                                .background(
                                    if (selected) colors.accentContainer else colors.surfaceContainerHigh,
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (selected) colors.accent else colors.hairlineBorder,
                                    shape = shapes.pill,
                                )
                                .clickable { onSelectProvider(provider) }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                ),
                                color = if (selected) colors.onAccentContainer else colors.onSurface,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(R.string.native_widget_weather_city_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { runSearch() }),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                )
                Button(
                    enabled = !searching && query.isNotBlank(),
                    onClick = runSearch,
                ) {
                    Text(text = stringResource(R.string.native_widget_weather_search))
                }
            }

            if (results.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(results) { loc ->
                        val subtitle = listOf(loc.region, loc.country)
                            .filter { it.isNotBlank() }
                            .joinToString(", ")
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(shapes.medium)
                                .background(colors.surfaceContainerHigh)
                                .clickable {
                                    onSelectLocation(loc)
                                    onDismiss()
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        ) {
                            Column {
                                Text(
                                    text = loc.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = colors.onSurface,
                                )
                                if (subtitle.isNotBlank()) {
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.onSurfaceMuted,
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (hasSearched && !searching) {
                Text(
                    text = stringResource(R.string.native_widget_weather_search_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceMuted,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = {
                        onUseGpsLocation()
                        onDismiss()
                    },
                ) {
                    Text(text = stringResource(R.string.native_widget_weather_use_gps))
                }
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.settings_cancel))
                }
            }
        }
    }
}
