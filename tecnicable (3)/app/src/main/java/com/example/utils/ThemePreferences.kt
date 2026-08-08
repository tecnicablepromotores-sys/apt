package com.example.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object ThemePreferences {
    val THEME_KEY = stringPreferencesKey("theme_mode")
}

class ThemeManager(private val context: Context) {
    val themeFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[ThemePreferences.THEME_KEY] ?: "SYSTEM" // SYSTEM, DARK, LIGHT
        }

    suspend fun setTheme(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[ThemePreferences.THEME_KEY] = mode
        }
    }
}
