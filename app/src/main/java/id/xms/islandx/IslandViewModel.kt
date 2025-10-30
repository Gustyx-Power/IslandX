package id.xms.islandx

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.xms.islandx.data.IslandState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class IslandViewModel : ViewModel() {

    private val _islandState = mutableStateOf<IslandState>(IslandState.Idle)
    val islandState: State<IslandState> = _islandState

    private val _isExpanded = mutableStateOf(false)
    val isExpanded: State<Boolean> = _isExpanded

    private var autoHideJob: Job? = null
    private var autoHideDelaySeconds: Float = 3f

    /**
     * Update auto-hide delay dari settings
     */
    fun setAutoHideDelay(delaySeconds: Float) {
        autoHideDelaySeconds = delaySeconds
    }

    /**
     * Show notification - TIDAK dismiss music yang sedang playing
     */
    fun showNotification(
        appName: String,
        title: String,
        text: String
    ) {
        // Jika sedang playing music, jangan tampil notifikasi
        if (_islandState.value is IslandState.Music) {
            return  // ← Skip notifikasi saat music playing
        }

        autoHideJob?.cancel()

        _islandState.value = IslandState.Notification(
            appName = appName,
            title = title,
            text = text
        )

        autoHideJob = viewModelScope.launch {
            delay((autoHideDelaySeconds * 1000).toLong())
            dismissIsland()
        }
    }

    /**
     * Show music player - TIDAK auto-hide (tetap sampai musik stop)
     */
    fun showMusic(
        title: String,
        artist: String,
        isPlaying: Boolean
    ) {
        autoHideJob?.cancel()

        _islandState.value = IslandState.Music(
            title = title,
            artist = artist,
            isPlaying = isPlaying
        )

        // Music TIDAK auto-hide - tetap visible sampai musik stop
    }

    /**
     * Stop music dan kembali ke idle
     */
    fun stopMusic() {
        if (_islandState.value is IslandState.Music) {
            autoHideJob?.cancel()
            _islandState.value = IslandState.Idle
            _isExpanded.value = false
        }
    }

    /**
     * Show call - TIDAK auto-hide
     */
    fun showCall(
        callerName: String,
        duration: Long,
        isIncoming: Boolean
    ) {
        autoHideJob?.cancel()

        _islandState.value = IslandState.Call(
            callerName = callerName,
            duration = duration,
            isIncoming = isIncoming
        )
    }

    /**
     * Show timer dengan auto-hide
     */
    fun showTimer(
        remainingTime: Long,
        totalTime: Long
    ) {
        autoHideJob?.cancel()

        _islandState.value = IslandState.Timer(
            remainingTime = remainingTime,
            totalTime = totalTime
        )

        autoHideJob = viewModelScope.launch {
            delay((autoHideDelaySeconds * 1000).toLong())
            dismissIsland()
        }
    }

    /**
     * Show recording indicator - TIDAK auto-hide
     */
    fun showRecording(
        duration: Long,
        isPaused: Boolean
    ) {
        autoHideJob?.cancel()

        _islandState.value = IslandState.Recording(
            duration = duration,
            isPaused = isPaused
        )
    }

    /**
     * Toggle island expansion (click handler)
     */
    fun onIslandClick() {
        _isExpanded.value = !_isExpanded.value

        autoHideJob?.cancel()

        // Extended delay saat expanded
        if (_isExpanded.value) {
            autoHideJob = viewModelScope.launch {
                delay(10000)
                dismissIsland()
            }
        }
    }

    /**
     * Dismiss island dan kembali ke idle state
     */
    fun dismissIsland() {
        autoHideJob?.cancel()
        _islandState.value = IslandState.Idle
        _isExpanded.value = false
    }

    override fun onCleared() {
        super.onCleared()
        autoHideJob?.cancel()
    }
}
