package id.xms.islandx

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.xms.islandx.data.IslandState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class IslandViewModel : ViewModel() {

    private val _islandState = mutableStateOf<IslandState>(IslandState.Idle)
    val islandState: State<IslandState> = _islandState

    private val _isExpanded = mutableStateOf(false)
    val isExpanded: State<Boolean> = _isExpanded

    fun showNotification(
        appName: String,
        title: String,
        text: String
    ) {
        _islandState.value = IslandState.Notification(
            appName = appName,
            title = title,
            text = text
        )

        // Auto dismiss after 3 seconds
        viewModelScope.launch {
            delay(3000)
            dismissIsland()
        }
    }

    fun showMusic(
        title: String,
        artist: String,
        isPlaying: Boolean
    ) {
        _islandState.value = IslandState.Music(
            title = title,
            artist = artist,
            isPlaying = isPlaying
        )
    }

    fun showCall(
        callerName: String,
        duration: Long,
        isIncoming: Boolean
    ) {
        _islandState.value = IslandState.Call(
            callerName = callerName,
            duration = duration,
            isIncoming = isIncoming
        )
    }

    fun onIslandClick() {
        _isExpanded.value = !_isExpanded.value
    }

    fun dismissIsland() {
        _islandState.value = IslandState.Idle
        _isExpanded.value = false
    }
}
