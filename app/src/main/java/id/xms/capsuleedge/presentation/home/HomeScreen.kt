package id.xms.capsuleedge.presentation.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.xms.capsuleedge.R
import id.xms.capsuleedge.data.repository.IslandStateRepository
import id.xms.capsuleedge.data.repository.SettingsRepository
import id.xms.capsuleedge.domain.model.IslandConfig
import id.xms.capsuleedge.service.OverlayService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsRepository = remember { SettingsRepository(context) }
    
    val uiState by IslandStateRepository.uiState.collectAsState()
    val config by settingsRepository.configFlow.collectAsState(initial = IslandConfig())
    
    // Animated background
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradient"
    )
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0A0A),
                        Color(0xFF1A1A2E),
                        Color(0xFF16213E)
                    ),
                    startY = gradientOffset * 300f
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CapsuleEdge",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Dynamic Island for Android",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }
                
                // Status indicator
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(
                            if (uiState.isServiceRunning) Color(0xFF4CAF50) else Color.Gray
                        )
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Main control card - Service toggle
            ServiceControlCard(
                isRunning = uiState.isServiceRunning,
                onToggle = {
                    if (uiState.isServiceRunning) {
                        OverlayService.stop(context)
                    } else {
                        OverlayService.start(context)
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Settings section
            Text(
                text = stringResource(R.string.settings_title),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Position settings
            SettingsCard(
                title = stringResource(R.string.settings_position),
                icon = Icons.Filled.ControlCamera
            ) {
                Column {
                    // X Offset
                    SliderSetting(
                        label = "Horizontal Offset",
                        value = config.offsetX,
                        valueRange = -100f..100f,
                        onValueChange = { newValue ->
                            scope.launch {
                                settingsRepository.updateOffset(newValue, config.offsetY)
                            }
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Y Offset: negative = up (towards notch), positive = down
                    SliderSetting(
                        label = "Vertical Offset",
                        value = config.offsetY,
                        valueRange = -30f..80f,
                        onValueChange = { newValue ->
                            scope.launch {
                                settingsRepository.updateOffset(config.offsetX, newValue)
                            }
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Size settings
            SettingsCard(
                title = stringResource(R.string.settings_size),
                icon = Icons.Filled.ZoomOutMap
            ) {
                // Capsule Width
                SliderSetting(
                    label = "Capsule Width",
                    value = config.collapsedWidth,
                    valueRange = 80f..200f,
                    onValueChange = { newValue ->
                        scope.launch {
                            settingsRepository.updateCollapsedWidth(newValue)
                        }
                    },
                    valueFormat = { "${it.toInt()}dp" }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Scale
                SliderSetting(
                    label = "Scale",
                    value = config.scale,
                    valueRange = 0.5f..1.5f,
                    onValueChange = { newValue ->
                        scope.launch {
                            settingsRepository.updateScale(newValue)
                        }
                    },
                    valueFormat = { "%.1fx".format(it) }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Preview section
            Text(
                text = "Preview",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Island preview
            IslandPreviewCard(config = config)
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ServiceControlCard(
    isRunning: Boolean,
    onToggle: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isRunning) 
                Color(0xFF4CAF50).copy(alpha = 0.15f) 
            else 
                Color.White.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Animated icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            if (isRunning)
                                Color(0xFF4CAF50).copy(alpha = 0.2f)
                            else
                                Color.White.copy(alpha = 0.1f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Filled.PlayArrow else Icons.Filled.Close,
                        contentDescription = null,
                        tint = if (isRunning) Color(0xFF4CAF50) else Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                Column {
                    Text(
                        text = if (isRunning) "Island Active" else "Island Inactive",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (isRunning) "Tap to stop" else "Tap to start",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }
            }
            
            Switch(
                checked = isRunning,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF4CAF50),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
                )
            )
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            content()
        }
    }
}

@Composable
private fun SliderSetting(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    valueFormat: (Float) -> String = { "%.0f".format(it) }
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
            Text(
                text = valueFormat(value),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            )
        )
    }
}

@Composable
private fun IslandPreviewCard(config: IslandConfig) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            // Preview of the island
            Box(
                modifier = Modifier
                    .offset(x = config.offsetX.dp, y = config.offsetY.dp)
                    .width((config.collapsedWidth * config.scale).dp)
                    .height((config.collapsedHeight * config.scale).dp)
                    .clip(RoundedCornerShape((config.cornerRadius * config.scale).dp))
                    .background(Color.Black)
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape((config.cornerRadius * config.scale).dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Camera dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1A1A1A))
                )
            }
        }
    }
}
