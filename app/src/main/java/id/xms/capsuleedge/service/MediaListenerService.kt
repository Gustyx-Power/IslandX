package id.xms.capsuleedge.service

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import id.xms.capsuleedge.R
import id.xms.capsuleedge.data.repository.IslandStateRepository
import id.xms.capsuleedge.domain.model.IslandEvent

/**
 * NotificationListenerService for listening to media sessions.
 * This provides proper access to MediaSessionManager.getActiveSessions()
 */
class MediaListenerService : NotificationListenerService() {
    
    private var mediaSessionManager: MediaSessionManager? = null
    private val activeControllers = mutableMapOf<String, MediaController>()
    private val mediaCallbacks = mutableMapOf<String, MediaController.Callback>()
    
    companion object {
        private const val TAG = "MediaListenerService"
        
        @Volatile
        private var instance: MediaListenerService? = null
        
        fun getInstance(): MediaListenerService? = instance
        
        fun isServiceEnabled(context: Context): Boolean {
            val componentName = ComponentName(context, MediaListenerService::class.java)
            val enabledListeners = android.provider.Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false
            return enabledListeners.contains(componentName.flattenToString())
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d(TAG, "MediaListenerService created")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        instance = null
        cleanupMediaSession()
        Log.d(TAG, "MediaListenerService destroyed")
    }
    
    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "NotificationListener connected - setting up media session")
        setupMediaSessionListener()
    }
    
    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        cleanupMediaSession()
    }
    
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // We handle notifications in AccessibilityService
        // This is just for media session access
    }
    
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Not needed
    }
    
    private fun setupMediaSessionListener() {
        try {
            mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            
            val componentName = ComponentName(this, MediaListenerService::class.java)
            
            // Add listener for session changes
            mediaSessionManager?.addOnActiveSessionsChangedListener({ controllers ->
                Log.d(TAG, "Active sessions changed: ${controllers?.size ?: 0} controllers")
                updateMediaControllers(controllers ?: emptyList())
            }, componentName)
            
            // Get current active sessions immediately
            try {
                val controllers = mediaSessionManager?.getActiveSessions(componentName) ?: emptyList()
                Log.d(TAG, "Initial active sessions: ${controllers.size}")
                updateMediaControllers(controllers)
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException getting active sessions", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up media session listener", e)
        }
    }
    
    private fun updateMediaControllers(controllers: List<MediaController>) {
        // Unregister old callbacks
        activeControllers.forEach { (pkg, controller) ->
            mediaCallbacks[pkg]?.let { callback ->
                try {
                    controller.unregisterCallback(callback)
                } catch (e: Exception) { /* ignore */ }
            }
        }
        activeControllers.clear()
        mediaCallbacks.clear()
        
        // Register new controllers
        controllers.forEach { controller ->
            val packageName = controller.packageName
            Log.d(TAG, "Registering controller for: $packageName")
            activeControllers[packageName] = controller
            
            val callback = object : MediaController.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackState?) {
                    Log.d(TAG, "Playback state changed for $packageName: ${state?.state}")
                    updateMediaState(controller)
                }
                
                override fun onMetadataChanged(metadata: MediaMetadata?) {
                    Log.d(TAG, "Metadata changed for $packageName")
                    updateMediaState(controller)
                }
            }
            
            mediaCallbacks[packageName] = callback
            controller.registerCallback(callback)
            
            // Check current state immediately
            updateMediaState(controller)
        }
    }
    
    private fun updateMediaState(controller: MediaController) {
        val metadata = controller.metadata
        val playbackState = controller.playbackState
        
        if (metadata == null || playbackState == null) {
            Log.d(TAG, "No metadata or playback state for ${controller.packageName}")
            return
        }
        
        val isPlaying = playbackState.state == PlaybackState.STATE_PLAYING
        Log.d(TAG, "Media state: ${controller.packageName}, playing=$isPlaying")
        
        // Update play state if we have an existing media event
        val currentEvent = IslandStateRepository.uiState.value.currentEvent
        if (!isPlaying) {
            if (currentEvent is IslandEvent.MediaPlayback && 
                currentEvent.packageName == controller.packageName) {
                IslandStateRepository.updateMediaPlayState(false)
            }
            return
        }
        
        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) 
            ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE) 
            ?: getString(R.string.unknown_title)
            
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: getString(R.string.unknown_artist)
            
        val albumArt: Bitmap? = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
            
        val duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)
        val position = playbackState.position
        
        Log.d(TAG, "Creating media event: $title by $artist")
        
        val mediaEvent = IslandEvent.MediaPlayback(
            packageName = controller.packageName,
            title = title,
            artist = artist,
            albumArt = albumArt,
            isPlaying = isPlaying,
            duration = duration,
            position = position
        )
        
        // Push to repository - UI reacts automatically
        IslandStateRepository.pushEvent(mediaEvent)
    }
    
    private fun cleanupMediaSession() {
        activeControllers.forEach { (pkg, controller) ->
            mediaCallbacks[pkg]?.let { callback ->
                try {
                    controller.unregisterCallback(callback)
                } catch (e: Exception) { /* ignore */ }
            }
        }
        activeControllers.clear()
        mediaCallbacks.clear()
    }
}
