package id.xms.capsuleedge.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import id.xms.capsuleedge.CapsuleEdgeApplication.Companion.NOTIFICATION_CHANNEL_ID
import id.xms.capsuleedge.CapsuleEdgeApplication.Companion.NOTIFICATION_ID
import id.xms.capsuleedge.R
import id.xms.capsuleedge.data.repository.IslandStateRepository
import id.xms.capsuleedge.presentation.MainActivity

/**
 * Foreground Service for keeping CapsuleEdge alive.
 * The actual overlay is now handled by CapsuleAccessibilityService
 * using TYPE_ACCESSIBILITY_OVERLAY for proper Z-ordering above status bar.
 */
class OverlayService : Service() {
    
    companion object {
        private const val ACTION_START = "id.xms.capsuleedge.action.START"
        private const val ACTION_STOP = "id.xms.capsuleedge.action.STOP"
        
        @Volatile
        private var instance: OverlayService? = null
        
        fun isRunning(): Boolean = instance != null
        
        fun start(context: Context) {
            val intent = Intent(context, OverlayService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stop(context: Context) {
            val intent = Intent(context, OverlayService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                // Tell AccessibilityService to hide overlay
                CapsuleAccessibilityService.stopOverlay()
                IslandStateRepository.setServiceRunning(false)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START, null -> {
                startForeground(NOTIFICATION_ID, createNotification())
                // Tell AccessibilityService to show overlay
                CapsuleAccessibilityService.startOverlay()
                IslandStateRepository.setServiceRunning(true)
            }
        }
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        instance = null
        CapsuleAccessibilityService.stopOverlay()
        IslandStateRepository.setServiceRunning(false)
    }
    
    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
