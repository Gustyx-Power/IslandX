package id.xms.capsuleedge.presentation.island

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.xms.capsuleedge.domain.model.IslandEvent
import id.xms.capsuleedge.service.MediaAction

/**
 * Media playback content displayed in the Island
 * Features iOS-like Dynamic Island music player UI
 */
@Composable
fun MediaContent(
    event: IslandEvent.MediaPlayback,
    isExpanded: Boolean,
    onMediaAction: (MediaAction) -> Unit
) {
    AnimatedContent(
        targetState = isExpanded,
        transitionSpec = {
            fadeIn(animationSpec = tween(200)) togetherWith
                    fadeOut(animationSpec = tween(200))
        },
        label = "media_content"
    ) { expanded ->
        if (expanded) {
            ExpandedMediaContent(event, onMediaAction)
        } else {
            CompactMediaContent(event, onMediaAction)
        }
    }
}

/**
 * Compact media view - shows album art, title, and mini visualizer
 */
@Composable
private fun CompactMediaContent(
    event: IslandEvent.MediaPlayback,
    onMediaAction: (MediaAction) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: Spinning album art (when playing)
        SpinningAlbumArt(
            albumArt = event.albumArt,
            isPlaying = event.isPlaying,
            size = 36
        )
        
        Spacer(modifier = Modifier.width(10.dp))
        
        // Center: Title and artist
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = event.title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = event.artist,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Right: Mini visualizer or pause icon
        if (event.isPlaying) {
            MiniVisualizer()
        } else {
            // Pause indicator
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Expanded media view - full player with controls
 */
@Composable
private fun ExpandedMediaContent(
    event: IslandEvent.MediaPlayback,
    onMediaAction: (MediaAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Top row: Album art, track info, and waveform
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Large album art with rounded corners
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF2A2A2A), Color(0xFF1A1A1A))
                        )
                    )
            ) {
                event.albumArt?.let { art ->
                    Image(
                        bitmap = art.asImageBitmap(),
                        contentDescription = "Album Art",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } ?: run {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.Center)
                    )
                }
            }
            
            // Track info with marquee for long text
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                // Title with marquee effect
                Text(
                    text = event.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee(
                        iterations = Int.MAX_VALUE,
                        velocity = 30.dp
                    )
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = event.artist,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Waveform visualizer on the right
            if (event.isPlaying) {
                WaveformVisualizer()
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        // Progress bar
        if (event.duration > 0) {
            val progress = (event.position.toFloat() / event.duration.toFloat()).coerceIn(0f, 1f)
            
            Column {
                // Progress track
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color.White, Color.White.copy(alpha = 0.8f))
                                )
                            )
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Time labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDuration(event.position),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp
                    )
                    Text(
                        text = formatDuration(event.duration),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Media control buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous button
            IconButton(
                onClick = { onMediaAction(MediaAction.PREVIOUS) },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "Previous",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
            
            // Play/Pause button (larger, with background)
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = { onMediaAction(MediaAction.PLAY_PAUSE) },
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        imageVector = if (event.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (event.isPlaying) "Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
            
            // Next button
            IconButton(
                onClick = { onMediaAction(MediaAction.NEXT) },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Next",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

/**
 * Spinning album art for compact view
 */
@Composable
private fun SpinningAlbumArt(
    albumArt: android.graphics.Bitmap?,
    isPlaying: Boolean,
    size: Int
) {
    val infiniteTransition = rememberInfiniteTransition(label = "spin")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Color(0xFF2A2A2A))
            .then(
                if (isPlaying) Modifier.rotate(rotation) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        albumArt?.let { art ->
            Image(
                bitmap = art.asImageBitmap(),
                contentDescription = "Album Art",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } ?: run {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size((size / 2).dp)
            )
        }
        
        // Center hole for vinyl effect
        Box(
            modifier = Modifier
                .size((size / 4).dp)
                .clip(CircleShape)
                .background(Color.Black)
        )
    }
}

/**
 * Mini audio visualizer animation (3 bars)
 */
@Composable
private fun MiniVisualizer() {
    val infiniteTransition = rememberInfiniteTransition(label = "mini_visualizer")
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val delay = index * 100
            val animatedHeight by infiniteTransition.animateFloat(
                initialValue = 4f,
                targetValue = 16f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 400,
                        delayMillis = delay,
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$index"
            )
            
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(animatedHeight.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF4CAF50), Color(0xFF8BC34A))
                        )
                    )
            )
        }
    }
}

/**
 * Waveform visualizer for expanded view
 */
@Composable
private fun WaveformVisualizer() {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        repeat(5) { index ->
            val phase = index * 0.5f
            val animatedHeight by infiniteTransition.animateFloat(
                initialValue = 8f + (phase * 4f),
                targetValue = 24f - (phase * 2f),
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 300 + (index * 50),
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "wave_$index"
            )
            
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(animatedHeight.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF4CAF50),
                                Color(0xFF81C784)
                            )
                        )
                    )
            )
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
