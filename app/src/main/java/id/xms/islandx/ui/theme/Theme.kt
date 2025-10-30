package id.xms.islandx.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = ExpressivePurple80,
    onPrimary = ExpressivePurple20,
    primaryContainer = ExpressivePurple30,
    onPrimaryContainer = ExpressivePurple90,
    secondary = ExpressiveTeal80,
    onSecondary = ExpressiveTeal20,
    secondaryContainer = ExpressiveTeal30,
    onSecondaryContainer = ExpressiveTeal90,
    tertiary = ExpressiveOrange80,
    onTertiary = ExpressiveOrange20,
    tertiaryContainer = ExpressiveOrange30,
    onTertiaryContainer = ExpressiveOrange90,
    error = ErrorRed80,
    errorContainer = ErrorRed30,
    onError = ErrorRed20,
    onErrorContainer = ErrorRed90,
    background = NeutralGray10,
    onBackground = NeutralGray90,
    surface = NeutralGray10,
    onSurface = NeutralGray90,
    surfaceVariant = NeutralGray30,
    onSurfaceVariant = NeutralGray80,
    outline = NeutralGray60,
    inverseOnSurface = NeutralGray10,
    inverseSurface = NeutralGray90,
    inversePrimary = ExpressivePurple40,
    surfaceTint = ExpressivePurple80,
    outlineVariant = NeutralGray30,
    scrim = NeutralGray0
)

private val LightColorScheme = lightColorScheme(
    primary = ExpressivePurple40,
    onPrimary = ExpressivePurple100,
    primaryContainer = ExpressivePurple90,
    onPrimaryContainer = ExpressivePurple10,
    secondary = ExpressiveTeal40,
    onSecondary = ExpressiveTeal100,
    secondaryContainer = ExpressiveTeal90,
    onSecondaryContainer = ExpressiveTeal10,
    tertiary = ExpressiveOrange40,
    onTertiary = ExpressiveOrange100,
    tertiaryContainer = ExpressiveOrange90,
    onTertiaryContainer = ExpressiveOrange10,
    error = ErrorRed40,
    errorContainer = ErrorRed90,
    onError = ErrorRed100,
    onErrorContainer = ErrorRed10,
    background = NeutralGray99,
    onBackground = NeutralGray10,
    surface = NeutralGray99,
    onSurface = NeutralGray10,
    surfaceVariant = NeutralGray90,
    onSurfaceVariant = NeutralGray30,
    outline = NeutralGray50,
    inverseOnSurface = NeutralGray95,
    inverseSurface = NeutralGray20,
    inversePrimary = ExpressivePurple80,
    surfaceTint = ExpressivePurple40,
    outlineVariant = NeutralGray80,
    scrim = NeutralGray0
)

@Composable
fun IslandXTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            // Hanya set window jika context adalah Activity
            val context = view.context
            if (context is Activity) {
                val window = context.window
                window.statusBarColor = colorScheme.primary.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
            // Jika bukan Activity (misalnya Service), skip window configuration
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ExpressiveTypography,
        shapes = ExpressiveShapes,
        content = content
    )
}
