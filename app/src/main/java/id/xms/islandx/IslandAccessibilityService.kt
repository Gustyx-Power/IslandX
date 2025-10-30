package id.xms.islandx

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.compositionContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.compose.runtime.Recomposer
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import id.xms.islandx.data.IslandPreferencesManager
import id.xms.islandx.ui.DynamicIslandOverlay
import id.xms.islandx.ui.theme.IslandXTheme
import id.xms.islandx.utils.CutoutInfo
import id.xms.islandx.utils.DisplayCutoutHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class IslandAccessibilityService : AccessibilityService(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    companion object {
        private const val TAG = "IslandAccessibility"
    }

    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private lateinit var viewModel: IslandViewModel
    private lateinit var preferencesManager: IslandPreferencesManager
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    private var cutoutInfo: CutoutInfo = CutoutInfo()
    private var layoutParams: WindowManager.LayoutParams? = null

    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "Broadcast received: ${intent?.action}")

            when (intent?.action) {
                NotificationListener.ACTION_NOTIFICATION_POSTED -> {
                    val appName = intent.getStringExtra(NotificationListener.EXTRA_APP_NAME) ?: return
                    val title = intent.getStringExtra(NotificationListener.EXTRA_TITLE) ?: ""
                    val text = intent.getStringExtra(NotificationListener.EXTRA_TEXT) ?: ""

                    Log.d(TAG, "Notification: app=$appName, title=$title, text=$text")
                    viewModel.showNotification(appName, title, text)
                }

                NotificationListener.ACTION_MUSIC_UPDATED -> {
                    val title = intent.getStringExtra(NotificationListener.EXTRA_TITLE) ?: "Unknown"
                    val artist = intent.getStringExtra(NotificationListener.EXTRA_ARTIST) ?: "Unknown Artist"
                    val isPlaying = intent.getBooleanExtra(NotificationListener.EXTRA_IS_PLAYING, false)

                    Log.d(TAG, "Music: title=$title, artist=$artist, playing=$isPlaying")
                    viewModel.showMusic(title, artist, isPlaying)
                }

                NotificationListener.ACTION_NOTIFICATION_REMOVED -> {
                    Log.d(TAG, "Notification removed")
                    viewModel.dismissIsland()
                }
            }
        }
    }

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val viewModelStore: ViewModelStore
        get() = store

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override fun onServiceConnected() {
        super.onServiceConnected()

        Log.d(TAG, "Service connected")

        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        viewModel = IslandViewModel()
        preferencesManager = IslandPreferencesManager(this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // Register notification receiver dengan music action
        val filter = IntentFilter().apply {
            addAction(NotificationListener.ACTION_NOTIFICATION_POSTED)
            addAction(NotificationListener.ACTION_NOTIFICATION_REMOVED)
            addAction(NotificationListener.ACTION_MUSIC_UPDATED)  // ← Tambah action music
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(notificationReceiver, filter, RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                registerReceiver(notificationReceiver, filter)
            }
            Log.d(TAG, "Receiver registered successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register receiver", e)
        }

        // Collect settings and update overlay
        serviceScope.launch {
            preferencesManager.settingsFlow.collectLatest { settings ->
                Log.d(TAG, "Settings updated: autoHideDelay=${settings.autoHideDelay}")
                viewModel.setAutoHideDelay(settings.autoHideDelay)

                if (overlayView == null) {
                    setupOverlay(settings)
                } else {
                    updateOverlay(settings)
                }
            }
        }

        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    private fun updateOverlay(settings: id.xms.islandx.data.IslandSettings) {
        overlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        setupOverlay(settings)
    }

    private fun setupOverlay(settings: id.xms.islandx.data.IslandSettings) {
        Log.d(TAG, "Setting up overlay")

        viewModel.setAutoHideDelay(settings.autoHideDelay)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        layoutParams = params

        overlayView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@IslandAccessibilityService)
            setViewTreeViewModelStoreOwner(this@IslandAccessibilityService)
            setViewTreeSavedStateRegistryOwner(this@IslandAccessibilityService)

            val recomposer = Recomposer(serviceScope.coroutineContext)
            compositionContext = recomposer

            serviceScope.launch(AndroidUiDispatcher.Main) {
                recomposer.runRecomposeAndApplyChanges()
            }

            addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        v.post {
                            cutoutInfo = DisplayCutoutHelper.getCutoutInfo(v)
                            updateIslandPosition(settings)
                        }
                    }
                }

                override fun onViewDetachedFromWindow(v: View) {}
            })

            setContent {
                IslandXTheme {
                    val state by viewModel.islandState

                    DynamicIslandOverlay(
                        state = state,
                        settings = settings,
                        cutoutInfo = cutoutInfo,
                        onIslandClick = {
                            viewModel.onIslandClick()
                        },
                        onDismiss = {
                            viewModel.dismissIsland()
                        }
                    )
                }
            }
        }

        try {
            windowManager?.addView(overlayView, params)
            Log.d(TAG, "Overlay added successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add overlay", e)
        }
    }

    private fun updateIslandPosition(settings: id.xms.islandx.data.IslandSettings) {
        layoutParams?.let { params ->
            val yOffset = DisplayCutoutHelper.calculateOptimalYPosition(
                cutoutInfo = cutoutInfo,
                userOffset = dpToPx(settings.yOffset),
                autoAdjust = settings.autoAdjustForNotch
            )

            params.y = yOffset

            try {
                overlayView?.let { view ->
                    windowManager?.updateViewLayout(view, params)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Handle accessibility events if needed
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()

        Log.d(TAG, "Service destroyed")

        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED

        try {
            unregisterReceiver(notificationReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister receiver", e)
        }

        overlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        serviceScope.cancel()
    }
}
