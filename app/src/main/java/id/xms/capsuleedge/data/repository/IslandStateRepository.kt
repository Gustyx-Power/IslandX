package id.xms.capsuleedge.data.repository

import id.xms.capsuleedge.domain.model.IslandEvent
import id.xms.capsuleedge.domain.model.IslandState
import id.xms.capsuleedge.domain.model.IslandUiState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Singleton repository that manages the current Island state.
 * Acts as a bridge between services and UI components.
 */
object IslandStateRepository {
    
    private val _uiState = MutableStateFlow(IslandUiState())
    val uiState: StateFlow<IslandUiState> = _uiState.asStateFlow()
    
    private val _eventQueue = MutableStateFlow<List<IslandEvent>>(emptyList())
    val eventQueue: StateFlow<List<IslandEvent>> = _eventQueue.asStateFlow()
    
    // Coroutine scope for media pause timeout
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var mediaPauseTimeoutJob: Job? = null
    
    /**
     * Push a new event to be displayed in the Island
     */
    fun pushEvent(event: IslandEvent) {
        _eventQueue.update { currentQueue ->
            // Add event and sort by priority (higher priority first)
            (currentQueue + event)
                .distinctBy { it::class.simpleName + it.timestamp }
                .sortedByDescending { it.priority }
                .take(10) // Keep max 10 events in queue
        }
        
        // If this is highest priority or current is idle, display immediately
        val currentEvent = _uiState.value.currentEvent
        if (currentEvent is IslandEvent.Idle || event.priority >= currentEvent.priority) {
            displayEvent(event)
        }
    }
    
    /**
     * Display a specific event in the Island
     * Notifications auto-expand to show full content
     */
    fun displayEvent(event: IslandEvent) {
        _uiState.update { currentState ->
            val displayState = when {
                event is IslandEvent.Idle -> IslandState.COLLAPSED
                // Auto-expand notifications so user can see full message and interact
                event is IslandEvent.Notification -> IslandState.EXPANDED
                else -> IslandState.COMPACT
            }
            
            currentState.copy(
                currentEvent = event,
                displayState = displayState
            )
        }
    }
    
    /**
     * Expand the Island to show full details
     */
    fun expandIsland() {
        _uiState.update { currentState ->
            if (currentState.currentEvent !is IslandEvent.Idle) {
                currentState.copy(displayState = IslandState.EXPANDED)
            } else {
                currentState
            }
        }
    }
    
    /**
     * Collapse the Island back to compact or collapsed state
     */
    fun collapseIsland() {
        _uiState.update { currentState ->
            val newState = if (currentState.currentEvent is IslandEvent.Idle) {
                IslandState.COLLAPSED
            } else {
                IslandState.COLLAPSED // Go to collapsed, not compact
            }
            currentState.copy(displayState = newState)
        }
    }
    
    /**
     * Expand to COMPACT state (slightly larger, for brief animations)
     */
    fun expandToCompact() {
        _uiState.update { currentState ->
            if (currentState.currentEvent !is IslandEvent.Idle) {
                currentState.copy(displayState = IslandState.COMPACT)
            } else {
                currentState
            }
        }
    }
    
    /**
     * Dismiss current event and show next in queue or idle
     */
    fun dismissCurrentEvent() {
        _eventQueue.update { queue ->
            if (queue.isNotEmpty()) queue.drop(1) else queue
        }
        
        val nextEvent = _eventQueue.value.firstOrNull() ?: IslandEvent.Idle
        displayEvent(nextEvent)
    }
    
    /**
     * Clear all events and return to idle
     */
    fun clearAllEvents() {
        _eventQueue.update { emptyList() }
        displayEvent(IslandEvent.Idle)
    }
    
    /**
     * Update service running state
     */
    fun setServiceRunning(running: Boolean) {
        _uiState.update { it.copy(isServiceRunning = running) }
    }
    
    /**
     * Update overlay enabled state
     */
    fun setOverlayEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isOverlayEnabled = enabled) }
    }
    
    /**
     * Update Island configuration
     */
    fun updateConfig(config: id.xms.capsuleedge.domain.model.IslandConfig) {
        _uiState.update { it.copy(config = config) }
    }
    
    /**
     * Update media playback position (for seek bar)
     */
    fun updateMediaPosition(position: Long) {
        _uiState.update { currentState ->
            val event = currentState.currentEvent
            if (event is IslandEvent.MediaPlayback) {
                currentState.copy(
                    currentEvent = event.copy(position = position)
                )
            } else {
                currentState
            }
        }
    }
    
    /**
     * Update media playback state
     * When paused, starts 1 minute timeout to auto-dismiss
     */
    fun updateMediaPlayState(isPlaying: Boolean) {
        val currentEvent = _uiState.value.currentEvent
        
        _uiState.update { currentState ->
            val event = currentState.currentEvent
            if (event is IslandEvent.MediaPlayback) {
                currentState.copy(
                    currentEvent = event.copy(isPlaying = isPlaying)
                )
            } else {
                currentState
            }
        }
        
        // Handle pause timeout
        if (currentEvent is IslandEvent.MediaPlayback) {
            if (isPlaying) {
                // Music resumed, cancel any pause timeout
                cancelMediaPauseTimeout()
            } else {
                // Music paused, start 1 minute timeout to dismiss
                startMediaPauseTimeout(currentEvent.packageName)
            }
        }
    }
    
    /**
     * Start timeout to dismiss media after 1 minute of pause
     */
    private fun startMediaPauseTimeout(packageName: String) {
        cancelMediaPauseTimeout() // Cancel any existing timeout
        
        mediaPauseTimeoutJob = scope.launch {
            delay(60_000) // Wait 1 minute (60 seconds)
            
            val currentEvent = _uiState.value.currentEvent
            if (currentEvent is IslandEvent.MediaPlayback && 
                currentEvent.packageName == packageName &&
                !currentEvent.isPlaying) {
                // Still paused after 1 minute, dismiss the media event
                dismissCurrentEvent()
            }
        }
    }
    
    /**
     * Cancel media pause timeout
     */
    private fun cancelMediaPauseTimeout() {
        mediaPauseTimeoutJob?.cancel()
        mediaPauseTimeoutJob = null
    }
}
