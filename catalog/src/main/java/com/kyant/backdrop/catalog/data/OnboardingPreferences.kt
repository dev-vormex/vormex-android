package com.kyant.backdrop.catalog.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object OnboardingPreferences {
    private val HAS_SEEN_ONBOARDING = booleanPreferencesKey("has_seen_onboarding")
    
    /**
     * Flow that emits the current onboarding completion status.
     * Returns false if not seen or key doesn't exist, true if completed.
     */
    fun hasSeenOnboarding(context: Context): Flow<Boolean> {
        return context.settingsDataStore.data.map { preferences ->
            preferences[HAS_SEEN_ONBOARDING] ?: false
        }
    }
    
    /**
     * Marks onboarding as completed.
     */
    suspend fun setHasSeenOnboarding(context: Context, value: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[HAS_SEEN_ONBOARDING] = value
        }
    }
}
