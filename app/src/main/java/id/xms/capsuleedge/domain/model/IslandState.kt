package id.xms.capsuleedge.domain.model

/**
 * Represents the current visual state of the Dynamic Island.
 */
enum class IslandState {
    /**
     * Default collapsed pill shape - minimal size
     */
    COLLAPSED,
    
    /**
     * Compact expanded state - shows preview info
     */
    COMPACT,
    
    /**
     * Fully expanded state - shows complete details with controls
     */
    EXPANDED
}

/**
 * Configuration for the Island position and size
 */
data class IslandConfig(
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val scale: Float = 1f,
    val collapsedWidth: Float = 120f,
    val collapsedHeight: Float = 36f,
    val compactWidth: Float = 240f,
    val compactHeight: Float = 56f,
    val expandedWidth: Float = 380f,  // Nearly screen width
    val expandedHeight: Float = 220f, // Taller for content
    val cornerRadius: Float = 24f
)

/**
 * Combined state for the Dynamic Island overlay
 */
data class IslandUiState(
    val currentEvent: IslandEvent = IslandEvent.Idle,
    val displayState: IslandState = IslandState.COLLAPSED,
    val config: IslandConfig = IslandConfig(),
    val isServiceRunning: Boolean = false,
    val isOverlayEnabled: Boolean = true
)
