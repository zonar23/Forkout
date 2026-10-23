package io.github.zonar23.forkout.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsRepositoryTest {

    /** In-memory DataStore; the file-based one cannot replace its file on Windows test hosts. */
    private class InMemoryDataStore : DataStore<Preferences> {
        private val state = MutableStateFlow(emptyPreferences())
        private val mutex = Mutex()

        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
            mutex.withLock { transform(state.value).also { state.value = it } }
    }

    private val repository = SettingsRepository(InMemoryDataStore())

    @Test
    fun defaults_whenNothingStored() = runTest {
        assertEquals(UserSettings(), repository.settings.first())
    }

    @Test
    fun storesAndReadsAllValues() = runTest {
        repository.setAmoledBlack(true)
        repository.setDefaultRestSeconds(120)
        repository.setWeightUnit(WeightUnit.LB)

        assertEquals(
            UserSettings(amoledBlack = true, defaultRestSeconds = 120, weightUnit = WeightUnit.LB),
            repository.settings.first(),
        )
    }
}
