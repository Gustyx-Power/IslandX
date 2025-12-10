package id.xms.capsuleedge.domain.model

import android.graphics.Bitmap
import android.graphics.drawable.Drawable

/**
 * Represents all possible events that can be displayed in the Dynamic Island.
 * Each event type contains relevant data for its display.
 */
sealed class IslandEvent {
    
    abstract val priority: Int
    abstract val timestamp: Long
    
    /**
     * No active event - Island shows in collapsed/default state
     */
    data object Idle : IslandEvent() {
        override val priority: Int = 0
        override val timestamp: Long = 0L
    }
    
    /**
     * Notification event from any app
     */
    data class Notification(
        val packageName: String,
        val appName: String,
        val title: String,
        val text: String,
        val appIcon: Drawable?,
        val largeIcon: Bitmap? = null,
        override val timestamp: Long = System.currentTimeMillis(),
        override val priority: Int = 5
    ) : IslandEvent()
    
    /**
     * Media playback event (Spotify, YouTube Music, etc.)
     */
    data class MediaPlayback(
        val packageName: String,
        val title: String,
        val artist: String,
        val albumArt: Bitmap?,
        val isPlaying: Boolean,
        val duration: Long = 0L,
        val position: Long = 0L,
        override val timestamp: Long = System.currentTimeMillis(),
        override val priority: Int = 8
    ) : IslandEvent()
    
    /**
     * Charging/Battery event
     */
    data class Charging(
        val batteryLevel: Int,
        val isCharging: Boolean,
        val isFastCharging: Boolean = false,
        override val timestamp: Long = System.currentTimeMillis(),
        override val priority: Int = 7
    ) : IslandEvent()
    
    /**
     * Bluetooth connection event
     */
    data class BluetoothConnection(
        val deviceName: String,
        val deviceType: BluetoothDeviceType,
        val isConnected: Boolean,
        val batteryLevel: Int? = null,
        override val timestamp: Long = System.currentTimeMillis(),
        override val priority: Int = 6
    ) : IslandEvent()
    
    /**
     * Ringer/Sound mode change event
     */
    data class RingerMode(
        val mode: SoundMode,
        override val timestamp: Long = System.currentTimeMillis(),
        override val priority: Int = 9
    ) : IslandEvent()
}

enum class BluetoothDeviceType {
    HEADPHONES,
    EARBUDS,
    SPEAKER,
    WATCH,
    OTHER
}

enum class SoundMode {
    NORMAL,
    VIBRATE,
    SILENT
}
