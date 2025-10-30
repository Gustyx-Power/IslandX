package id.xms.islandx

import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class NotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "NotificationListener"
        const val ACTION_NOTIFICATION_POSTED = "id.xms.islandx.NOTIFICATION_POSTED"
        const val ACTION_NOTIFICATION_REMOVED = "id.xms.islandx.NOTIFICATION_REMOVED"
        const val ACTION_MUSIC_UPDATED = "id.xms.islandx.MUSIC_UPDATED"
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_TITLE = "title"
        const val EXTRA_TEXT = "text"
        const val EXTRA_APP_NAME = "app_name"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        const val EXTRA_IS_PLAYING = "is_playing"
        const val EXTRA_ARTIST = "artist"

        // Music player package names
        private val MUSIC_PACKAGES = setOf(
            "com.google.android.youtube",
            "com.spotify.music",
            "com.google.android.apps.youtube.music",
            "com.android.music",
            "com.apple.android.music",
            "com.amazon.mp3",
            "deezer.android.app",
            "com.jio.media.jiobeats"
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        Log.d(TAG, "Notification posted from: ${sbn?.packageName}")

        sbn?.let {
            val packageName = it.packageName
            val notification = it.notification
            val extras = notification.extras

            val title = extras.getCharSequence("android.title")?.toString() ?: ""
            val text = extras.getCharSequence("android.text")?.toString() ?: ""

            // Check jika dari music player
            if (MUSIC_PACKAGES.contains(packageName)) {
                Log.d(TAG, "Music player notification: $packageName")

                // Extract music info dari notification
                val subText = extras.getCharSequence("android.subText")?.toString() ?: ""
                val artist = subText.ifEmpty {
                    extras.getCharSequence("android.text")?.toString() ?: "Unknown Artist"
                }

                // Check if playing (dari notification actions)
                val actions = notification.actions
                var isPlaying = false
                if (actions != null) {
                    // Jika ada action "Pause", berarti sedang playing
                    isPlaying = actions.any { action ->
                        action.title?.toString()?.contains("Pause", ignoreCase = true) == true ||
                                action.title?.toString()?.contains("暫停", ignoreCase = true) == true
                    }
                }

                Log.d(TAG, "Music: title=$title, artist=$artist, playing=$isPlaying")

                // Send music broadcast
                val intent = Intent(ACTION_MUSIC_UPDATED).apply {
                    putExtra(EXTRA_PACKAGE_NAME, packageName)
                    putExtra(EXTRA_TITLE, title)
                    putExtra(EXTRA_ARTIST, artist)
                    putExtra(EXTRA_IS_PLAYING, isPlaying)
                    setPackage(application.packageName)
                }

                sendBroadcast(intent)
                Log.d(TAG, "Music broadcast sent")

            } else {
                // Regular notification
                Log.d(TAG, "Regular notification: Title: $title, Text: $text")

                val intent = Intent(ACTION_NOTIFICATION_POSTED).apply {
                    putExtra(EXTRA_PACKAGE_NAME, packageName)
                    putExtra(EXTRA_TITLE, title)
                    putExtra(EXTRA_TEXT, text)
                    putExtra(EXTRA_APP_NAME, getAppName(packageName))
                    putExtra(EXTRA_NOTIFICATION_ID, it.id)
                    setPackage(application.packageName)
                }

                sendBroadcast(intent)
                Log.d(TAG, "Notification broadcast sent")
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)

        sbn?.let {
            val packageName = it.packageName

            // Jika music player notification dihapus, dismiss island
            if (MUSIC_PACKAGES.contains(packageName)) {
                Log.d(TAG, "Music player notification removed")

                val intent = Intent(ACTION_NOTIFICATION_REMOVED).apply {
                    putExtra(EXTRA_PACKAGE_NAME, packageName)
                    putExtra(EXTRA_NOTIFICATION_ID, it.id)
                    setPackage(application.packageName)
                }

                sendBroadcast(intent)
            }
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
