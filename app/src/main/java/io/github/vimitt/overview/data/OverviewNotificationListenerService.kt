package io.github.vimitt.overview.data

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Mantém o conjunto em memória dos pacotes com notificações ativas elegíveis para badge.
 */
class NotificationBadgeRepository(
    private val context: Context,
) {
    val activeBadgePackages: StateFlow<Set<String>> = NotificationBadgeStore.packages

    fun hasNotificationAccess(): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        ).orEmpty()
        val expectedComponent = ComponentName(
            context,
            OverviewNotificationListenerService::class.java,
        ).flattenToString()
        val shortComponent = ComponentName(
            context,
            OverviewNotificationListenerService::class.java,
        ).flattenToShortString()
        return enabled.split(':').any { entry ->
            entry == expectedComponent || entry == shortComponent
        }
    }

    fun createAccessSettingsIntent(): Intent {
        val component = ComponentName(context, OverviewNotificationListenerService::class.java)
        return Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).apply {
            putExtra(
                Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                component.flattenToString(),
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}

private object NotificationBadgeStore {
    private val _packages = MutableStateFlow<Set<String>>(emptySet())
    val packages: StateFlow<Set<String>> = _packages.asStateFlow()

    fun updateFromStatusBarNotifications(notifications: Array<out StatusBarNotification>?) {
        if (notifications == null) {
            _packages.value = emptySet()
            return
        }
        val active = HashSet<String>()
        for (sbn in notifications) {
            val notification = sbn.notification ?: continue
            val isOngoing = (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0
            val isGroupSummary = (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
            if (!isOngoing && !isGroupSummary) {
                active.add(sbn.packageName)
            }
        }
        _packages.value = active
    }

    fun clear() {
        _packages.value = emptySet()
    }
}

/**
 * Serviço opcional de escuta de notificações usado quando o usuário habilita
 * "Pontos de notificação" nas configurações de Aparência.
 */
class OverviewNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        NotificationBadgeStore.updateFromStatusBarNotifications(activeNotifications)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        NotificationBadgeStore.updateFromStatusBarNotifications(activeNotifications)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        NotificationBadgeStore.updateFromStatusBarNotifications(activeNotifications)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        NotificationBadgeStore.clear()
    }
}
