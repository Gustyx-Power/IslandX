package id.xms.capsuleedge.presentation.island

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.xms.capsuleedge.domain.model.BluetoothDeviceType
import id.xms.capsuleedge.domain.model.IslandEvent

/**
 * Bluetooth connection content displayed in the Island
 */
@Composable
fun BluetoothContent(
    event: IslandEvent.BluetoothConnection,
    isExpanded: Boolean
) {
    if (isExpanded) {
        ExpandedBluetoothContent(event)
    } else {
        CompactBluetoothContent(event)
    }
}

@Composable
private fun CompactBluetoothContent(event: IslandEvent.BluetoothConnection) {
    // Pulse animation for connection
    val infiniteTransition = rememberInfiniteTransition(label = "bluetooth_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Device icon with pulse
        Box(
            modifier = Modifier
                .scale(if (event.isConnected) scale else 1f)
        ) {
            Icon(
                imageVector = getBluetoothDeviceIcon(event.deviceType),
                contentDescription = event.deviceName,
                tint = if (event.isConnected) Color(0xFF2196F3) else Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
        }
        
        // Device name
        Text(
            text = event.deviceName,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        
        // Connection status indicator
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    if (event.isConnected) Color(0xFF2196F3) else Color.Gray
                )
        )
    }
}

@Composable
private fun ExpandedBluetoothContent(event: IslandEvent.BluetoothConnection) {
    val infiniteTransition = rememberInfiniteTransition(label = "bluetooth_expanded")
    val waveAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave"
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // Large device icon with waves
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(80.dp)
        ) {
            // Connection waves
            if (event.isConnected) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .size((40 + index * 16).dp)
                            .clip(CircleShape)
                            .background(
                                Color(0xFF2196F3).copy(alpha = waveAlpha * (1f - index * 0.3f))
                            )
                    )
                }
            }
            
            // Device icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF2196F3).copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getBluetoothDeviceIcon(event.deviceType),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        
        // Device info
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = event.deviceName,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (event.isConnected) "Connected" else "Disconnected",
                    color = if (event.isConnected) Color(0xFF2196F3) else Color.Gray,
                    fontSize = 14.sp
                )
                
                // Battery level if available
                event.batteryLevel?.let { battery ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = getBatteryIcon(battery),
                            contentDescription = "Battery",
                            tint = getBatteryColor(battery),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "$battery%",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
        
        // Bluetooth indicator
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Bluetooth,
                contentDescription = null,
                tint = Color(0xFF2196F3),
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "Bluetooth",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp
            )
        }
    }
}

private fun getBluetoothDeviceIcon(type: BluetoothDeviceType): ImageVector {
    return when (type) {
        BluetoothDeviceType.HEADPHONES -> Icons.Filled.Headphones
        BluetoothDeviceType.EARBUDS -> Icons.Filled.Headset
        BluetoothDeviceType.SPEAKER -> Icons.Filled.Speaker
        BluetoothDeviceType.WATCH -> Icons.Filled.Watch
        BluetoothDeviceType.OTHER -> Icons.Filled.BluetoothConnected
    }
}

private fun getBatteryIcon(level: Int): ImageVector {
    return when {
        level <= 20 -> Icons.Filled.Battery1Bar
        level <= 40 -> Icons.Filled.Battery2Bar
        level <= 60 -> Icons.Filled.Battery3Bar
        level <= 80 -> Icons.Filled.Battery4Bar
        else -> Icons.Filled.BatteryFull
    }
}

private fun getBatteryColor(level: Int): Color {
    return when {
        level <= 20 -> Color(0xFFF44336)
        level <= 40 -> Color(0xFFFF9800)
        else -> Color(0xFF4CAF50)
    }
}
