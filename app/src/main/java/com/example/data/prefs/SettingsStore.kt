package com.example.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Three states rather than a boolean. The previous toggle was a plain
 * `MutableStateFlow(false)` held in HomeViewModel, which meant two problems: the choice was lost
 * on every process death, and there was no way to express "follow the system" -- which is the
 * correct default.
 */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {

    private val themeModeKey = stringPreferencesKey("theme_mode")

    val themeMode: Flow<ThemeMode> = context.settingsDataStore.data.map { prefs ->
        val stored = prefs[themeModeKey] ?: return@map ThemeMode.SYSTEM
        runCatching { ThemeMode.valueOf(stored) }.getOrDefault(ThemeMode.SYSTEM)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { prefs -> prefs[themeModeKey] = mode.name }
    }

    /**
     * Which revision of the seeded feed this install has. Seeding was previously guarded only by
     * "is the table empty", so anyone who had already opened the app kept the original three posts
     * forever and never saw later seed content. Bumping [CURRENT_SEED_VERSION] re-seeds; inserts
     * use REPLACE and the seed ids are stable, so re-seeding is idempotent.
     */
    val seedVersion: Flow<Int> = context.settingsDataStore.data.map { prefs ->
        prefs[seedVersionKey] ?: 0
    }

    suspend fun setSeedVersion(version: Int) {
        context.settingsDataStore.edit { prefs -> prefs[seedVersionKey] = version }
    }

    private val seedVersionKey = intPreferencesKey("seed_version")

    companion object {
        /** Bump when seedPosts() changes. 1 = the eight-post seed that introduced figures. */
        const val CURRENT_SEED_VERSION = 1
    }
}
