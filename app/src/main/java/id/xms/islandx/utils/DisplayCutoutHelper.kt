package id.xms.islandx.utils

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.view.DisplayCutout
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import androidx.annotation.RequiresApi

/**
 * Data class untuk menyimpan informasi cutout/notch
 */
data class CutoutInfo(
    val hasCutout: Boolean = false,
    val topInset: Int = 0,
    val leftInset: Int = 0,
    val rightInset: Int = 0,
    val bottomInset: Int = 0,
    val cutoutRects: List<Rect> = emptyList(),
    val safeInsetTop: Int = 0,
    val screenWidth: Int = 0,
    val screenHeight: Int = 0
)

/**
 * Helper untuk mendeteksi dan menangani display cutout/notch
 */
object DisplayCutoutHelper {

    /**
     * Deteksi display cutout dari View
     */
    fun getCutoutInfo(view: View): CutoutInfo {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return getDefaultCutoutInfo(view.context)
        }

        val insets = view.rootWindowInsets ?: return getDefaultCutoutInfo(view.context)
        val displayCutout = insets.displayCutout ?: return CutoutInfo(
            hasCutout = false,
            screenWidth = view.context.resources.displayMetrics.widthPixels,
            screenHeight = view.context.resources.displayMetrics.heightPixels
        )

        val metrics = view.context.resources.displayMetrics

        return CutoutInfo(
            hasCutout = true,
            topInset = displayCutout.safeInsetTop,
            leftInset = displayCutout.safeInsetLeft,
            rightInset = displayCutout.safeInsetRight,
            bottomInset = displayCutout.safeInsetBottom,
            cutoutRects = displayCutout.boundingRects,
            safeInsetTop = displayCutout.safeInsetTop,
            screenWidth = metrics.widthPixels,
            screenHeight = metrics.heightPixels
        )
    }

    /**
     * Deteksi cutout dari Context (fallback)
     */
    fun getCutoutInfo(context: Context): CutoutInfo {
        return getDefaultCutoutInfo(context)
    }

    /**
     * Get default cutout info dengan screen dimensions
     */
    private fun getDefaultCutoutInfo(context: Context): CutoutInfo {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            return CutoutInfo(
                hasCutout = false,
                screenWidth = bounds.width(),
                screenHeight = bounds.height()
            )
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)
            return CutoutInfo(
                hasCutout = false,
                screenWidth = metrics.widthPixels,
                screenHeight = metrics.heightPixels
            )
        }
    }

    /**
     * Hitung posisi Y optimal untuk island berdasarkan cutout
     */
    fun calculateOptimalYPosition(
        cutoutInfo: CutoutInfo,
        userOffset: Int,
        autoAdjust: Boolean
    ): Int {
        if (!autoAdjust || !cutoutInfo.hasCutout) {
            return userOffset
        }

        // Jika ada cutout di top, posisikan island di bawah cutout
        return if (cutoutInfo.topInset > 0) {
            cutoutInfo.topInset + userOffset
        } else {
            userOffset
        }
    }

    /**
     * Hitung lebar optimal island berdasarkan cutout
     */
    fun calculateOptimalWidth(
        cutoutInfo: CutoutInfo,
        requestedWidth: Int,
        horizontalMargin: Int
    ): Int {
        // Jika tidak ada screen width info, return requested width
        if (cutoutInfo.screenWidth <= 0) {
            return requestedWidth
        }

        val availableWidth = cutoutInfo.screenWidth -
                cutoutInfo.leftInset -
                cutoutInfo.rightInset -
                (horizontalMargin * 2)

        return minOf(requestedWidth, availableWidth)
    }

    /**
     * Cek apakah island akan overlap dengan cutout
     */
    fun willOverlapCutout(
        cutoutInfo: CutoutInfo,
        islandY: Int,
        islandHeight: Int,
        islandX: Int,
        islandWidth: Int
    ): Boolean {
        if (!cutoutInfo.hasCutout) return false

        val islandRect = Rect(
            islandX,
            islandY,
            islandX + islandWidth,
            islandY + islandHeight
        )

        return cutoutInfo.cutoutRects.any { cutoutRect ->
            Rect.intersects(islandRect, cutoutRect)
        }
    }

    /**
     * Get cutout type as string for debugging
     */
    fun getCutoutTypeDescription(cutoutInfo: CutoutInfo): String {
        return when {
            !cutoutInfo.hasCutout -> "No Cutout"
            cutoutInfo.topInset > 0 && cutoutInfo.cutoutRects.size == 1 -> {
                val rect = cutoutInfo.cutoutRects.first()
                val centerX = rect.centerX()
                val screenCenter = cutoutInfo.screenWidth / 2

                when {
                    Math.abs(centerX - screenCenter) < 100 -> "Center Notch/Punch Hole"
                    centerX < screenCenter -> "Left Corner Cutout"
                    else -> "Right Corner Cutout"
                }
            }
            cutoutInfo.cutoutRects.size > 1 -> "Multiple Cutouts"
            else -> "Unknown Cutout Type"
        }
    }
}
