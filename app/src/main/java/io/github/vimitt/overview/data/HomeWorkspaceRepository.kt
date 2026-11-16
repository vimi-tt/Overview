package io.github.vimitt.overview.data

import android.content.ComponentName
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.vimitt.overview.R
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
        preferredPage: Int = 0,
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
                preferredPage = preferredPage,
                allowNextPageFallback = preferredCellX == null || preferredCellY == null,
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
                page = slot.page,
            )
            prefs[KEY_WORKSPACE_ITEMS] = encodeItems(current)
            added = true
        }
        return added
    }

    suspend fun addPinnedShortcut(
        packageName: String,
        shortcutId: String,
        label: String,
        userHandleHash: Int = 0,
        preferredCellX: Int? = null,
        preferredCellY: Int? = null,
        preferredPage: Int = 0,
        columns: Int = io.github.vimitt.overview.domain.model.HomeGridSpec.COLUMNS,
        rows: Int = io.github.vimitt.overview.domain.model.HomeGridSpec.ROWS,
    ): Boolean {
        if (packageName.isBlank() || shortcutId.isBlank()) return false
        var added = false
        context.homeWorkspaceDataStore.edit { prefs ->
            val current = decodeItems(prefs[KEY_WORKSPACE_ITEMS].orEmpty()).toMutableList()
            val slot = placementUseCase.findAvailableSlot(
                existingItems = current,
                desiredSpanX = 1,
                desiredSpanY = 1,
                preferredCellX = preferredCellX,
                preferredCellY = preferredCellY,
                preferredPage = preferredPage,
                allowNextPageFallback = preferredCellX == null || preferredCellY == null,
                columns = columns,
                rows = rows,
            ) ?: return@edit

            current += HomeItem.PinnedShortcut(
                id = "pinned_${UUID.randomUUID()}",
                packageName = packageName,
                shortcutId = shortcutId,
                label = label.ifBlank { shortcutId },
                userHandleHash = userHandleHash,
                cellX = slot.cellX,
                cellY = slot.cellY,
                page = slot.page,
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
        preferredPage: Int = 0,
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
                preferredPage = preferredPage,
                allowNextPageFallback = preferredCellX == null || preferredCellY == null,
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
                page = slot.page,
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
        preferredPage: Int = 0,
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
                preferredPage = preferredPage,
                allowNextPageFallback = preferredCellX == null || preferredCellY == null,
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
                page = slot.page,
            )
            prefs[KEY_WORKSPACE_ITEMS] = encodeItems(current)
            added = true
        }
        return added
    }

    suspend fun addFolder(
        name: String,
        appKeys: List<String>,
        preferredCellX: Int? = null,
        preferredCellY: Int? = null,
        preferredPage: Int = 0,
        columns: Int = io.github.vimitt.overview.domain.model.HomeGridSpec.COLUMNS,
        rows: Int = io.github.vimitt.overview.domain.model.HomeGridSpec.ROWS,
    ): Boolean {
        val cleanName = name.trim().ifBlank { return false }
        val distinctKeys = appKeys.filter { it.isNotBlank() }.distinct()
        if (distinctKeys.isEmpty()) return false

        var added = false
        context.homeWorkspaceDataStore.edit { prefs ->
            val current = decodeItems(prefs[KEY_WORKSPACE_ITEMS].orEmpty()).toMutableList()
            val slot = placementUseCase.findAvailableSlot(
                existingItems = current,
                desiredSpanX = 1,
                desiredSpanY = 1,
                preferredCellX = preferredCellX,
                preferredCellY = preferredCellY,
                preferredPage = preferredPage,
                allowNextPageFallback = preferredCellX == null || preferredCellY == null,
                columns = columns,
                rows = rows,
            ) ?: return@edit

            current += HomeItem.Folder(
                id = "folder_${UUID.randomUUID()}",
                name = cleanName,
                appKeys = distinctKeys,
                cellX = slot.cellX,
                cellY = slot.cellY,
                page = slot.page,
            )
            prefs[KEY_WORKSPACE_ITEMS] = encodeItems(current)
            added = true
        }
        return added
    }

    suspend fun createFolderFromShortcuts(
        sourceShortcutId: String?,
        targetShortcutId: String,
        appKeyToAdd: String,
        defaultName: String,
    ): Boolean {
        if (sourceShortcutId == targetShortcutId || appKeyToAdd.isBlank()) return false
        var created = false
        context.homeWorkspaceDataStore.edit { prefs ->
            val current = decodeItems(prefs[KEY_WORKSPACE_ITEMS].orEmpty()).toMutableList()
            val targetIndex = current.indexOfFirst { it.id == targetShortcutId }
            val target = current.getOrNull(targetIndex) as? HomeItem.AppShortcut ?: return@edit
            val mergedKeys = listOf(target.appKey, appKeyToAdd).filter { it.isNotBlank() }.distinct()
            current[targetIndex] = HomeItem.Folder(
                id = "folder_${UUID.randomUUID()}",
                name = defaultName.trim().ifBlank { context.getString(R.string.home_folder_default_name) },
                appKeys = mergedKeys,
                cellX = target.cellX,
                cellY = target.cellY,
                page = target.page,
            )
            if (sourceShortcutId != null) {
                current.removeAll { it.id == sourceShortcutId }
            }
            prefs[KEY_WORKSPACE_ITEMS] = encodeItems(current)
            created = true
        }
        return created
    }

    suspend fun addAppToFolder(
        folderId: String,
        appKey: String,
        sourceShortcutId: String? = null,
    ): Boolean {
        if (appKey.isBlank()) return false
        var updated = false
        context.homeWorkspaceDataStore.edit { prefs ->
            val current = decodeItems(prefs[KEY_WORKSPACE_ITEMS].orEmpty()).toMutableList()
            val index = current.indexOfFirst { it.id == folderId }
            val folder = current.getOrNull(index) as? HomeItem.Folder ?: return@edit
            val nextKeys = (folder.appKeys + appKey).distinct()
            current[index] = folder.copy(appKeys = nextKeys)
            if (sourceShortcutId != null && sourceShortcutId != folderId) {
                current.removeAll { it.id == sourceShortcutId }
            }
            prefs[KEY_WORKSPACE_ITEMS] = encodeItems(current)
            updated = true
        }
        return updated
    }

    suspend fun removeAppFromFolder(
        folderId: String,
        appKey: String,
    ) {
        context.homeWorkspaceDataStore.edit { prefs ->
            val current = decodeItems(prefs[KEY_WORKSPACE_ITEMS].orEmpty()).toMutableList()
            val index = current.indexOfFirst { it.id == folderId }
            val folder = current.getOrNull(index) as? HomeItem.Folder ?: return@edit
            val remaining = folder.appKeys.filterNot { it == appKey }
            if (remaining.isEmpty()) {
                current.removeAt(index)
            } else {
                current[index] = folder.copy(appKeys = remaining)
            }
            prefs[KEY_WORKSPACE_ITEMS] = encodeItems(current)
        }
    }

    suspend fun updateFolder(
        folderId: String,
        name: String,
        appKeys: List<String>,
    ) {
        val cleanName = name.trim().ifBlank { return }
        val distinctKeys = appKeys.filter { it.isNotBlank() }.distinct()
        context.homeWorkspaceDataStore.edit { prefs ->
            val current = decodeItems(prefs[KEY_WORKSPACE_ITEMS].orEmpty()).toMutableList()
            val index = current.indexOfFirst { it.id == folderId }
            val folder = current.getOrNull(index) as? HomeItem.Folder ?: return@edit
            if (distinctKeys.isEmpty()) {
                current.removeAt(index)
            } else {
                current[index] = folder.copy(name = cleanName, appKeys = distinctKeys)
            }
            prefs[KEY_WORKSPACE_ITEMS] = encodeItems(current)
        }
    }

    suspend fun moveItem(
        id: String,
        newCellX: Int,
        newCellY: Int,
        newPage: Int? = null,
        columns: Int = io.github.vimitt.overview.domain.model.HomeGridSpec.COLUMNS,
        rows: Int = io.github.vimitt.overview.domain.model.HomeGridSpec.ROWS,
    ): Boolean {
        var moved = false
        context.homeWorkspaceDataStore.edit { prefs ->
            val current = decodeItems(prefs[KEY_WORKSPACE_ITEMS].orEmpty()).toMutableList()
            val index = current.indexOfFirst { it.id == id }
            if (index == -1) return@edit
            val item = current[index]
            val targetPage = (newPage ?: item.page).coerceIn(
                0,
                io.github.vimitt.overview.domain.model.HomeGridSpec.MAX_PAGES - 1,
            )

            if (!placementUseCase.canPlace(
                    existingItems = current,
                    cellX = newCellX,
                    cellY = newCellY,
                    spanX = item.spanX,
                    spanY = item.spanY,
                    page = targetPage,
                    ignoreItemId = id,
                    columns = columns,
                    rows = rows,
                )
            ) {
                return@edit
            }

            current[index] = when (item) {
                is HomeItem.AppShortcut -> item.copy(cellX = newCellX, cellY = newCellY, page = targetPage)
                is HomeItem.PinnedShortcut -> item.copy(cellX = newCellX, cellY = newCellY, page = targetPage)
                is HomeItem.Folder -> item.copy(cellX = newCellX, cellY = newCellY, page = targetPage)
                is HomeItem.Widget -> item.copy(cellX = newCellX, cellY = newCellY, page = targetPage)
                is HomeItem.NativeWidget -> item.copy(cellX = newCellX, cellY = newCellY, page = targetPage)
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
        newCellX: Int? = null,
        newCellY: Int? = null,
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

            // Se expandir além da borda direita/inferior, ou se vier nova origem das alças, ajusta os limites.
            val candidateX = (newCellX ?: item.cellX).coerceIn(
                0,
                (columns - safeSpanX).coerceAtLeast(0),
            )
            val candidateY = (newCellY ?: item.cellY).coerceIn(
                0,
                (rows - safeSpanY).coerceAtLeast(0),
            )

            if (!placementUseCase.canPlace(
                    existingItems = current,
                    cellX = candidateX,
                    cellY = candidateY,
                    spanX = safeSpanX,
                    spanY = safeSpanY,
                    page = item.page,
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
                is HomeItem.AppShortcut,
                is HomeItem.PinnedShortcut,
                is HomeItem.Folder,
                -> item
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

    /**
     * Exporta os itens portáveis da tela inicial em JSON (omiti widgets do sistema, pois `appWidgetId` é local).
     */
    suspend fun exportBackupItemsJson(): JSONArray {
        val portableItems = currentItems().filterNot { it is HomeItem.Widget }
        return encodeItemsArray(portableItems)
    }

    /**
     * Restaura os itens portáveis da tela inicial a partir de um backup JSON e retorna os `appWidgetId`s removidos.
     */
    suspend fun importBackupItemsJson(array: JSONArray): List<Int> {
        val decoded = decodeItems(array.toString()).filterNot { it is HomeItem.Widget }
        var removedWidgetIds: List<Int> = emptyList()
        context.homeWorkspaceDataStore.edit { prefs ->
            val current = decodeItems(prefs[KEY_WORKSPACE_ITEMS].orEmpty())
            removedWidgetIds = current.filterIsInstance<HomeItem.Widget>().map { it.appWidgetId }
            prefs[KEY_WORKSPACE_ITEMS] = encodeItems(decoded)
        }
        return removedWidgetIds
    }

    internal fun encodeItems(items: List<HomeItem>): String = encodeItemsArray(items).toString()

    internal fun encodeItemsArray(items: List<HomeItem>): JSONArray {
        val array = JSONArray()
        for (item in items) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("cellX", item.cellX)
                put("cellY", item.cellY)
                put("page", item.page)
                when (item) {
                    is HomeItem.AppShortcut -> {
                        put("type", TYPE_SHORTCUT)
                        put("packageName", item.packageName)
                        put("activityName", item.activityName)
                        put("userHandleHash", item.userHandleHash)
                    }

                    is HomeItem.PinnedShortcut -> {
                        put("type", TYPE_PINNED_SHORTCUT)
                        put("packageName", item.packageName)
                        put("shortcutId", item.shortcutId)
                        put("label", item.label)
                        put("userHandleHash", item.userHandleHash)
                    }

                    is HomeItem.Folder -> {
                        put("type", TYPE_FOLDER)
                        put("name", item.name)
                        val keysArray = JSONArray()
                        for (key in item.appKeys) {
                            keysArray.put(key)
                        }
                        put("appKeys", keysArray)
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
        return array
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
                    val page = obj.optInt("page", 0).coerceIn(
                        0,
                        io.github.vimitt.overview.domain.model.HomeGridSpec.MAX_PAGES - 1,
                    )
                    when (obj.getString("type")) {
                        TYPE_SHORTCUT -> add(
                            HomeItem.AppShortcut(
                                id = id,
                                packageName = obj.getString("packageName"),
                                activityName = obj.getString("activityName"),
                                userHandleHash = obj.optInt("userHandleHash", 0),
                                cellX = cellX,
                                cellY = cellY,
                                page = page,
                            ),
                        )

                        TYPE_PINNED_SHORTCUT -> {
                            val packageName = obj.optString("packageName")
                            val shortcutId = obj.optString("shortcutId")
                            if (packageName.isBlank() || shortcutId.isBlank()) continue
                            add(
                                HomeItem.PinnedShortcut(
                                    id = id,
                                    packageName = packageName,
                                    shortcutId = shortcutId,
                                    label = obj.optString("label").ifBlank { shortcutId },
                                    userHandleHash = obj.optInt("userHandleHash", 0),
                                    cellX = cellX,
                                    cellY = cellY,
                                    page = page,
                                ),
                            )
                        }

                        TYPE_FOLDER -> {
                            val name = obj.optString("name").takeIf { it.isNotBlank() } ?: continue
                            val keysArray = obj.optJSONArray("appKeys")
                            val appKeys = buildList {
                                if (keysArray != null) {
                                    for (j in 0 until keysArray.length()) {
                                        val key = keysArray.optString(j)
                                        if (key.isNotBlank()) add(key)
                                    }
                                }
                            }
                            add(
                                HomeItem.Folder(
                                    id = id,
                                    name = name,
                                    appKeys = appKeys,
                                    cellX = cellX,
                                    cellY = cellY,
                                    page = page,
                                ),
                            )
                        }

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
                                page = page,
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
                                    page = page,
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
        const val TYPE_PINNED_SHORTCUT = "pinned_shortcut"
        const val TYPE_FOLDER = "folder"
        const val TYPE_WIDGET = "widget"
        const val TYPE_NATIVE_WIDGET = "native_widget"
    }
}
