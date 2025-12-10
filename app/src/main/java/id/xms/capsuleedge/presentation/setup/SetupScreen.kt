package id.xms.capsuleedge.presentation.setup

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.xms.capsuleedge.R
import id.xms.capsuleedge.service.CapsuleAccessibilityService

data class PermissionItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val isGranted: Boolean,
    val onGrant: () -> Unit
)

@Composable
fun SetupScreen(
    overlayGranted: Boolean,
    accessibilityGranted: Boolean,
    notificationGranted: Boolean,
    mediaListenerGranted: Boolean,
    batteryGranted: Boolean,
    onSetupComplete: () -> Unit,
    onOpenMediaListenerSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Use permission states passed from MainActivity (refreshed on every onResume)
    val overlayPermission = overlayGranted
    val accessibilityPermission = accessibilityGranted
    var notificationPermission by remember { mutableStateOf(notificationGranted) }
    val mediaListenerPermission = mediaListenerGranted
    val batteryOptimization = batteryGranted
    
    // Update notification permission when prop changes
    LaunchedEffect(notificationGranted) {
        notificationPermission = notificationGranted
    }
    
    // Launcher for notification permission
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            notificationPermission = granted
        }
    )
    
    val allPermissionsGranted = overlayPermission && accessibilityPermission && 
            notificationPermission && mediaListenerPermission && batteryOptimization
    
    // Animated background
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
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
                        Color(0xFF0A0A0A)
                    ),
                    startY = gradientOffset * 500f
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            
            // Header with animated island preview
            AnimatedIslandPreview()
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Title
            Text(
                text = stringResource(R.string.setup_title),
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = stringResource(R.string.setup_subtitle),
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Permission cards
            val permissions = listOf(
                PermissionItem(
                    title = stringResource(R.string.permission_overlay_title),
                    description = stringResource(R.string.permission_overlay_desc),
                    icon = Icons.Filled.Layers,
                    isGranted = overlayPermission,
                    onGrant = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }
                ),
                PermissionItem(
                    title = stringResource(R.string.permission_accessibility_title),
                    description = stringResource(R.string.permission_accessibility_desc),
                    icon = Icons.Filled.Accessibility,
                    isGranted = accessibilityPermission,
                    onGrant = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    }
                ),
                PermissionItem(
                    title = stringResource(R.string.permission_notification_title),
                    description = stringResource(R.string.permission_notification_desc),
                    icon = Icons.Filled.Notifications,
                    isGranted = notificationPermission,
                    onGrant = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            notificationPermission = true
                        }
                    }
                ),
                PermissionItem(
                    title = stringResource(R.string.permission_media_title),
                    description = stringResource(R.string.permission_media_desc),
                    icon = Icons.Filled.MusicNote,
                    isGranted = mediaListenerPermission,
                    onGrant = {
                        onOpenMediaListenerSettings()
                    }
                ),
                PermissionItem(
                    title = stringResource(R.string.permission_battery_title),
                    description = stringResource(R.string.permission_battery_desc),
                    icon = Icons.Filled.BatteryStd,
                    isGranted = batteryOptimization,
                    onGrant = {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    }
                )
            )
            
            permissions.forEachIndexed { index, permission ->
                PermissionCard(
                    item = permission,
                    delay = index * 100
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Continue button
            AnimatedVisibility(
                visible = allPermissionsGranted,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                Button(
                    onClick = onSetupComplete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.btn_start_service),
                        color = Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            if (!allPermissionsGranted) {
                Text(
                    text = "Grant all permissions to continue",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 14.sp
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun AnimatedIslandPreview() {
    val infiniteTransition = rememberInfiniteTransition(label = "island_preview")
    
    val width by infiniteTransition.animateFloat(
        initialValue = 100f,
        targetValue = 200f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "width"
    )
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Box(
        modifier = Modifier
            .scale(scale)
            .width(width.dp)
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(18.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        // Small indicator
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Color(0xFF1A1A1A))
        )
    }
}

@Composable
private fun PermissionCard(
    item: PermissionItem,
    delay: Int
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delay.toLong())
        visible = true
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInHorizontally { -it }
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.05f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (item.isGranted) 
                                Color(0xFF4CAF50).copy(alpha = 0.2f)
                            else 
                                Color.White.copy(alpha = 0.1f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (item.isGranted) Icons.Filled.Check else item.icon,
                        contentDescription = null,
                        tint = if (item.isGranted) Color(0xFF4CAF50) else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Text content
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = item.description,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 13.sp
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Grant button
                if (!item.isGranted) {
                    Button(
                        onClick = item.onGrant,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Grant",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Granted",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun checkNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == 
            android.content.pm.PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}
