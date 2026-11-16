package io.github.vimitt.overview.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Resultado da importação de um arquivo de backup JSON do Overview Launcher.
 */
data class BackupImportResult(
    val restoredPreferences: Boolean,
    val restoredWorkspaceCount: Int,
)

/**
 * Serializa e restaura backups locais (JSON) de configurações e da tela inicial via SAF.
 */
object LauncherBackupHelper {
    const val BACKUP_SCHEMA_VERSION = 1

    suspend fun exportBackupJson(
        context: Context,
        settingsRepository: SettingsRepository,
        homeWorkspaceRepository: HomeWorkspaceRepository,
    ): String {
        val prefsJson = settingsRepository.exportPreferencesJson()
        val workspaceArray = homeWorkspaceRepository.exportBackupItemsJson()
        val versionName = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
        }.getOrDefault("")

        return buildBackupDocument(
            preferencesJson = prefsJson,
            workspaceArray = workspaceArray,
            appVersionName = versionName,
            exportedAtMillis = System.currentTimeMillis(),
        )
    }

    internal fun buildBackupDocument(
        preferencesJson: JSONObject,
        workspaceArray: JSONArray,
        appVersionName: String = "",
        exportedAtMillis: Long = 0L,
    ): String {
        return JSONObject().apply {
            put("version", BACKUP_SCHEMA_VERSION)
            put("appVersionName", appVersionName)
            put("exportedAtMillis", exportedAtMillis)
            put("preferences", preferencesJson)
            put("workspace", workspaceArray)
        }.toString(2)
    }

    suspend fun importBackupJson(
        raw: String,
        settingsRepository: SettingsRepository,
        homeWorkspaceRepository: HomeWorkspaceRepository,
        appWidgetController: AppWidgetController,
    ): BackupImportResult {
        val root = JSONObject(raw)
        val version = root.optInt("version", -1)
        require(version >= 1) { "Unsupported backup schema version" }

        val prefsObj = root.optJSONObject("preferences")
        val workspaceArray = root.optJSONArray("workspace")
        require(prefsObj != null || workspaceArray != null) { "Backup missing payload" }

        var restoredPrefs = false
        if (prefsObj != null) {
            settingsRepository.importPreferencesJson(prefsObj)
            restoredPrefs = true
        }

        var restoredCount = 0
        if (workspaceArray != null) {
            val removedWidgetIds = homeWorkspaceRepository.importBackupItemsJson(workspaceArray)
            removedWidgetIds.forEach { widgetId ->
                appWidgetController.deleteAppWidgetId(widgetId)
            }
            restoredCount = workspaceArray.length()
        }

        return BackupImportResult(
            restoredPreferences = restoredPrefs,
            restoredWorkspaceCount = restoredCount,
        )
    }

    suspend fun exportToUri(
        context: Context,
        uri: Uri,
        settingsRepository: SettingsRepository,
        homeWorkspaceRepository: HomeWorkspaceRepository,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val json = exportBackupJson(context, settingsRepository, homeWorkspaceRepository)
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.bufferedWriter(Charsets.UTF_8).use { writer ->
                    writer.write(json)
                }
            } ?: error("Unable to open backup output stream")
        }
    }

    suspend fun importFromUri(
        context: Context,
        uri: Uri,
        settingsRepository: SettingsRepository,
        homeWorkspaceRepository: HomeWorkspaceRepository,
        appWidgetController: AppWidgetController,
    ): Result<BackupImportResult> = withContext(Dispatchers.IO) {
        runCatching {
            val raw = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).use { reader ->
                    reader.readText()
                }
            } ?: error("Unable to open backup input stream")
            importBackupJson(
                raw = raw,
                settingsRepository = settingsRepository,
                homeWorkspaceRepository = homeWorkspaceRepository,
                appWidgetController = appWidgetController,
            )
        }
    }
}
