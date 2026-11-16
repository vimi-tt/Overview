package io.github.vimitt.overview.data

import android.content.ComponentName
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.vimitt.overview.domain.model.AppInfo
import io.github.vimitt.overview.domain.model.HomeItem
import io.github.vimitt.overview.domain.model.NativeWidgetType
import io.github.vimitt.overview.domain.usecase.HomeGridPlacementUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.UUID

private val Context.homeWorkspaceDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "overview_home_workspace",
)

/**
 * Repositório responsável por persistir os atalhos e widgets fixados na tela inicial.
 */
class HomeWorkspaceRepository(
    private val context: Context,
    private val placementUseCase: HomeGridPlacementUseCase,
) {
    val items: Flow<List<HomeItem>> = context.homeWorkspaceDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            decodeItems(prefs[KEY_WORKSPACE_ITEMS].orEmpty())
        }

    suspend fun addAppShortcut(
        app: AppInfo,
        preferredCellX: Int? = null,
        preferredCellY: Int? = null,
        columns: Int = io.github.vimitt.overview.domain.model.HomeGridSpec.COLUMNS,
        rows: Int = io.github.vimitt.overview.domain.model.HomeGridSpec.ROWS,
    ): Boolean {
        var added = false
        context.homeWorkspaceDataStore.edit { prefs ->
            val current = decodeItems(prefs[KEY_WORKSPACE_ITEMS].orEmpty()).toMutableList()
            val slot = placementUseCase.findAvailableSlot(
                existingItems = current,
                desiredSpanX = 1,
                desiredSpanY = 1,
                preferredCellX = preferredCellX,
                preferredCellY = preferredCellY,
                columns = columns,
                rows = rows,
            ) ?: return@edit

            current += HomeItem.AppShortcut(
                id = UUID.randomUUID().toString(),
                packageName = app.packageName,
                activityName = app.activityName,
                userHandleHash = app.userHandle?.hashCode() ?: 0,
                cellX = slot.cellX,
                cellY = slot.cellY,
            )
            prefs[KEY_WORKSPACE_ITEMS] = encodeItems(current)
            added = true
        }
        return added
    }

    suspend fun addWidget(
        appWidgetId: Int,
        provider: ComponentName,
        desiredSpanX: Int,
        desiredSpanY: Int,
        preferredCellX: Int? = null,
        preferredCellY: Int? = null,
        columns: Int = io.github.vimitt.overview.domain.model.HomeGridSpec.COLUMNS,
        rows: Int = io.github.vimitt.overview.domain.model.HomeGridSpec.ROWS,
    ): Boolean {
        var added = false
        context.homeWorkspaceDataStore.edit { prefs ->
            val current = decodeItems(prefs[KEY_WORKSPACE_ITEMS].orEmpty()).toMutableList()
            val slot = placementUseCase.findAvailableSlot(
                existingItems = current,
                desiredSpanX = desiredSpanX,
                desiredSpanY = desiredSpanY,
                preferredCellX = preferredCellX,
                preferredCellY = preferredCellY,
                columns = columns,
                rows = rows,
            ) ?: return@edit

            current += HomeItem.Widget(
                id = "widget_$appWidgetId",
                appWidgetId = appWidgetId,
                providerPackage = provider.packageName,
                providerClass = provider.className,
                cellX = slot.cellX,
                cellY = slot.cellY,
                spanX = slot.spanX,
                spanY = slot.spanY,
            )
            prefs[KEY_WORKSPACE_ITEMS] = encodeItems(current)
            added = true
        }
        return added
    }

    suspend fun addNativeWidget(
        widgetType: NativeWidgetType,
        preferredCellX: Int? = null,
        preferredCellY: Int? = null,
        columns: Int = io.github.vimitt.overview.domain.model.HomeGridSpec.COLUMNS,
        rows: Int = io.github.vimitt.overview.domain.model.HomeGridSpec.ROWS,
    ): Boolean {
        var added = false
        context.homeWorkspaceDataStore.edit { prefs ->
            val current = decodeItems(prefs[KEY_WORKSPACE_ITEMS].orEmpty()).toMutableList()
            val slot = placementUseCase.findAvailableSlot(
                existingItems = current,
                desiredSpanX = widgetType.defaultSpanX,
                desiredSpanY = widgetType.defaultSpanY,
                preferredCellX = preferredCellX,
                preferredCellY = preferredCellY,
                columns = columns,
                rows = rows,
            ) ?: return@edit

            current += HomeItem.NativeWidget(
                id = "native_${widgetType.name}_${UUID.randomUUID()}",
                widgetType = widgetType,
                cellX = slot.cellX,
                cellY = slot.cellY,
                spanX = slot.spanX,
                spanY = slot.spanY,
            )
            prefs[KEY_WORKSPACE_ITEMS] = encodeItems(current)
            added = true
        }
        return added
    }

    suspend fun moveItem(
        id: String,
        newCellX: Int,
        newCellY: Int,
        columns: Int = io.github.vimitt.overview.domain.model.HomeGridSpec.COLUMNS,
        rows: Int = io.github.vimitt.overview.domain.model.HomeGridSpec.ROWS,
    ): Boolean {
        var moved = false
        context.homeWorkspaceDataStore.edit { prefs ->
            val current = decodeItems(prefs[KEY_WORKSPACE_ITEMS].orEmpty()).toMutableList()
            val index = current.indexOfFirst { it.id == id }
            if (index == -1) return@edit
            val item = current[index]

            if (!placementUseCase.canPlace(
                    existingItems = current,
                    cellX = newCellX,
                    cellY = newCellY,
                    spanX = item.spanX,
                    spanY = item.spanY,
                    ignoreItemId = id,
                    columns = columns,
                    rows = rows,
                )
            ) {
                return@edit
            }

            current[index] = when (item) {
                is HomeItem.AppShortcut -> item.copy(cellX = newCellX, cellY = newCellY)
                is HomeItem.Widget -> item.copy(cellX = newCellX, cellY = newCellY)
                is HomeItem.NativeWidget -> item.copy(cellX = newCellX, cellY = newCellY)
            }
            prefs[KEY_WORKSPACE_ITEMS] = encodeItems(current)
            moved = true
        }
        return moved
    }

    suspend fun resizeWidget(
        id: String,
        newSpanX: Int,
        newSpanY: Int,
        columns: Int = io.github.vimitt.overview.domain.model.HomeGridSpec.COLUMNS,
        rows: Int = io.github.vimitt.overview.domain.model.HomeGridSpec.ROWS,
    ): Boolean {
        var resized = false
        context.homeWorkspaceDataStore.edit { prefs ->
            val current = decodeItems(prefs[KEY_WORKSPACE_ITEMS].orEmpty()).toMutableList()
            val index = current.indexOfFirst { it.id == id }
            if (index == -1) return@edit
            val item = current[index]
            if (item !is HomeItem.Widget && item !is HomeItem.NativeWidget) return@edit

            val safeSpanX = if (item is HomeItem.NativeWidget) {
                newSpanX.coerceIn(item.widgetType.minSpanX.coerceAtMost(columns), columns)
            } else {
                newSpanX.coerceIn(1, columns)
            }
            val safeSpanY = if (item is HomeItem.NativeWidget) {
                newSpanY.coerceIn(item.widgetType.minSpanY.coerceAtMost(rows), rows)
            } else {
                newSpanY.coerceIn(1, rows)
            }

            // Se expandir além da borda direita/inferior, tenta recuar a origem automaticamente.
            val candidateX = item.cellX.coerceAtMost(
                (columns - safeSpanX).coerceAtLeast(0),
            )
            val candidateY = item.cellY.coerceAtMost(
                (rows - safeSpanY).coerceAtLeast(0),
            )

            if (!placementUseCase.canPlace(
                    existingItems = current,
                    cellX = candidateX,
                    cellY = candidateY,
                    spanX = safeSpanX,
                    spanY = safeSpanY,
                    ignoreItemId = id,
                    columns = columns,
                    rows = rows,
                )
            ) {
                return@edit
            }

            current[index] = when (item) {
                is HomeItem.Widget -> item.copy(
                    cellX = candidateX,
                    cellY = candidateY,
                    spanX = safeSpanX,
                    spanY = safeSpanY,
                )
                is HomeItem.NativeWidget -> item.copy(
                    cellX = candidateX,
                    cellY = candidateY,
                    spanX = safeSpanX,
                    spanY = safeSpanY,
                )
                is HomeItem.AppShortcut -> item
            }
            prefs[KEY_WORKSPACE_ITEMS] = encodeItems(current)
            resized = true
        }
        return resized
    }

    suspend fun removeItem(id: String): HomeItem? {
        var removed: HomeItem? = null
        context.homeWorkspaceDataStore.edit { prefs ->
            val current = decodeItems(prefs[KEY_WORKSPACE_ITEMS].orEmpty()).toMutableList()
            val index = current.indexOfFirst { it.id == id }
            if (index != -1) {
                removed = current.removeAt(index)
                prefs[KEY_WORKSPACE_ITEMS] = encodeItems(current)
            }
        }
        return removed
    }

    suspend fun currentItems(): List<HomeItem> = items.first()

    internal fun encodeItems(items: List<HomeItem>): String {
        val array = JSONArray()
        for (item in items) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("cellX", item.cellX)
                put("cellY", item.cellY)
                when (item) {
                    is HomeItem.AppShortcut -> {
                        put("type", TYPE_SHORTCUT)
                        put("packageName", item.packageName)
                        put("activityName", item.activityName)
                        put("userHandleHash", item.userHandleHash)
                    }

                    is HomeItem.Widget -> {
                        put("type", TYPE_WIDGET)
                        put("appWidgetId", item.appWidgetId)
                        put("providerPackage", item.providerPackage)
                        put("providerClass", item.providerClass)
                        put("spanX", item.spanX)
                        put("spanY", item.spanY)
                    }

                    is HomeItem.NativeWidget -> {
                        put("type", TYPE_NATIVE_WIDGET)
                        put("widgetType", item.widgetType.name)
                        put("spanX", item.spanX)
                        put("spanY", item.spanY)
                    }
                }
            }
            array.put(obj)
        }
        return array.toString()
    }

    internal fun decodeItems(raw: String): List<HomeItem> {
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList(array.length()) {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.getString("id")
                    val cellX = obj.getInt("cellX")
                    val cellY = obj.getInt("cellY")
                    when (obj.getString("type")) {
                        TYPE_SHORTCUT -> add(
                            HomeItem.AppShortcut(
                                id = id,
                                packageName = obj.getString("packageName"),
                                activityName = obj.getString("activityName"),
                                userHandleHash = obj.optInt("userHandleHash", 0),
                                cellX = cellX,
                                cellY = cellY,
                            ),
                        )

                        TYPE_WIDGET -> add(
                            HomeItem.Widget(
                                id = id,
                                appWidgetId = obj.getInt("appWidgetId"),
                                providerPackage = obj.getString("providerPackage"),
                                providerClass = obj.getString("providerClass"),
                                cellX = cellX,
                                cellY = cellY,
                                spanX = obj.optInt("spanX", 2),
                                spanY = obj.optInt("spanY", 2),
                            ),
                        )

                        TYPE_NATIVE_WIDGET -> {
                            val typeName = obj.optString("widgetType")
                            val widgetType = NativeWidgetType.entries.firstOrNull { it.name == typeName }
                                ?: continue
                            add(
                                HomeItem.NativeWidget(
                                    id = id,
                                    widgetType = widgetType,
                                    cellX = cellX,
                                    cellY = cellY,
                                    spanX = obj.optInt("spanX", widgetType.defaultSpanX),
                                    spanY = obj.optInt("spanY", widgetType.defaultSpanY),
                                ),
                            )
                        }
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    private companion object {
        val KEY_WORKSPACE_ITEMS = stringPreferencesKey("workspace_items_json")
        const val TYPE_SHORTCUT = "shortcut"
        const val TYPE_WIDGET = "widget"
        const val TYPE_NATIVE_WIDGET = "native_widget"
    }
}
