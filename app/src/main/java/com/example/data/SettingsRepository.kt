package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings"
)

/**
 * Small user preferences that must survive a restart.
 *
 * The dark-mode toggle previously lived in a plain MutableStateFlow inside the ViewModel, so
 * it reset on every cold start — the setting appeared to work and then silently forgot.
 */
class SettingsRepository(context: Context) {

    private val store = context.applicationContext.settingsDataStore

    private val darkModeKey = booleanPreferencesKey("dark_mode")
    private val lastSeenActivityKey = longPreferencesKey("last_seen_activity_at")

    /** A corrupt or unreadable store should fall back to defaults, not crash the app. */
    private val preferences: Flow<Preferences> = store.data.catch { cause ->
        if (cause is IOException) emit(emptyPreferences()) else throw cause
    }

    val isDarkMode: Flow<Boolean> = preferences.map { it[darkModeKey] ?: false }

    /** Timestamp of the newest activity entry the user has already seen. */
    val lastSeenActivityAt: Flow<Long> = preferences.map { it[lastSeenActivityKey] ?: 0L }

    suspend fun setDarkMode(enabled: Boolean) {
        store.edit { it[darkModeKey] = enabled }
    }

    suspend fun markActivitySeen(timestamp: Long) {
        store.edit { current ->
            // Never move the marker backwards: an older screen resuming must not resurrect
            // notifications the user has already read.
            val existing = current[lastSeenActivityKey] ?: 0L
            if (timestamp > existing) current[lastSeenActivityKey] = timestamp
        }
    }
}
