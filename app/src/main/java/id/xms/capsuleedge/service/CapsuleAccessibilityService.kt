package id.xms.capsuleedge.service

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import id.xms.capsuleedge.R
import id.xms.capsuleedge.data.repository.IslandStateRepository
import id.xms.capsuleedge.data.repository.SettingsRepository
import id.xms.capsuleedge.domain.model.IslandEvent
import id.xms.capsuleedge.domain.model.IslandState
import id.xms.capsuleedge.presentation.island.DynamicIslandOverlay
import id.xms.capsuleedge.ui.theme.CapsuleEdgeTheme
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

/**
 * AccessibilityService that handles:
 * 1. Notification interception
 * 2. Dynamic Island overlay (using TYPE_ACCESSIBILITY_OVERLAY for highest Z-order)
 * 3. Media session monitoring
 * 4. Charging events
 */
class CapsuleAccessibilityService : AccessibilityService(), LifecycleOwner, SavedStateRegistryOwner {
    
    private val TAG = "CapsuleAccessibility"
    
    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null
    private lateinit var settingsRepository: SettingsRepository
    
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())
    
    // Media session management
    private var mediaSessionManager: MediaSessionManager? = null
    private val activeControllers = mutableMapOf<String, MediaController>()
    private val mediaCallbacks = mutableMapOf<String, MediaController.Callback>()
    
    // Progress polling job for media
    private var mediaProgressJob: Job? = null
    
    // Job to auto-dismiss media after pause timeout (1 minute)
    private var mediaPauseTimeoutJob: Job? = null

    // Charging receiver
    private var chargingReceiver: BroadcastReceiver? = null
    
    // Safe layout update tracking
    private var isViewAttached = false
    private var overlayParams: WindowManager.LayoutParams? = null
    
    // Orientation/landscape detection
    private var orientationListener: android.view.OrientationEventListener? = null
    private var isCurrentlyLandscape = false
    private var hideInLandscapeEnabled = true  // Setting from preferences
    
    // Track recently shown notifications to avoid duplicates
    private val recentNotifications = mutableSetOf<String>()
    
    // Packages to ignore (system notifications, etc)
    private val ignoredPackages = setOf(
        "android",
        "com.android.systemui",
        "com.android.providers.downloads",
        "com.android.vending"
    )
    
    override val lifecycle: Lifecycle
        get() = lifecycleRegistry
    
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry
    
    companion object {
        @Volatile
        private var instance: CapsuleAccessibilityService? = null
        
        fun getInstance(): CapsuleAccessibilityService? = instance
        
        fun isServiceEnabled(context: Context): Boolean {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            
            val componentName = ComponentName(context, CapsuleAccessibilityService::class.java)
            return enabledServices.contains(componentName.flattenToString())
        }
        
        fun isOverlayShowing(): Boolean = instance?.overlayView != null
        
        fun startOverlay() {
            instance?.showOverlay()
        }
        
        fun stopOverlay() {
            instance?.hideOverlay()
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        settingsRepository = SettingsRepository(this)
        
        Log.d(TAG, "AccessibilityService created")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        instance = null
        hideOverlay()
        cleanupReceivers()
        cleanupMediaSession()
        cleanupOrientationListener()
        serviceScope.cancel()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        IslandStateRepository.setServiceRunning(false)
        Log.d(TAG, "AccessibilityService destroyed")
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "AccessibilityService connected")
        
        // Setup event listeners
        setupChargingReceiver()
        setupMediaSessionListener()
        setupOrientationListener()  // Detect landscape to hide overlay
        
        // Collect config updates and safely update overlay position
        serviceScope.launch {
            settingsRepository.configFlow.collectLatest { config ->
                IslandStateRepository.updateConfig(config)
                // Safely update overlay position when config changes
                updateOverlayPosition(config.offsetX, config.offsetY)
            }
        }
        
        // Collect hide in landscape setting
        serviceScope.launch {
            settingsRepository.hideInLandscape.collectLatest { hide ->
                hideInLandscapeEnabled = hide
                // If we're currently in landscape and setting changed, update visibility
                if (isCurrentlyLandscape) {
                    setOverlayVisibility(!hide)
                }
            }
        }
        
        // Start overlay if overlay permission is granted
        if (Settings.canDrawOverlays(this)) {
            showOverlay()
        }
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        
        when (event.eventType) {
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> {
                handleNotificationEvent(event)
            }
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                // Only collapse when user explicitly clicks in another app
                // Removed TYPE_WINDOW_STATE_CHANGED to prevent auto-collapse during screen recording
                handleOutsideTouch(event)
            }
            else -> { /* Ignore */ }
        }
    }
    
    /**
     * Called when device configuration changes (including orientation)
     * This is more reliable than OrientationEventListener for detecting
     * when user exits games/videos back to portrait
     */
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        
        val isLandscape = newConfig.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        
        Log.d(TAG, "Configuration changed - landscape: $isLandscape")
        
        // Update state and visibility
        if (isLandscape != isCurrentlyLandscape) {
            isCurrentlyLandscape = isLandscape
            
            if (hideInLandscapeEnabled) {
                if (isLandscape) {
                    Log.d(TAG, "Entering landscape - hiding overlay")
                    setOverlayVisibility(false)
                } else {
                    Log.d(TAG, "Entering portrait - showing overlay")
                    setOverlayVisibility(true)
                }
            }
        }
    }
    
    /**
     * Handle touch outside the island - collapse if expanded
     * Only triggered by explicit user clicks, not window changes
     */
    private fun handleOutsideTouch(event: AccessibilityEvent) {
        val currentState = IslandStateRepository.uiState.value
        val eventPackage = event.packageName?.toString() ?: return
        
        // Ignore screen recorders and system UI packages
        val ignoredForCollapse = setOf(
            "com.android.systemui",
            "com.miui.screenrecorder",
            "com.samsung.android.app.screenrecorder",
            "com.google.android.apps.recorder",
            "com.heytap.screenrecorder",
            "com.coloros.screenrecorder",
            "com.vivo.screenrecorder",
            "com.asus.screenrecorder",
            "com.zte.screenrecorder"
        )
        
        if (eventPackage in ignoredForCollapse) return
        if (eventPackage == packageName) return

        // Only collapse if island is expanded and the click is from another app
        if (currentState.displayState == IslandState.EXPANDED) {
            IslandStateRepository.collapseIsland()
            updateTouchHandling(false)
        }
    }

    override fun onInterrupt() {
        // Handle interruption
    }
    
    // ==================== OVERLAY MANAGEMENT ====================
    
    private fun showOverlay() {
        if (overlayView != null || isViewAttached) return
        
        Log.d(TAG, "Showing overlay with TYPE_ACCESSIBILITY_OVERLAY")
        
        overlayParams = createWindowParams()
        
        overlayView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@CapsuleAccessibilityService)
            setViewTreeSavedStateRegistryOwner(this@CapsuleAccessibilityService)
            
            setContent {
                CapsuleEdgeTheme {
                    val uiState by IslandStateRepository.uiState.collectAsState()
                    
                    DynamicIslandOverlay(
                        uiState = uiState,
                        onTap = { handleIslandTap() },
                        onLongPress = { handleIslandLongPress() },
                        onDismiss = { IslandStateRepository.dismissCurrentEvent() },
                        onMediaAction = { action -> handleMediaAction(action) }
                    )
                }
            }
        }
        
        try {
            windowManager.addView(overlayView, overlayParams)
            isViewAttached = true
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
            IslandStateRepository.setServiceRunning(true)
            Log.d(TAG, "Overlay added successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add overlay", e)
            isViewAttached = false
            overlayView = null
            overlayParams = null
        }
    }
    
    private fun hideOverlay() {
        if (!isViewAttached) {
            overlayView = null
            overlayParams = null
            return
        }
        
        overlayView?.let {
            try {
                windowManager.removeView(it)
                Log.d(TAG, "Overlay removed")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove overlay", e)
            }
        }
        overlayView = null
        overlayParams = null
        isViewAttached = false
    }
    
    /**
     * Setup orientation listener to hide overlay in landscape mode
     * This prevents the capsule from blocking video/game content
     */
    private fun setupOrientationListener() {
        orientationListener = object : android.view.OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                
                // Use actual Configuration for more reliable detection
                val config = resources.configuration
                val isLandscape = config.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                
                // Only act if orientation state changed
                if (isLandscape != isCurrentlyLandscape) {
                    isCurrentlyLandscape = isLandscape
                    
                    // Only hide if the setting is enabled
                    if (hideInLandscapeEnabled) {
                        if (isLandscape) {
                            Log.d(TAG, "Landscape detected - hiding overlay")
                            setOverlayVisibility(false)
                        } else {
                            Log.d(TAG, "Portrait detected - showing overlay")
                            setOverlayVisibility(true)
                        }
                    }
                }
            }
        }
        
        if (orientationListener?.canDetectOrientation() == true) {
            orientationListener?.enable()
            Log.d(TAG, "Orientation listener enabled")
        }
        
        // Also check initial orientation
        checkCurrentOrientation()
    }
    
    /**
     * Check current orientation and update visibility
     */
    private fun checkCurrentOrientation() {
        val config = resources.configuration
        isCurrentlyLandscape = config.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        
        if (hideInLandscapeEnabled && isCurrentlyLandscape) {
            setOverlayVisibility(false)
        }
    }
    
    /**
     * Set overlay visibility without destroying it
     */
    private fun setOverlayVisibility(visible: Boolean) {
        overlayView?.let { view ->
            handler.post {
                view.visibility = if (visible) android.view.View.VISIBLE else android.view.View.GONE
            }
        }
    }
    
    /**
     * Cleanup orientation listener
     */
    private fun cleanupOrientationListener() {
        orientationListener?.disable()
        orientationListener = null
    }
    
    /**
     * Create window params for the overlay
     * Uses MATCH_PARENT width to allow full-width expansion to screen edges
     * WRAP_CONTENT height - Compose UI handles the sizing
     */
    private fun createWindowParams(): WindowManager.LayoutParams {
        val type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,   // Full width for edge-to-edge
            WindowManager.LayoutParams.WRAP_CONTENT,   // Height adapts to content
            type,
            // FLAG_NOT_FOCUSABLE: doesn't take keyboard focus
            // FLAG_NOT_TOUCH_MODAL: touches outside the island go to apps below
            // FLAG_LAYOUT_IN_SCREEN: layout relative to screen coordinates
            // FLAG_LAYOUT_NO_LIMITS: allows drawing above status bar
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            y = dpToPx(8) // Small offset from top - user can adjust via settings
        }
    }
    
    /**
     * Safely update the overlay position based on user settings
     * Uses safe checks to prevent crashes when view is not attached
     */
    fun updateOverlayPosition(offsetX: Float, offsetY: Float) {
        // Safety check - don't update if view not attached
        if (!isViewAttached || overlayView == null) {
            Log.w(TAG, "Cannot update position: view not attached")
            return
        }
        
        val view = overlayView ?: return
        val params = overlayParams ?: return
        
        params.x = dpToPx(offsetX.toInt())
        params.y = dpToPx(offsetY.toInt())
        
        try {
            windowManager.updateViewLayout(view, params)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "View not attached to window manager", e)
            isViewAttached = false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update overlay position: ${e.message}")
        }
    }
    
    private fun getStatusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else {
            dpToPx(24)
        }
    }
    
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
    
    /**
     * Safely update touch handling flags
     */
    fun updateTouchHandling(interceptTouch: Boolean) {
        // Safety check - don't update if view not attached
        if (!isViewAttached || overlayView == null) {
            return
        }
        
        val view = overlayView ?: return
        val params = overlayParams ?: return
        
        if (interceptTouch) {
            params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        } else {
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
        
        try {
            windowManager.updateViewLayout(view, params)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "View not attached to window manager", e)
            isViewAttached = false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update touch handling: ${e.message}")
        }
    }
    
    // ==================== NOTIFICATION HANDLING ====================
    
    private fun handleNotificationEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        val notification = event.parcelableData as? Notification ?: return
        
        if (packageName == this.packageName) return
        if (packageName in ignoredPackages) return
        if (notification.flags and Notification.FLAG_ONGOING_EVENT != 0) return
        if (isMediaNotification(notification)) return
        if (isProgressNotification(notification)) return
        
        val title = notification.extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = notification.extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        
        if (title.isEmpty() && text.isEmpty()) return
        
        val notificationKey = "$packageName:$title:$text"
        if (notificationKey in recentNotifications) return
        
        recentNotifications.add(notificationKey)
        handler.postDelayed({
            recentNotifications.remove(notificationKey)
        }, 10000)
        
        val appName = try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
        
        val appIcon: Drawable? = try {
            packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            null
        }
        
        val largeIcon = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notification.getLargeIcon()?.let { icon ->
                try {
                    val drawable = icon.loadDrawable(this)
                    (drawable as? BitmapDrawable)?.bitmap
                } catch (e: Exception) {
                    null
                }
            }
        } else {
            @Suppress("DEPRECATION")
            notification.largeIcon
        }
        
        val notificationEvent = IslandEvent.Notification(
            packageName = packageName,
            appName = appName,
            title = title,
            text = text,
            appIcon = appIcon,
            largeIcon = largeIcon
        )
        
        IslandStateRepository.pushEvent(notificationEvent)
        
        // Auto-dismiss notification after 5 seconds
        // First collapse, then dismiss after 1 more second
        serviceScope.launch {
            delay(5000) // Wait 5 seconds
            
            val currentEvent = IslandStateRepository.uiState.value.currentEvent
            if (currentEvent is IslandEvent.Notification && 
                currentEvent.timestamp == notificationEvent.timestamp) {
                // First collapse the island
                IslandStateRepository.collapseIsland()
                
                // Wait 1 second for collapse animation, then dismiss
                delay(1000)
                
                val stillSameEvent = IslandStateRepository.uiState.value.currentEvent
                if (stillSameEvent is IslandEvent.Notification && 
                    stillSameEvent.timestamp == notificationEvent.timestamp) {
                    IslandStateRepository.dismissCurrentEvent()
                }
            }
        }
    }
    
    private fun isMediaNotification(notification: Notification): Boolean {
        val extras = notification.extras ?: return false
        return extras.containsKey(Notification.EXTRA_MEDIA_SESSION) ||
                notification.category == Notification.CATEGORY_TRANSPORT ||
                notification.category == Notification.CATEGORY_SERVICE
    }
    
    private fun isProgressNotification(notification: Notification): Boolean {
        val extras = notification.extras ?: return false
        val progress = extras.getInt(Notification.EXTRA_PROGRESS, -1)
        val progressMax = extras.getInt(Notification.EXTRA_PROGRESS_MAX, -1)
        return progress >= 0 && progressMax > 0
    }
    
    // ==================== CHARGING HANDLING ====================
    
    private fun setupChargingReceiver() {
        chargingReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_POWER_CONNECTED -> handleChargingConnected()
                    Intent.ACTION_POWER_DISCONNECTED -> handleChargingDisconnected()
                }
            }
        }
        
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(chargingReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(chargingReceiver, filter)
        }
    }
    
    private fun handleChargingConnected() {
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        
        val isFastCharging = try {
            val chargingCurrent = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            kotlin.math.abs(chargingCurrent) > 1500000
        } catch (e: Exception) {
            false
        }
        
        val chargingEvent = IslandEvent.Charging(
            batteryLevel = batteryLevel,
            isCharging = true,
            isFastCharging = isFastCharging
        )
        
        // Push event and briefly expand to show charging indicator
        IslandStateRepository.pushEvent(chargingEvent)
        
        // Brief expand animation: expand wide for 4 seconds then collapse
        serviceScope.launch {
            // First, expand to EXPANDED to show the charging info clearly and wide
            IslandStateRepository.expandIsland()
            
            // Wait 4 seconds to let user see the charging animation properly
            delay(4000)
            
            // Collapse back to normal size (still showing charging indicator briefly)
            IslandStateRepository.collapseIsland()
            
            // After 2 more seconds, dismiss the charging event entirely
            delay(2000)
            val current = IslandStateRepository.uiState.value.currentEvent
            if (current is IslandEvent.Charging) {
                IslandStateRepository.dismissCurrentEvent()
            }
        }
    }
    
    private fun handleChargingDisconnected() {
        val current = IslandStateRepository.uiState.value.currentEvent
        if (current is IslandEvent.Charging) {
            IslandStateRepository.dismissCurrentEvent()
        }
    }
    
    private fun cleanupReceivers() {
        chargingReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) { /* ignore */ }
        }
        chargingReceiver = null
    }
    
    // ==================== MEDIA SESSION HANDLING ====================
    
    private fun setupMediaSessionListener() {
        try {
            mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            
            val componentName = ComponentName(this, CapsuleAccessibilityService::class.java)
            
            mediaSessionManager?.addOnActiveSessionsChangedListener({ controllers ->
                updateMediaControllers(controllers ?: emptyList())
            }, componentName)
            
            try {
                val controllers = mediaSessionManager?.getActiveSessions(componentName) ?: emptyList()
                updateMediaControllers(controllers)
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException getting active sessions", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up media session listener", e)
        }
    }
    
    private fun updateMediaControllers(controllers: List<MediaController>) {
        activeControllers.forEach { (pkg, controller) ->
            mediaCallbacks[pkg]?.let { callback ->
                try {
                    controller.unregisterCallback(callback)
                } catch (e: Exception) { /* ignore */ }
            }
        }
        activeControllers.clear()
        mediaCallbacks.clear()
        
        controllers.forEach { controller ->
            val packageName = controller.packageName
            activeControllers[packageName] = controller
            
            val callback = object : MediaController.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackState?) {
                    updateMediaState(controller)
                }
                
                override fun onMetadataChanged(metadata: MediaMetadata?) {
                    updateMediaState(controller)
                }
            }
            
            mediaCallbacks[packageName] = callback
            controller.registerCallback(callback)
            updateMediaState(controller)
        }
    }
    
    private fun updateMediaState(controller: MediaController) {
        val metadata = controller.metadata ?: return
        val playbackState = controller.playbackState ?: return
        
        val isPlaying = playbackState.state == PlaybackState.STATE_PLAYING
        
        if (!isPlaying) {
            val currentEvent = IslandStateRepository.uiState.value.currentEvent
            if (currentEvent is IslandEvent.MediaPlayback && 
                currentEvent.packageName == controller.packageName) {
                IslandStateRepository.updateMediaPlayState(false)
                stopMediaProgressPolling()
                
                // Start 1 minute timeout to dismiss media if still paused
                startMediaPauseTimeout(controller.packageName)
            }
            return
        }
        
        // Music is playing, cancel any pause timeout
        cancelMediaPauseTimeout()
        
        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) 
            ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE) 
            ?: getString(R.string.unknown_title)
            
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: getString(R.string.unknown_artist)
            
        val albumArt: Bitmap? = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
            
        val duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)
        val position = playbackState.position
        
        val mediaEvent = IslandEvent.MediaPlayback(
            packageName = controller.packageName,
            title = title,
            artist = artist,
            albumArt = albumArt,
            isPlaying = isPlaying,
            duration = duration,
            position = position
        )
        
        IslandStateRepository.pushEvent(mediaEvent)

        // Start progress polling when playing
        if (isPlaying) {
            startMediaProgressPolling()
        }
    }
    
    private fun cleanupMediaSession() {
        stopMediaProgressPolling()
        activeControllers.forEach { (pkg, controller) ->
            mediaCallbacks[pkg]?.let { callback ->
                try {
                    controller.unregisterCallback(callback)
                } catch (e: Exception) { /* ignore */ }
            }
        }
        activeControllers.clear()
        mediaCallbacks.clear()
    }
    
    // ==================== ISLAND INTERACTIONS ====================
    
    private fun handleIslandTap() {
        val currentState = IslandStateRepository.uiState.value
        
        when (currentState.displayState) {
            IslandState.COLLAPSED -> {
                if (currentState.currentEvent !is IslandEvent.Idle) {
                    IslandStateRepository.expandIsland()
                }
            }
            IslandState.COMPACT -> {
                // Expand to show full content
                IslandStateRepository.expandIsland()
            }
            IslandState.EXPANDED -> {
                when (val event = currentState.currentEvent) {
                    is IslandEvent.Notification -> {
                        // For notifications: open app and dismiss
                        openEventApp(event)
                        IslandStateRepository.dismissCurrentEvent()
                    }
                    is IslandEvent.MediaPlayback -> {
                        // For media: just collapse, don't dismiss (music still playing)
                        // User can control media from the compact/collapsed island
                        IslandStateRepository.collapseIsland()
                    }
                    else -> {
                        // For other events: just collapse
                        IslandStateRepository.collapseIsland()
                    }
                }
            }
        }
    }
    
    private fun handleIslandLongPress() {
        val currentState = IslandStateRepository.uiState.value
        
        when (currentState.displayState) {
            IslandState.COLLAPSED, IslandState.COMPACT -> {
                IslandStateRepository.expandIsland()
                updateTouchHandling(true)
            }
            IslandState.EXPANDED -> {
                IslandStateRepository.collapseIsland()
                updateTouchHandling(false)
            }
        }
    }
    
    private fun openEventApp(event: IslandEvent) {
        val packageName = when (event) {
            is IslandEvent.Notification -> event.packageName
            is IslandEvent.MediaPlayback -> event.packageName
            else -> null
        }
        
        packageName?.let { pkg ->
            try {
                val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
                launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launchIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun handleMediaAction(action: MediaAction) {
        // Get the currently active media controller
        val currentEvent = IslandStateRepository.uiState.value.currentEvent
        val packageName = (currentEvent as? IslandEvent.MediaPlayback)?.packageName

        // First try local controllers, then try MediaListenerService
        var controller = if (packageName != null) {
            activeControllers[packageName] ?: MediaListenerService.getController(packageName)
        } else {
            activeControllers.values.firstOrNull() ?: MediaListenerService.getAnyActiveController()
        }

        controller?.let {
            val transportControls = it.transportControls
            when (action) {
                MediaAction.PLAY_PAUSE -> {
                    val isPlaying = it.playbackState?.state == PlaybackState.STATE_PLAYING
                    if (isPlaying) {
                        transportControls.pause()
                    } else {
                        transportControls.play()
                    }
                }
                MediaAction.NEXT -> transportControls.skipToNext()
                MediaAction.PREVIOUS -> transportControls.skipToPrevious()
            }
        } ?: run {
            // Fallback to broadcast if no controller available
            val keyCode = when (action) {
                MediaAction.PLAY_PAUSE -> android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                MediaAction.NEXT -> android.view.KeyEvent.KEYCODE_MEDIA_NEXT
                MediaAction.PREVIOUS -> android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS
            }

            val downIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                putExtra(Intent.EXTRA_KEY_EVENT, android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keyCode))
            }
            val upIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                putExtra(Intent.EXTRA_KEY_EVENT, android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, keyCode))
            }

            sendBroadcast(downIntent)
            sendBroadcast(upIntent)
        }
    }

    /**
     * Start polling media progress every 1 second
     */
    private fun startMediaProgressPolling() {
        stopMediaProgressPolling()
        mediaProgressJob = serviceScope.launch {
            while (isActive) {
                val currentEvent = IslandStateRepository.uiState.value.currentEvent
                if (currentEvent is IslandEvent.MediaPlayback && currentEvent.isPlaying) {
                    // Try local controllers first, then MediaListenerService
                    val controller = activeControllers[currentEvent.packageName]
                        ?: MediaListenerService.getController(currentEvent.packageName)
                    controller?.playbackState?.let { state ->
                        if (state.state == PlaybackState.STATE_PLAYING) {
                            val currentPosition: Long

                            // Check if lastPositionUpdateTime is valid (not 0 and not too old)
                            val lastUpdateTime = state.lastPositionUpdateTime
                            val now = System.currentTimeMillis()

                            if (lastUpdateTime > 0 && lastUpdateTime <= now) {
                                // Calculate current position based on elapsed time
                                val timeDiff = now - lastUpdateTime
                                // Only apply time diff if it's reasonable (less than 10 seconds)
                                if (timeDiff < 10000) {
                                    val playbackSpeed = if (state.playbackSpeed > 0) state.playbackSpeed else 1f
                                    currentPosition = (state.position + (timeDiff * playbackSpeed).toLong())
                                        .coerceIn(0, currentEvent.duration)
                                } else {
                                    // Time diff too large, just use the position from state
                                    currentPosition = state.position.coerceIn(0, currentEvent.duration)
                                }
                            } else {
                                // lastPositionUpdateTime is invalid, just increment by 1 second
                                currentPosition = (currentEvent.position + 1000).coerceIn(0, currentEvent.duration)
                            }

                            IslandStateRepository.updateMediaPosition(currentPosition)
                        }
                    }
                }
                delay(1000)
            }
        }
    }

    /**
     * Stop polling media progress
     */
    private fun stopMediaProgressPolling() {
        mediaProgressJob?.cancel()
        mediaProgressJob = null
    }
    
    /**
     * Start timeout to dismiss media after 1 minute of pause
     */
    private fun startMediaPauseTimeout(packageName: String) {
        cancelMediaPauseTimeout() // Cancel any existing timeout
        
        mediaPauseTimeoutJob = serviceScope.launch {
            delay(60_000) // Wait 1 minute (60 seconds)
            
            val currentEvent = IslandStateRepository.uiState.value.currentEvent
            if (currentEvent is IslandEvent.MediaPlayback && 
                currentEvent.packageName == packageName &&
                !currentEvent.isPlaying) {
                // Still paused after 1 minute, dismiss the media event
                IslandStateRepository.dismissCurrentEvent()
            }
        }
    }
    
    /**
     * Cancel media pause timeout
     */
    private fun cancelMediaPauseTimeout() {
        mediaPauseTimeoutJob?.cancel()
        mediaPauseTimeoutJob = null
    }
}

enum class MediaAction {
    PLAY_PAUSE,
    NEXT,
    PREVIOUS
}
