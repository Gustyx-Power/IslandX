package id.xms.islandx.data

import androidx.compose.ui.graphics.Color

sealed class IslandState {
    object Idle : IslandState()

    data class Music(
        val title: String,
        val artist: String,
        val isPlaying: Boolean,
        val albumArt: Any? = null
    ) : IslandState()

    data class Call(
        val callerName: String,
        val duration: Long,
        val isIncoming: Boolean
    ) : IslandState()

    data class Notification(
        val appName: String,
        val title: String,
        val text: String,
        val icon: Any? = null,
        val color: Color = Color.White
    ) : IslandState()

    data class Timer(
        val remainingTime: Long,
        val totalTime: Long
    ) : IslandState()

    data class Recording(
        val duration: Long,
        val isPaused: Boolean
    ) : IslandState()
}

data class NotificationData(
    val id: Int,
    val packageName: String,
    val appName: String,
    val title: String?,
    val text: String?,
    val time: Long,
    val priority: Int
)

enum class IslandSize {
    COMPACT,    // Idle state
    EXPANDED,   // Normal notification
    LARGE       // Interactive content
}
