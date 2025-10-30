package id.xms.islandx

import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotificationListener : NotificationListenerService() {

    companion object {
        const val ACTION_NOTIFICATION_POSTED = "id.xms.islandx.NOTIFICATION_POSTED"
        const val ACTION_NOTIFICATION_REMOVED = "id.xms.islandx.NOTIFICATION_REMOVED"
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_TITLE = "title"
        const val EXTRA_TEXT = "text"
        const val EXTRA_APP_NAME = "app_name"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        sbn?.let {
            val notification = it.notification
            val extras = notification.extras

            val title = extras.getCharSequence("android.title")?.toString() ?: ""
            val text = extras.getCharSequence("android.text")?.toString() ?: ""
            val appName = it.packageName

            // Send broadcast to accessibility service
            val intent = Intent(ACTION_NOTIFICATION_POSTED).apply {
                putExtra(EXTRA_PACKAGE_NAME, appName)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_TEXT, text)
                putExtra(EXTRA_APP_NAME, getAppName(appName))
                putExtra(EXTRA_NOTIFICATION_ID, it.id)

                // Set package to this app for security
                setPackage(packageName)
            }

            // Send as regular broadcast instead of LocalBroadcastManager
            sendBroadcast(intent)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)

        sbn?.let {
            // Send broadcast untuk notification removed
            val intent = Intent(ACTION_NOTIFICATION_REMOVED).apply {
                putExtra(EXTRA_PACKAGE_NAME, it.packageName)
                putExtra(EXTRA_NOTIFICATION_ID, it.id)
                setPackage(packageName)
            }

            sendBroadcast(intent)
        }
    }

    private fun getAppName(packageName: String): String {
        return try {
            val packageManager = applicationContext.packageManager
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }
}
