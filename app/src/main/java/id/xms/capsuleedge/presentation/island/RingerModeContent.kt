package id.xms.capsuleedge.presentation.island

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.xms.capsuleedge.domain.model.IslandEvent
import id.xms.capsuleedge.domain.model.SoundMode

/**
 * Ringer/Sound mode content displayed in the Island
 */
@Composable
fun RingerModeContent(
    event: IslandEvent.RingerMode,
    isExpanded: Boolean
) {
    if (isExpanded) {
        ExpandedRingerContent(event)
    } else {
        CompactRingerContent(event)
    }
}

@Composable
private fun CompactRingerContent(event: IslandEvent.RingerMode) {
    // Quick pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "ringer_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Animated icon
        Box(
            modifier = Modifier.scale(scale)
        ) {
            Icon(
                imageVector = getRingerIcon(event.mode),
                contentDescription = null,
                tint = getRingerColor(event.mode),
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Mode text
        Text(
            text = getRingerModeText(event.mode),
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ExpandedRingerContent(event: IslandEvent.RingerMode) {
    val infiniteTransition = rememberInfiniteTransition(label = "ringer_expanded")
    
    // Vibration animation for vibrate mode
    val vibrationOffset by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(50),
            repeatMode = RepeatMode.Reverse
        ),
        label = "vibration"
    )
    
    // Wave animation for normal mode
    val waveScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave"
    )
    
    val waveAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_alpha"
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Large animated icon
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(80.dp)
        ) {
            // Sound waves for normal mode
            if (event.mode == SoundMode.NORMAL) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .size((40 + index * 12).dp)
                            .scale(waveScale)
                            .clip(CircleShape)
                            .background(
                                Color(0xFF4CAF50).copy(alpha = waveAlpha * (1f - index * 0.3f))
                            )
                    )
                }
            }
            
            // Main icon
            val iconModifier = when (event.mode) {
                SoundMode.VIBRATE -> Modifier
                    .size(48.dp)
                    .offset(x = vibrationOffset.dp)
                else -> Modifier.size(48.dp)
            }
            
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(getRingerColor(event.mode).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getRingerIcon(event.mode),
                    contentDescription = null,
                    tint = getRingerColor(event.mode),
                    modifier = iconModifier
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Mode text
        Text(
            text = getRingerModeText(event.mode),
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Mode description
        Text(
            text = getRingerModeDescription(event.mode),
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp
        )
    }
}

private fun getRingerIcon(mode: SoundMode): ImageVector {
    return when (mode) {
        SoundMode.NORMAL -> Icons.Filled.VolumeUp
        SoundMode.VIBRATE -> Icons.Filled.PhoneAndroid
        SoundMode.SILENT -> Icons.Filled.VolumeOff
    }
}

private fun getRingerColor(mode: SoundMode): Color {
    return when (mode) {
        SoundMode.NORMAL -> Color(0xFF4CAF50)
        SoundMode.VIBRATE -> Color(0xFFFF9800)
        SoundMode.SILENT -> Color(0xFFF44336)
    }
}

private fun getRingerModeText(mode: SoundMode): String {
    return when (mode) {
        SoundMode.NORMAL -> "Sound On"
        SoundMode.VIBRATE -> "Vibrate"
        SoundMode.SILENT -> "Silent"
    }
}

private fun getRingerModeDescription(mode: SoundMode): String {
    return when (mode) {
        SoundMode.NORMAL -> "Calls and notifications will ring"
        SoundMode.VIBRATE -> "Phone will vibrate for calls"
        SoundMode.SILENT -> "All sounds are muted"
    }
}
