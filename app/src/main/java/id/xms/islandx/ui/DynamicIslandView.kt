package id.xms.islandx.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.xms.islandx.data.IslandSettings
import id.xms.islandx.data.IslandSize
import id.xms.islandx.data.IslandState
import id.xms.islandx.ui.theme.*
import id.xms.islandx.utils.CutoutInfo
import id.xms.islandx.utils.DisplayCutoutHelper
import java.util.Locale
import kotlin.random.Random
import kotlinx.coroutines.delay

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

    // Music punya state tersendiri untuk expand on button click
    var isMusicExpanded by remember { mutableStateOf(false) }
    var musicExpandTimer by remember { mutableStateOf(false) }

    // Reset music expanded setelah 3 detik
    LaunchedEffect(musicExpandTimer) {
        if (musicExpandTimer) {
            delay(3000)
            isMusicExpanded = false
            musicExpandTimer = false
        }
    }

    val targetSize = remember(state, isMusicExpanded) {
        when {
            state is IslandState.Music && isMusicExpanded -> IslandSize.EXPANDED // Music expanded saat button click
            state is IslandState.Music -> IslandSize.COMPACT // Music tetap compact
            state is IslandState.Idle -> IslandSize.COMPACT
            state is IslandState.Call -> IslandSize.LARGE
            state is IslandState.Notification || state is IslandState.Timer || state is IslandState.Recording -> IslandSize.EXPANDED
            else -> IslandSize.COMPACT
        }
    }

    val baseWidth = settings.width.dp
    val baseHeight = settings.height.dp

    val maxWidth = with(density) {
        DisplayCutoutHelper.calculateOptimalWidth(
            cutoutInfo = cutoutInfo,
            requestedWidth = settings.width,
            horizontalMargin = settings.horizontalMargin
        ).toDp()
    }

    val targetWidth: Dp = when (targetSize) {
        IslandSize.COMPACT -> baseWidth
        IslandSize.EXPANDED -> baseWidth * 0.78f
        IslandSize.LARGE -> baseWidth
    }

    val targetHeight: Dp = when (targetSize) {
        IslandSize.COMPACT -> baseHeight
        IslandSize.EXPANDED -> baseHeight * 1.67f
        IslandSize.LARGE -> baseHeight * 2.5f
    }

    val animatedWidth by animateDpAsState(
        targetValue = minOf(targetWidth, maxWidth),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "island_width_animation"
    )

    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "island_height_animation"
    )

    val shape = when (targetSize) {
        IslandSize.COMPACT -> IslandCompactShape
        IslandSize.EXPANDED -> IslandExpandedShape
        IslandSize.LARGE -> IslandLargeShape
    }

    val isOverlapping = with(density) {
        DisplayCutoutHelper.willOverlapCutout(
            cutoutInfo = cutoutInfo,
            islandY = settings.yOffset.dp.toPx().toInt(),
            islandHeight = animatedHeight.toPx().toInt(),
            islandX = (cutoutInfo.screenWidth / 2 - animatedWidth.toPx().toInt() / 2),
            islandWidth = animatedWidth.toPx().toInt()
        )
    }

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
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = state,
            label = "island_content_animation"
        ) { currentState ->
            when {
                currentState is IslandState.Idle -> IdleContent()
                currentState is IslandState.Music -> {
                    if (isMusicExpanded) {
                        MusicContentExpanded(
                            currentState,
                            onButtonClick = { musicExpandTimer = true }
                        )
                    } else {
                        MusicContentCompact(
                            currentState,
                            onButtonClick = {
                                isMusicExpanded = true
                                musicExpandTimer = true
                            }
                        )
                    }
                }
                currentState is IslandState.Call -> CallContent(currentState)
                currentState is IslandState.Notification -> NotificationContent(currentState)
                currentState is IslandState.Timer -> TimerContent(currentState)
                currentState is IslandState.Recording -> RecordingContent(currentState)
            }
        }
    }
}

/**
 * Idle state content
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
 * Audio Visualizer - 3 bars
 */
@Composable
private fun AudioVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 3
) {
    val barHeights = remember { List(barCount) { mutableFloatStateOf(0.3f) } }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                barHeights.forEachIndexed { _, heightState ->
                    val targetHeight = Random.nextFloat() * 0.7f + 0.3f

                    animate(
                        initialValue = heightState.floatValue,
                        targetValue = targetHeight,
                        animationSpec = tween(
                            durationMillis = Random.nextInt(200, 400),
                            easing = FastOutSlowInEasing
                        )
                    ) { value, _ ->
                        heightState.floatValue = value
                    }
                }
                kotlinx.coroutines.delay(Random.nextLong(100, 300))
            }
        } else {
            barHeights.forEach { heightState ->
                animate(
                    initialValue = heightState.floatValue,
                    targetValue = 0.2f,
                    animationSpec = tween(durationMillis = 300)
                ) { value, _ ->
                    heightState.floatValue = value
                }
            }
        }
    }

    Canvas(modifier = modifier) {
        val barWidth = size.width / (barCount * 2 - 1)
        val maxBarHeight = size.height

        barHeights.forEachIndexed { index, heightState ->
            val barHeight = maxBarHeight * heightState.floatValue
            val x = index * barWidth * 2
            val y = (maxBarHeight - barHeight) / 2

            drawRoundRect(
                color = IslandAccent,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )
        }
    }
}

/**
 * Music compact - kecil seperti idle
 * Album | Visualizer | Skip buttons
 */
@Composable
private fun MusicContentCompact(
    state: IslandState.Music,
    onButtonClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Album art (kiri)
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(MaterialTheme.shapes.small)
                .background(IslandAccent.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = IslandAccent,
                modifier = Modifier.size(12.dp)
            )
        }

        // Audio Visualizer (tengah)
        AudioVisualizer(
            isPlaying = state.isPlaying,
            modifier = Modifier
                .width(24.dp)
                .height(18.dp)
        )

        // Skip Previous
        IconButton(
            onClick = { onButtonClick() },
            modifier = Modifier.size(18.dp),
            content = {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    tint = IslandAccent,
                    modifier = Modifier.size(12.dp)
                )
            }
        )

        // Play/Pause
        IconButton(
            onClick = { onButtonClick() },
            modifier = Modifier.size(18.dp),
            content = {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Default.Pause
                    else Icons.Default.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    tint = IslandAccent,
                    modifier = Modifier.size(12.dp)
                )
            }
        )

        // Skip Next
        IconButton(
            onClick = { onButtonClick() },
            modifier = Modifier.size(18.dp),
            content = {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = IslandAccent,
                    modifier = Modifier.size(12.dp)
                )
            }
        )
    }
}

/**
 * Music expanded - tampil title + artist
 */
@Composable
private fun MusicContentExpanded(
    state: IslandState.Music,
    onButtonClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title
        Text(
            text = state.title,
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Artist
        Text(
            text = state.artist,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Compact controls saat expanded
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onButtonClick() },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    tint = IslandAccent,
                    modifier = Modifier.size(14.dp)
                )
            }

            IconButton(
                onClick = { onButtonClick() },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Default.Pause
                    else Icons.Default.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    tint = IslandAccent,
                    modifier = Modifier.size(14.dp)
                )
            }

            IconButton(
                onClick = { onButtonClick() },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = IslandAccent,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * Call content
 */
@Composable
private fun CallContent(state: IslandState.Call) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
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

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { },
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

            if (state.isIncoming) {
                IconButton(
                    onClick = { },
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
 * Timer content
 */
@Composable
private fun TimerContent(state: IslandState.Timer) {
    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator(
            progress = { state.remainingTime.toFloat() / state.totalTime.toFloat() },
            modifier = Modifier.size(32.dp),
            color = ExpressiveOrange80,
            strokeWidth = 3.dp,
        )

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
 * Recording content
 */
@Composable
private fun RecordingContent(state: IslandState.Recording) {
    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        var alpha by remember { mutableFloatStateOf(1f) }

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

        Box(
            modifier = Modifier
                .size(12.dp)
                .background(
                    ErrorRed40.copy(alpha = alpha),
                    shape = MaterialTheme.shapes.extraSmall
                )
        )

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

        Icon(
            imageVector = if (state.isPaused) Icons.Default.PlayArrow
            else Icons.Default.Pause,
            contentDescription = null,
            tint = IslandAccent
        )
    }
}

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
