package id.xms.islandx

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.xms.islandx.data.IslandPreferencesManager
import id.xms.islandx.data.IslandSettings
import id.xms.islandx.ui.theme.IslandXTheme
import id.xms.islandx.utils.DisplayCutoutHelper
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            IslandXTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SettingsScreen(
                        onOpenAccessibilitySettings = {
                            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        },
                        onOpenNotificationSettings = {
                            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenAccessibilitySettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit
) {
    val context = LocalContext.current
    val preferencesManager = remember { IslandPreferencesManager(context) }
    val settings by preferencesManager.settingsFlow.collectAsState(initial = IslandSettings())
    val scope = rememberCoroutineScope()

    // Get cutout info
    val cutoutInfo = remember { DisplayCutoutHelper.getCutoutInfo(context) }
    val cutoutType = remember(cutoutInfo) {
        DisplayCutoutHelper.getCutoutTypeDescription(cutoutInfo)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "IslandX",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Welcome Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Selamat datang di IslandX",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Dynamic Island untuk Android Anda",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            // Device Info Card (if has cutout)
            if (cutoutInfo.hasCutout) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "Informasi Perangkat",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Text(
                            text = "Tipe Cutout: $cutoutType",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Safe Inset Top: ${cutoutInfo.safeInsetTop}px",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Resolusi: ${cutoutInfo.screenWidth}×${cutoutInfo.screenHeight}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Permissions Section
            Text(
                text = "Izin yang Diperlukan",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 8.dp)
            )

            PermissionCard(
                title = "Aksesibilitas",
                description = "Diperlukan untuk menampilkan Dynamic Island di atas aplikasi lain",
                icon = Icons.Default.Accessibility,
                isEnabled = settings.isEnabled,
                onEnableClick = onOpenAccessibilitySettings
            )

            PermissionCard(
                title = "Akses Notifikasi",
                description = "Diperlukan untuk menampilkan notifikasi di Dynamic Island",
                icon = Icons.Default.Notifications,
                isEnabled = false,
                onEnableClick = onOpenNotificationSettings
            )

            // Customization Section
            Text(
                text = "Kustomisasi Posisi & Ukuran",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 16.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Y Offset
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Posisi Vertikal (Y)",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "${settings.yOffset} dp",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "Jarak dari atas layar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Slider(
                            value = settings.yOffset.toFloat(),
                            onValueChange = { value ->
                                scope.launch {
                                    preferencesManager.updateYOffset(value.toInt())
                                }
                            },
                            valueRange = 0f..100f,
                            steps = 19,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    HorizontalDivider()

                    // Width
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Lebar Island",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "${settings.width} dp",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "Lebar maksimal Dynamic Island",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Slider(
                            value = settings.width.toFloat(),
                            onValueChange = { value ->
                                scope.launch {
                                    preferencesManager.updateWidth(value.toInt())
                                }
                            },
                            valueRange = 200f..400f,
                            steps = 19,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    HorizontalDivider()

                    // Height
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Tinggi Compact",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "${settings.height} dp",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "Tinggi saat mode idle/compact",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Slider(
                            value = settings.height.toFloat(),
                            onValueChange = { value ->
                                scope.launch {
                                    preferencesManager.updateHeight(value.toInt())
                                }
                            },
                            valueRange = 28f..48f,
                            steps = 19,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    HorizontalDivider()

                    // Horizontal Margin
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Margin Horizontal",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "${settings.horizontalMargin} dp",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "Jarak dari tepi kiri dan kanan",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Slider(
                            value = settings.horizontalMargin.toFloat(),
                            onValueChange = { value ->
                                scope.launch {
                                    preferencesManager.updateHorizontalMargin(value.toInt())
                                }
                            },
                            valueRange = 0f..40f,
                            steps = 19,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            // General Settings Section
            Text(
                text = "Pengaturan Umum",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 16.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingItem(
                        title = "Auto-Adjust untuk Notch",
                        description = "Sesuaikan posisi otomatis dengan cutout",
                        icon = Icons.Default.AutoAwesome,
                        checked = settings.autoAdjustForNotch,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                preferencesManager.updateAutoAdjustNotch(enabled)
                            }
                        }
                    )

                    HorizontalDivider()

                    SettingItem(
                        title = "Tampilkan di Lockscreen",
                        description = "Tampilkan island saat layar terkunci",
                        icon = Icons.Default.Lock,
                        checked = settings.showOnLockscreen,
                        onCheckedChange = { show ->
                            scope.launch {
                                preferencesManager.updateShowOnLockscreen(show)
                            }
                        }
                    )

                    HorizontalDivider()

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null
                                )
                                Column {
                                    Text(
                                        text = "Durasi Auto-Hide",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "${settings.autoHideDelay.toInt()} detik",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }

                        Slider(
                            value = settings.autoHideDelay,
                            onValueChange = { delay ->
                                scope.launch {
                                    preferencesManager.updateAutoHideDelay(delay)
                                }
                            },
                            valueRange = 1f..10f,
                            steps = 8,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            // Preview Button
            Button(
                onClick = {
                    // TODO: Trigger test notification untuk preview
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Preview Dynamic Island")
            }

            // About Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Tentang IslandX",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Versi 1.0\nDibuat dengan Material 3 Expressive\nSupport Android 9 - 15",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isEnabled: Boolean,
    onEnableClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isEnabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Button(
                onClick = onEnableClick,
                enabled = !isEnabled
            ) {
                Text(if (isEnabled) "Aktif" else "Aktifkan")
            }
        }
    }
}

@Composable
fun SettingItem(
    title: String,
    description: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
