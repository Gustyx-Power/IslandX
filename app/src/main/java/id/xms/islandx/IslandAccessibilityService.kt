package id.xms.islandx

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.compose.runtime.Recomposer
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

    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private lateinit var viewModel: IslandViewModel
    private lateinit var preferencesManager: IslandPreferencesManager
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Lifecycle components
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    private var cutoutInfo: CutoutInfo = CutoutInfo()
    private var layoutParams: WindowManager.LayoutParams? = null

    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                NotificationListener.ACTION_NOTIFICATION_POSTED -> {
                    val appName = intent.getStringExtra(NotificationListener.EXTRA_APP_NAME) ?: return
                    val title = intent.getStringExtra(NotificationListener.EXTRA_TITLE) ?: ""
                    val text = intent.getStringExtra(NotificationListener.EXTRA_TEXT) ?: ""

                    viewModel.showNotification(appName, title, text)
                }
                NotificationListener.ACTION_NOTIFICATION_REMOVED -> {
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

        // Initialize SavedStateRegistry
        savedStateRegistryController.performRestore(null)

        // Initialize lifecycle
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        viewModel = IslandViewModel()
        preferencesManager = IslandPreferencesManager(this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // Register notification receiver
        val filter = IntentFilter().apply {
            addAction(NotificationListener.ACTION_NOTIFICATION_POSTED)
            addAction(NotificationListener.ACTION_NOTIFICATION_REMOVED)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(notificationReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(notificationReceiver, filter)
        }

        // Collect settings and update overlay
        serviceScope.launch {
            preferencesManager.settingsFlow.collectLatest { settings ->
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

            // Enable drawing in cutout area for Android P+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        layoutParams = params

        overlayView = ComposeView(this).apply {
            // Set all required ViewTree owners BEFORE setContent
            setViewTreeLifecycleOwner(this@IslandAccessibilityService)
            setViewTreeViewModelStoreOwner(this@IslandAccessibilityService)
            setViewTreeSavedStateRegistryOwner(this@IslandAccessibilityService)

            // Set up Recomposer for the service context
            val recomposer = Recomposer(serviceScope.coroutineContext)
            compositionContext = recomposer

            // Launch recomposer
            serviceScope.launch(AndroidUiDispatcher.Main) {
                recomposer.runRecomposeAndApplyChanges()
            }

            // Detect cutout when view is attached
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
                    val state = viewModel.islandState.value
                    val currentSettings = settings

                    DynamicIslandOverlay(
                        state = state,
                        settings = currentSettings,
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
        } catch (e: Exception) {
            e.printStackTrace()
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
        // Handle interrupt
    }

    override fun onDestroy() {
        super.onDestroy()

        // Clean up lifecycle
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED

        try {
            unregisterReceiver(notificationReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
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
