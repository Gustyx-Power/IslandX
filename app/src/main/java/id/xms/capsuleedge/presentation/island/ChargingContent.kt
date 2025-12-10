package id.xms.capsuleedge.presentation.island

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.xms.capsuleedge.domain.model.IslandEvent

/**
 * Charging/Battery content displayed in the Island
 * Features expressive animations for an engaging charging experience
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
    val infiniteTransition = rememberInfiniteTransition(label = "charging_compact")
    
    // Pulsing glow effect
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    
    // Subtle scale pulse
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Charging indicator with pulse
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.scale(pulseScale)
        ) {
            // Animated lightning bolt
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        Color(if (event.isFastCharging) 0xFFFF9800 else 0xFF4CAF50)
                            .copy(alpha = glowAlpha * 0.3f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.FlashOn,
                    contentDescription = null,
                    tint = Color(if (event.isFastCharging) 0xFFFF9800 else 0xFF4CAF50)
                        .copy(alpha = glowAlpha),
                    modifier = Modifier.size(16.dp)
                )
            }
            
            Text(
                text = if (event.isFastCharging) "Fast" else "Charging",
                color = Color(if (event.isFastCharging) 0xFFFF9800 else 0xFF4CAF50)
                    .copy(alpha = glowAlpha),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        // Battery percentage with animation
        Text(
            text = "${event.batteryLevel}%",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.scale(pulseScale)
        )
    }
}

@Composable
private fun ExpandedChargingContent(event: IslandEvent.Charging) {
    val infiniteTransition = rememberInfiniteTransition(label = "charging_expanded")
    
    // Multiple animated elements for expressive UI
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_rotation"
    )
    
    val ringRotation2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_rotation_2"
    )
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    
    // Floating particles animation
    val particleOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "particles"
    )
    
    val primaryColor = if (event.isFastCharging) Color(0xFFFF9800) else Color(0xFF4CAF50)
    val secondaryColor = if (event.isFastCharging) Color(0xFFFFB74D) else Color(0xFF81C784)
    
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Animated battery circle with multiple rings
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(100.dp)
                .scale(pulseScale)
        ) {
            // Outer glow
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .alpha(glowAlpha * 0.5f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        )
                    )
            )
            
            // Outer rotating ring
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .rotate(ringRotation)
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.1f),
                                primaryColor.copy(alpha = 0.6f),
                                secondaryColor.copy(alpha = 0.8f),
                                primaryColor.copy(alpha = 0.1f)
                            )
                        )
                    )
            )
            
            // Inner counter-rotating ring
            Box(
                modifier = Modifier
                    .size(78.dp)
                    .rotate(ringRotation2)
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(
                            colors = listOf(
                                secondaryColor.copy(alpha = 0.1f),
                                primaryColor.copy(alpha = 0.5f),
                                secondaryColor.copy(alpha = 0.1f)
                            )
                        )
                    )
            )
            
            // Center circle with percentage
            Box(
                modifier = Modifier
                    .size(66.dp)
                    .clip(CircleShape)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Lightning icon
                    Icon(
                        imageVector = Icons.Rounded.FlashOn,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier
                            .size(18.dp)
                            .graphicsLayer {
                                translationY = -particleOffset / 4
                            }
                    )
                    
                    // Battery percentage
                    Text(
                        text = "${event.batteryLevel}%",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        // Right side: Status and progress
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Charging status
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (event.isFastCharging) "⚡ Fast Charging" else "🔋 Charging",
                    color = primaryColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = when {
                        event.batteryLevel >= 95 -> "Almost full!"
                        event.batteryLevel >= 80 -> "Getting there..."
                        event.batteryLevel >= 50 -> "Halfway charged"
                        event.batteryLevel >= 20 -> "Keep charging"
                        else -> "Low battery, charging..."
                    },
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
            
            // Animated progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                // Progress fill with gradient
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(event.batteryLevel / 100f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(primaryColor, secondaryColor)
                            )
                        )
                )
                
                // Animated shimmer effect
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(30.dp)
                        .offset(x = (event.batteryLevel / 100f * 200 - 15 + particleOffset).dp)
                        .alpha(glowAlpha)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.4f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
            
            // Charging speed indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(if (event.isFastCharging) 3 else 1) { index ->
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .alpha(if (index == 0) 1f else glowAlpha)
                            .background(primaryColor)
                    )
                }
                
                Text(
                    text = if (event.isFastCharging) "High speed" else "Standard",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp
                )
            }
        }
    }
}
