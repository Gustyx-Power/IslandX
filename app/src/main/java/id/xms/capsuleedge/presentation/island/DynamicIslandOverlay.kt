package id.xms.capsuleedge.presentation.island

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import id.xms.capsuleedge.domain.model.IslandEvent
import id.xms.capsuleedge.domain.model.IslandState
import id.xms.capsuleedge.domain.model.IslandUiState
import id.xms.capsuleedge.service.MediaAction

/**
 * Main Dynamic Island overlay composable.
 * Handles all state transitions with spring-based physics animations.
 * 
 * This composable renders just the island itself - positioning is handled
 * by the WindowManager in CapsuleAccessibilityService.
 */
@Composable
fun DynamicIslandOverlay(
    uiState: IslandUiState,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onDismiss: () -> Unit,
    onMediaAction: (MediaAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val config = uiState.config
    
    // Get screen width for dynamic expanded width calculation
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.toFloat()
    // Expanded width = screen width - minimal margin (4dp total, 2dp per side)
    // This makes the island reach almost to the screen edges
    val dynamicExpandedWidth = (screenWidthDp - 4f).coerceAtLeast(280f)
    
    // Spring animation spec for organic, bouncy feel
    val springSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
    
    // Calculate target dimensions based on state
    val targetWidth = when (uiState.displayState) {
        IslandState.COLLAPSED -> config.collapsedWidth
        IslandState.COMPACT -> config.compactWidth
        IslandState.EXPANDED -> dynamicExpandedWidth  // Use dynamic screen-based width
    }
    
    val targetHeight = when (uiState.displayState) {
        IslandState.COLLAPSED -> config.collapsedHeight
        IslandState.COMPACT -> config.compactHeight
        IslandState.EXPANDED -> config.expandedHeight
    }
    
    // Animated dimensions with spring physics
    val animatedWidth by animateFloatAsState(
        targetValue = targetWidth * config.scale,
        animationSpec = springSpec,
        label = "island_width"
    )
    
    val animatedHeight by animateFloatAsState(
        targetValue = targetHeight * config.scale,
        animationSpec = springSpec,
        label = "island_height"
    )
    
    // Animated corner radius - more round when collapsed, less when expanded
    val cornerRadius by animateFloatAsState(
        targetValue = when (uiState.displayState) {
            IslandState.COLLAPSED -> config.cornerRadius
            IslandState.COMPACT -> config.cornerRadius * 0.9f
            IslandState.EXPANDED -> config.cornerRadius * 0.8f
        },
        animationSpec = springSpec,
        label = "corner_radius"
    )
    
    // Animated shadow elevation
    val elevation by animateFloatAsState(
        targetValue = when (uiState.displayState) {
            IslandState.COLLAPSED -> 4f
            IslandState.COMPACT -> 8f
            IslandState.EXPANDED -> 16f
        },
        animationSpec = tween(300),
        label = "elevation"
    )
    
    // Main Island Container - just the island, no extra layout
    // Positioning is handled by WindowManager params
    Box(
        modifier = modifier
            .size(
                width = animatedWidth.dp,
                height = animatedHeight.dp
            )
            .shadow(
                elevation = elevation.dp,
                shape = RoundedCornerShape(cornerRadius.dp),
                spotColor = Color.Black.copy(alpha = 0.5f)
            )
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onLongPress = { onLongPress() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Content based on current event and state
        IslandContent(
            event = uiState.currentEvent,
            displayState = uiState.displayState,
            onDismiss = onDismiss,
            onMediaAction = onMediaAction
        )
    }
}

/**
 * Content displayed inside the Island based on event type
 */
@Composable
private fun IslandContent(
    event: IslandEvent,
    displayState: IslandState,
    onDismiss: () -> Unit,
    onMediaAction: (MediaAction) -> Unit
) {
    when (event) {
        is IslandEvent.Idle -> {
            // Show minimal camera dot indicator
            CameraIndicator()
        }
        is IslandEvent.Notification -> {
            NotificationContent(
                event = event,
                isExpanded = displayState == IslandState.EXPANDED,
                onDismiss = onDismiss
            )
        }
        is IslandEvent.MediaPlayback -> {
            MediaContent(
                event = event,
                isExpanded = displayState == IslandState.EXPANDED,
                onMediaAction = onMediaAction
            )
        }
        is IslandEvent.Charging -> {
            ChargingContent(
                event = event,
                isExpanded = displayState == IslandState.EXPANDED
            )
        }
        is IslandEvent.BluetoothConnection -> {
            BluetoothContent(
                event = event,
                isExpanded = displayState == IslandState.EXPANDED
            )
        }
        is IslandEvent.RingerMode -> {
            RingerModeContent(
                event = event,
                isExpanded = displayState == IslandState.EXPANDED
            )
        }
    }
}

/**
 * Camera cutout indicator shown when island is idle/collapsed
 */
@Composable
private fun CameraIndicator() {
    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF1A1A1A)),
        contentAlignment = Alignment.Center
    ) {
        // Subtle camera lens effect
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFF0D0D0D))
        )
    }
}
