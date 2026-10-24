package io.github.zonar23.forkout.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class WeightUnit { KG, LB }

data class UserSettings(
    val amoledBlack: Boolean = false,
    val defaultRestSeconds: Int = DEFAULT_REST_SECONDS,
    val weightUnit: WeightUnit = WeightUnit.KG,
) {
    companion object {
        const val DEFAULT_REST_SECONDS = 90
    }
}

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val settings: Flow<UserSettings> = dataStore.data.map { prefs ->
        UserSettings(
            amoledBlack = prefs[Keys.AMOLED_BLACK] ?: false,
            defaultRestSeconds = prefs[Keys.DEFAULT_REST_SECONDS] ?: UserSettings.DEFAULT_REST_SECONDS,
            weightUnit = prefs[Keys.WEIGHT_UNIT]
                ?.let { name -> WeightUnit.entries.firstOrNull { it.name == name } }
                ?: WeightUnit.KG,
        )
    }

    suspend fun setAmoledBlack(enabled: Boolean) {
        dataStore.edit { it[Keys.AMOLED_BLACK] = enabled }
    }

    suspend fun setDefaultRestSeconds(seconds: Int) {
        require(seconds >= 0) { "Rest must not be negative" }
        dataStore.edit { it[Keys.DEFAULT_REST_SECONDS] = seconds }
    }

    /** Changes the default rest relative to the stored value, so fast repeated steps add up. */
    suspend fun adjustDefaultRestSeconds(deltaSeconds: Int, maxSeconds: Int) {
        dataStore.edit { prefs ->
            val current = prefs[Keys.DEFAULT_REST_SECONDS] ?: UserSettings.DEFAULT_REST_SECONDS
            prefs[Keys.DEFAULT_REST_SECONDS] = (current + deltaSeconds).coerceIn(0, maxSeconds)
        }
    }

    suspend fun setWeightUnit(unit: WeightUnit) {
        dataStore.edit { it[Keys.WEIGHT_UNIT] = unit.name }
    }

    private object Keys {
        val AMOLED_BLACK = booleanPreferencesKey("amoled_black")
        val DEFAULT_REST_SECONDS = intPreferencesKey("default_rest_seconds")
        val WEIGHT_UNIT = stringPreferencesKey("weight_unit")
    }
}
