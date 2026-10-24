package io.github.zonar23.forkout.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Which rest the user skipped. Stored instead of kept in memory, because the rest can be
 * skipped from the notification while the app is in the background or not running at all.
 */
@Singleton
class RestStateRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    /** Id of the set whose following rest was skipped, or null. */
    val skippedRestSetId: Flow<Long?> = dataStore.data.map { it[SKIPPED_REST_SET_ID] }

    suspend fun skipRestAfter(setId: Long) {
        dataStore.edit { it[SKIPPED_REST_SET_ID] = setId }
    }

    suspend fun clear() {
        dataStore.edit { it.remove(SKIPPED_REST_SET_ID) }
    }

    private companion object {
        val SKIPPED_REST_SET_ID = longPreferencesKey("skipped_rest_set_id")
    }
}
