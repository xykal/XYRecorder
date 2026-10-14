package com.xyverse.xyrecorder.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "xyrecorder_settings")

/**
 * Repository for managing app settings using DataStore
 */
class SettingsRepository(private val context: Context) {
    
    private val ds = context.dataStore
    
    // Keys
    private object Keys {
        val NR_ENABLED = booleanPreferencesKey("nr_enabled")
        val NR_PRESET = stringPreferencesKey("nr_preset")
    }
    
    /**
     * Flow of app settings
     */
    val settingsFlow: Flow<AppSettings> = ds.data.map { prefs ->
        AppSettings(
            noiseReduction = NoiseReductionSettings(
                enabled = prefs[Keys.NR_ENABLED] ?: true,
                preset = prefs[Keys.NR_PRESET] ?: "medium"
            )
        )
    }
    
    /**
     * Update noise reduction settings
     */
    suspend fun updateNoiseReduction(block: NoiseReductionSettings.() -> NoiseReductionSettings) {
        ds.edit { prefs ->
            val current = NoiseReductionSettings(
                enabled = prefs[Keys.NR_ENABLED] ?: true,
                preset = prefs[Keys.NR_PRESET] ?: "medium"
            )
            val updated = current.block()
            prefs[Keys.NR_ENABLED] = updated.enabled
            prefs[Keys.NR_PRESET] = updated.preset
        }
    }
}
