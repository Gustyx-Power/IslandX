package id.xms.capsuleedge.presentation.island

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.xms.capsuleedge.domain.model.IslandEvent

/**
 * Charging/Battery content displayed in the Island
 */
@Composable
fun ChargingContent(
    event: IslandEvent.Charging,
    isExpanded: Boolean
) {
    if (isExpanded) {
        ExpandedChargingContent(event)
    } else {
        CompactChargingContent(event)
    }
}

@Composable
private fun CompactChargingContent(event: IslandEvent.Charging) {
    // Animated lightning bolt glow
    val infiniteTransition = rememberInfiniteTransition(label = "charging_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Charging text (no icon)
        Text(
            text = if (event.isFastCharging) "Fast Charging" else "Charging",
            color = Color(0xFF4CAF50).copy(alpha = glowAlpha),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        
        // Battery percentage
        Text(
            text = "${event.batteryLevel}%",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ExpandedChargingContent(event: IslandEvent.Charging) {
    val infiniteTransition = rememberInfiniteTransition(label = "charging_expanded")
    
    // Animated ring effect
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_rotation"
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // Animated battery circle
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(80.dp)
        ) {
            // Rotating gradient ring
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(ringRotation)
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(
                            colors = listOf(
                                Color(0xFF4CAF50).copy(alpha = 0.1f),
                                Color(0xFF4CAF50).copy(alpha = 0.5f),
                                Color(0xFF8BC34A).copy(alpha = 0.8f),
                                Color(0xFF4CAF50).copy(alpha = 0.1f)
                            )
                        )
                    )
            )
            
            // Inner circle
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                // Just battery percentage, no icon
                Text(
                    text = "${event.batteryLevel}%",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        // Charging status text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Plain text without emoji/icon
            Text(
                text = if (event.isFastCharging) "Fast Charging" else "Charging",
                color = Color(0xFF4CAF50),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            
            // Estimated time (placeholder - would need actual calculation)
            Text(
                text = "Battery will be full soon",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
        }
        
        // Progress bar
        LinearProgressIndicator(
            progress = { event.batteryLevel / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = Color(0xFF4CAF50),
            trackColor = Color.White.copy(alpha = 0.1f),
            strokeCap = StrokeCap.Round
        )
    }
}
