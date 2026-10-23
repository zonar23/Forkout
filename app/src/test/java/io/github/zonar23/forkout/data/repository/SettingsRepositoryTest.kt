package io.github.zonar23.forkout.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsRepositoryTest {

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
