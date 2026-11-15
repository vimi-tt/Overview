package io.github.vimitt.overview.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import android.util.Log

/**
 * Utilitário seguro para acionar ações de sistema a partir dos gestos configurados.
 */
object SystemPanelHelper {
    private const val TAG = "SystemPanelHelper"

    @SuppressLint("WrongConstant", "PrivateApi")
    fun expandNotificationsPanel(context: Context): Boolean {
        return invokeStatusBarMethod(context, "expandNotificationsPanel")
    }

    @SuppressLint("WrongConstant", "PrivateApi")
    fun expandQuickSettingsPanel(context: Context): Boolean {
        return invokeStatusBarMethod(context, "expandSettingsPanel")
    }

    private fun invokeStatusBarMethod(context: Context, methodName: String): Boolean {
        return try {
            val statusBarService = context.getSystemService("statusbar") ?: return false
            val statusBarClass = Class.forName("android.app.StatusBarManager")
            val method = statusBarClass.getMethod(methodName)
            method.invoke(statusBarService)
            true
        } catch (exception: Exception) {
            Log.w(TAG, "Falha ao executar $methodName", exception)
            false
        }
    }

    fun openAssistant(context: Context) {
        val assistIntent = Intent(Intent.ACTION_ASSIST).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val voiceFallback = Intent(Intent.ACTION_VOICE_COMMAND).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(assistIntent) }
            .recoverCatching { context.startActivity(voiceFallback) }
    }

    fun openCamera(context: Context) {
        val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }
}
