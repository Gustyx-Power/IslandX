package id.xms.islandx.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "island_settings")

data class IslandSettings(
    val isEnabled: Boolean = false,
    val yOffset: Int = 20, // Offset dari top dalam dp
    val width: Int = 360, // Lebar island dalam dp
    val height: Int = 36, // Tinggi compact state dalam dp
    val autoAdjustForNotch: Boolean = true,
    val autoHideDelay: Float = 3f,
    val showOnLockscreen: Boolean = true,
    val horizontalMargin: Int = 16 // Margin kiri-kanan dalam dp
)

class IslandPreferencesManager(private val context: Context) {

    companion object {
        private val IS_ENABLED = booleanPreferencesKey("is_enabled")
        private val Y_OFFSET = intPreferencesKey("y_offset")
        private val WIDTH = intPreferencesKey("width")
        private val HEIGHT = intPreferencesKey("height")
        private val AUTO_ADJUST_NOTCH = booleanPreferencesKey("auto_adjust_notch")
        private val AUTO_HIDE_DELAY = floatPreferencesKey("auto_hide_delay")
        private val SHOW_ON_LOCKSCREEN = booleanPreferencesKey("show_on_lockscreen")
        private val HORIZONTAL_MARGIN = intPreferencesKey("horizontal_margin")
    }

    val settingsFlow: Flow<IslandSettings> = context.dataStore.data.map { preferences ->
        IslandSettings(
            isEnabled = preferences[IS_ENABLED] ?: false,
            yOffset = preferences[Y_OFFSET] ?: 20,
            width = preferences[WIDTH] ?: 360,
            height = preferences[HEIGHT] ?: 36,
            autoAdjustForNotch = preferences[AUTO_ADJUST_NOTCH] ?: true,
            autoHideDelay = preferences[AUTO_HIDE_DELAY] ?: 3f,
            showOnLockscreen = preferences[SHOW_ON_LOCKSCREEN] ?: true,
            horizontalMargin = preferences[HORIZONTAL_MARGIN] ?: 16
        )
    }

    suspend fun updateYOffset(offset: Int) {
        context.dataStore.edit { preferences ->
            preferences[Y_OFFSET] = offset
        }
    }

    suspend fun updateWidth(width: Int) {
        context.dataStore.edit { preferences ->
            preferences[WIDTH] = width
        }
    }

    suspend fun updateHeight(height: Int) {
        context.dataStore.edit { preferences ->
            preferences[HEIGHT] = height
        }
    }

    suspend fun updateAutoAdjustNotch(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_ADJUST_NOTCH] = enabled
        }
    }

    suspend fun updateAutoHideDelay(delay: Float) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_HIDE_DELAY] = delay
        }
    }

    suspend fun updateShowOnLockscreen(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_ON_LOCKSCREEN] = show
        }
    }

    suspend fun updateHorizontalMargin(margin: Int) {
        context.dataStore.edit { preferences ->
            preferences[HORIZONTAL_MARGIN] = margin
        }
    }
}
