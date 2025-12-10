package id.xms.capsuleedge.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import id.xms.capsuleedge.domain.model.IslandConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "capsule_edge_settings")

class SettingsRepository(private val context: Context) {
    
    companion object {
        private val KEY_OFFSET_X = floatPreferencesKey("offset_x")
        private val KEY_OFFSET_Y = floatPreferencesKey("offset_y")
        private val KEY_SCALE = floatPreferencesKey("scale")
        private val KEY_COLLAPSED_WIDTH = floatPreferencesKey("collapsed_width")
        private val KEY_COLLAPSED_HEIGHT = floatPreferencesKey("collapsed_height")
        private val KEY_COMPACT_WIDTH = floatPreferencesKey("compact_width")
        private val KEY_COMPACT_HEIGHT = floatPreferencesKey("compact_height")
        private val KEY_EXPANDED_WIDTH = floatPreferencesKey("expanded_width")
        private val KEY_EXPANDED_HEIGHT = floatPreferencesKey("expanded_height")
        private val KEY_CORNER_RADIUS = floatPreferencesKey("corner_radius")
        private val KEY_OVERLAY_ENABLED = booleanPreferencesKey("overlay_enabled")
        private val KEY_AUTO_START = booleanPreferencesKey("auto_start")
        private val KEY_FIRST_LAUNCH = booleanPreferencesKey("first_launch")
        private val KEY_HIDE_IN_LANDSCAPE = booleanPreferencesKey("hide_in_landscape")
    }
    
    val configFlow: Flow<IslandConfig> = context.dataStore.data.map { preferences ->
        IslandConfig(
            offsetX = preferences[KEY_OFFSET_X] ?: 0f,
            offsetY = preferences[KEY_OFFSET_Y] ?: 0f,
            scale = preferences[KEY_SCALE] ?: 1f,
            collapsedWidth = preferences[KEY_COLLAPSED_WIDTH] ?: 120f,
            collapsedHeight = preferences[KEY_COLLAPSED_HEIGHT] ?: 36f,
            compactWidth = preferences[KEY_COMPACT_WIDTH] ?: 240f,
            compactHeight = preferences[KEY_COMPACT_HEIGHT] ?: 56f,
            expandedWidth = preferences[KEY_EXPANDED_WIDTH] ?: 380f,
            expandedHeight = preferences[KEY_EXPANDED_HEIGHT] ?: 220f,
            cornerRadius = preferences[KEY_CORNER_RADIUS] ?: 24f
        )
    }
    
    val isOverlayEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_OVERLAY_ENABLED] ?: true
    }
    
    val isAutoStartEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_AUTO_START] ?: true
    }
    
    val isFirstLaunch: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_FIRST_LAUNCH] ?: true
    }
    
    val hideInLandscape: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_HIDE_IN_LANDSCAPE] ?: true  // Default: hide in landscape
    }
    
    suspend fun saveConfig(config: IslandConfig) {
        context.dataStore.edit { preferences ->
            preferences[KEY_OFFSET_X] = config.offsetX
            preferences[KEY_OFFSET_Y] = config.offsetY
            preferences[KEY_SCALE] = config.scale
            preferences[KEY_COLLAPSED_WIDTH] = config.collapsedWidth
            preferences[KEY_COLLAPSED_HEIGHT] = config.collapsedHeight
            preferences[KEY_COMPACT_WIDTH] = config.compactWidth
            preferences[KEY_COMPACT_HEIGHT] = config.compactHeight
            preferences[KEY_EXPANDED_WIDTH] = config.expandedWidth
            preferences[KEY_EXPANDED_HEIGHT] = config.expandedHeight
            preferences[KEY_CORNER_RADIUS] = config.cornerRadius
        }
    }
    
    suspend fun setOverlayEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_OVERLAY_ENABLED] = enabled
        }
    }
    
    suspend fun setAutoStart(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AUTO_START] = enabled
        }
    }
    
    suspend fun setFirstLaunchComplete() {
        context.dataStore.edit { preferences ->
            preferences[KEY_FIRST_LAUNCH] = false
        }
    }
    
    suspend fun setHideInLandscape(hide: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_HIDE_IN_LANDSCAPE] = hide
        }
    }
    
    suspend fun updateOffset(x: Float, y: Float) {
        context.dataStore.edit { preferences ->
            preferences[KEY_OFFSET_X] = x
            preferences[KEY_OFFSET_Y] = y
        }
    }
    
    suspend fun updateScale(scale: Float) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SCALE] = scale.coerceIn(0.5f, 2f)
        }
    }
    
    suspend fun updateCollapsedWidth(width: Float) {
        context.dataStore.edit { preferences ->
            preferences[KEY_COLLAPSED_WIDTH] = width.coerceIn(80f, 200f)
        }
    }
}
