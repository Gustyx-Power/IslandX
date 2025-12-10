package id.xms.capsuleedge.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import id.xms.capsuleedge.data.repository.SettingsRepository
import id.xms.capsuleedge.presentation.home.HomeScreen
import id.xms.capsuleedge.presentation.setup.SetupScreen
import id.xms.capsuleedge.service.CapsuleAccessibilityService
import id.xms.capsuleedge.service.MediaListenerService
import id.xms.capsuleedge.service.OverlayService
import id.xms.capsuleedge.ui.theme.CapsuleEdgeTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    private lateinit var settingsRepository: SettingsRepository
    
    // Permission states as StateFlow for reactive updates
    private val _overlayPermission = MutableStateFlow(false)
    private val _accessibilityPermission = MutableStateFlow(false)
    private val _notificationPermission = MutableStateFlow(false)
    private val _mediaListenerPermission = MutableStateFlow(false)
    private val _batteryOptimization = MutableStateFlow(false)
    private val _allPermissionsGranted = MutableStateFlow(false)
    private val _showSetup = MutableStateFlow(true)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        settingsRepository = SettingsRepository(this)
        
        // Initial permission check
        refreshPermissionStates()
        
        // Add lifecycle observer for automatic permission refresh on resume
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissionStates()
                
                // Auto-start service if all permissions granted and not already running
                if (_allPermissionsGranted.value && !OverlayService.isRunning()) {
                    lifecycleScope.launch {
                        val isFirstLaunch = settingsRepository.isFirstLaunch.first()
                        if (!isFirstLaunch) {
                            OverlayService.start(this@MainActivity)
                        }
                    }
                }
            }
        })
        
        setContent {
            CapsuleEdgeTheme {
                // Collect permission states reactively
                val overlayPermission by _overlayPermission.collectAsState()
                val accessibilityPermission by _accessibilityPermission.collectAsState()
                val notificationPermission by _notificationPermission.collectAsState()
                val mediaListenerPermission by _mediaListenerPermission.collectAsState()
                val batteryOptimization by _batteryOptimization.collectAsState()
                val allPermissionsGranted by _allPermissionsGranted.collectAsState()
                val showSetup by _showSetup.collectAsState()
                
                var isFirstLaunch by remember { mutableStateOf<Boolean?>(null) }
                
                // Check if first launch
                LaunchedEffect(Unit) {
                    isFirstLaunch = settingsRepository.isFirstLaunch.first()
                    _showSetup.value = isFirstLaunch == true || !allPermissionsGranted
                }
                
                // Update showSetup when permissions change
                LaunchedEffect(allPermissionsGranted, isFirstLaunch) {
                    if (isFirstLaunch != null) {
                        _showSetup.value = isFirstLaunch == true || !allPermissionsGranted
                    }
                }
                
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when {
                        isFirstLaunch == null -> {
                            // Loading state - could show splash
                        }
                        showSetup -> {
                            SetupScreen(
                                overlayGranted = overlayPermission,
                                accessibilityGranted = accessibilityPermission,
                                notificationGranted = notificationPermission,
                                mediaListenerGranted = mediaListenerPermission,
                                batteryGranted = batteryOptimization,
                                onSetupComplete = {
                                    lifecycleScope.launch {
                                        settingsRepository.setFirstLaunchComplete()
                                    }
                                    // Start the overlay service
                                    OverlayService.start(this@MainActivity)
                                    _showSetup.value = false
                                },
                                onOpenMediaListenerSettings = {
                                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                    startActivity(intent)
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        else -> {
                            HomeScreen(
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh permission states immediately when app resumes
        refreshPermissionStates()
    }
    
    /**
     * CRITICAL FIX #3: Refresh all permission states
     * Called on every onResume to detect permission changes from system settings
     */
    private fun refreshPermissionStates() {
        // Overlay permission
        _overlayPermission.value = Settings.canDrawOverlays(this)
        
        // Accessibility permission
        _accessibilityPermission.value = CapsuleAccessibilityService.isServiceEnabled(this)
        
        // Notification permission (Android 13+)
        _notificationPermission.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        
        // Media Listener permission (NotificationListenerService)
        _mediaListenerPermission.value = MediaListenerService.isServiceEnabled(this)
        
        // Battery optimization
        _batteryOptimization.value = try {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            powerManager.isIgnoringBatteryOptimizations(packageName)
        } catch (e: Exception) {
            false
        }
        
        // Update combined state - media listener is now required
        _allPermissionsGranted.value = _overlayPermission.value && 
                _accessibilityPermission.value && 
                _notificationPermission.value && 
                _mediaListenerPermission.value &&
                _batteryOptimization.value
    }
}
