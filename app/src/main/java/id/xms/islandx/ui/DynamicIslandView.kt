package id.xms.islandx.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.xms.islandx.data.IslandSettings
import id.xms.islandx.data.IslandSize
import id.xms.islandx.data.IslandState
import id.xms.islandx.ui.theme.*
import id.xms.islandx.utils.CutoutInfo
import id.xms.islandx.utils.DisplayCutoutHelper
import java.util.Locale

/**
 * Main Dynamic Island Overlay Composable
 * Displays interactive island with adaptive size based on content
 */
@Composable
fun DynamicIslandOverlay(
    state: IslandState,
    settings: IslandSettings,
    cutoutInfo: CutoutInfo,
    onIslandClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    val targetSize = when (state) {
        is IslandState.Idle -> IslandSize.COMPACT
        is IslandState.Music, is IslandState.Call -> IslandSize.LARGE
        else -> IslandSize.EXPANDED
    }

    // Calculate optimal width based on cutout
    val maxWidth = with(density) {
        DisplayCutoutHelper.calculateOptimalWidth(
            cutoutInfo = cutoutInfo,
            requestedWidth = settings.width,
            horizontalMargin = settings.horizontalMargin
        ).toDp()
    }

    // Animated width based on island state
    val animatedWidth by animateDpAsState(
        targetValue = when (targetSize) {
            IslandSize.COMPACT -> minOf(120.dp, maxWidth)
            IslandSize.EXPANDED -> minOf(280.dp, maxWidth)
            IslandSize.LARGE -> minOf(maxWidth, 360.dp)
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "island_width_animation"
    )

    val baseHeight = settings.height.dp

    // Animated height based on island state
    val animatedHeight by animateDpAsState(
        targetValue = when (targetSize) {
            IslandSize.COMPACT -> baseHeight
            IslandSize.EXPANDED -> baseHeight * 1.67f
            IslandSize.LARGE -> baseHeight * 2.5f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "island_height_animation"
    )

    // Shape based on size
    val shape = when (targetSize) {
        IslandSize.COMPACT -> IslandCompactShape
        IslandSize.EXPANDED -> IslandExpandedShape
        IslandSize.LARGE -> IslandLargeShape
    }

    // Check if island will overlap with cutout
    val isOverlapping = with(density) {
        DisplayCutoutHelper.willOverlapCutout(
            cutoutInfo = cutoutInfo,
            islandY = settings.yOffset.dp.toPx().toInt(),
            islandHeight = animatedHeight.toPx().toInt(),
            islandX = (cutoutInfo.screenWidth / 2 - animatedWidth.toPx().toInt() / 2),
            islandWidth = animatedWidth.toPx().toInt()
        )
    }

    // Main island container
    Box(
        modifier = modifier
            .width(animatedWidth)
            .height(animatedHeight)
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        if (isOverlapping) ErrorRed40.copy(alpha = 0.3f)
                        else IslandBackground.copy(alpha = 0.95f),
                        if (isOverlapping) ErrorRed40.copy(alpha = 0.4f)
                        else IslandBackground.copy(alpha = 0.98f)
                    )
                )
            )
            .clickable(
                onClick = onIslandClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Animated content based on state
        AnimatedContent(
            targetState = state,
            label = "island_content_animation"
        ) { currentState ->
            when (currentState) {
                is IslandState.Idle -> IdleContent()
                is IslandState.Music -> MusicContent(currentState)
                is IslandState.Call -> CallContent(currentState)
                is IslandState.Notification -> NotificationContent(currentState)
                is IslandState.Timer -> TimerContent(currentState)
                is IslandState.Recording -> RecordingContent(currentState)
            }
        }
    }
}

/**
 * Idle state content - pulsing indicator
 */
@Composable
private fun IdleContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        var scale by remember { mutableFloatStateOf(1f) }

        LaunchedEffect(Unit) {
            while (true) {
                animate(
                    initialValue = 1f,
                    targetValue = 1.1f,
                    animationSpec = tween(durationMillis = 1000)
                ) { value, _ ->
                    scale = value
                }
                animate(
                    initialValue = 1.1f,
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 1000)
                ) { value, _ ->
                    scale = value
                }
            }
        }

        Box(
            modifier = Modifier
                .size(12.dp)
                .scale(scale)
                .background(IslandAccent, shape = MaterialTheme.shapes.extraSmall)
        )
    }
}

/**
 * Music player content with controls
 */
@Composable
private fun MusicContent(state: IslandState.Music) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Song info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = state.title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = state.artist,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Playback controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { /* Handle previous */ },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    tint = IslandAccent
                )
            }

            IconButton(
                onClick = { /* Handle play/pause */ },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Default.Pause
                    else Icons.Default.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    tint = IslandAccent
                )
            }

            IconButton(
                onClick = { /* Handle next */ },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = IslandAccent
                )
            }
        }
    }
}

/**
 * Phone call content with controls
 */
@Composable
private fun CallContent(state: IslandState.Call) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Caller info
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = if (state.isIncoming) Icons.Default.PhonelinkRing
                else Icons.Default.Phone,
                contentDescription = null,
                tint = if (state.isIncoming) ExpressiveTeal80
                else ExpressivePurple80,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = state.callerName,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatDuration(state.duration),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        // Call controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // End call button
            IconButton(
                onClick = { /* Handle end call */ },
                modifier = Modifier
                    .size(40.dp)
                    .background(ErrorRed40, shape = MaterialTheme.shapes.extraLarge)
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "End Call",
                    tint = Color.White
                )
            }

            // Answer button (only for incoming calls)
            if (state.isIncoming) {
                IconButton(
                    onClick = { /* Handle answer call */ },
                    modifier = Modifier
                        .size(40.dp)
                        .background(ExpressiveTeal40, shape = MaterialTheme.shapes.extraLarge)
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Answer",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Notification content
 */
@Composable
private fun NotificationContent(state: IslandState.Notification) {
    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // App icon placeholder
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    state.color.copy(alpha = 0.2f),
                    shape = MaterialTheme.shapes.small
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = state.color,
                modifier = Modifier.size(20.dp)
            )
        }

        // Notification text
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = state.appName,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = state.title,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Timer content with circular progress
 */
@Composable
private fun TimerContent(state: IslandState.Timer) {
    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Circular progress indicator
        CircularProgressIndicator(
            progress = { state.remainingTime.toFloat() / state.totalTime.toFloat() },
            modifier = Modifier.size(32.dp),
            color = ExpressiveOrange80,
            strokeWidth = 3.dp,
        )

        // Timer info
        Column {
            Text(
                text = "Timer",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f)
            )
            Text(
                text = formatDuration(state.remainingTime),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
        }
    }
}

/**
 * Recording indicator content
 */
@Composable
private fun RecordingContent(state: IslandState.Recording) {
    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        var alpha by remember { mutableFloatStateOf(1f) }

        // Pulsing recording indicator
        LaunchedEffect(state.isPaused) {
            if (!state.isPaused) {
                while (true) {
                    animate(
                        initialValue = 1f,
                        targetValue = 0.3f,
                        animationSpec = tween(durationMillis = 500)
                    ) { value, _ ->
                        alpha = value
                    }
                    animate(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 500)
                    ) { value, _ ->
                        alpha = value
                    }
                }
            } else {
                alpha = 1f
            }
        }

        // Recording dot
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(
                    ErrorRed40.copy(alpha = alpha),
                    shape = MaterialTheme.shapes.extraSmall
                )
        )

        // Recording info
        Column {
            Text(
                text = "Recording",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f)
            )
            Text(
                text = formatDuration(state.duration),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Play/Pause indicator
        Icon(
            imageVector = if (state.isPaused) Icons.Default.PlayArrow
            else Icons.Default.Pause,
            contentDescription = null,
            tint = IslandAccent
        )
    }
}

/**
 * Format duration in milliseconds to HH:MM:SS or MM:SS
 */
private fun formatDuration(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    val hours = (millis / (1000 * 60 * 60))

    return if (hours > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}
