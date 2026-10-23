package io.github.zonar23.forkout.data.seed

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.zonar23.forkout.data.db.ForkoutDatabase
import io.github.zonar23.forkout.data.db.model.ExerciseType
import io.github.zonar23.forkout.data.db.model.MuscleGroup
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SeedCallbackTest {

    private lateinit var db: ForkoutDatabase

    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ForkoutDatabase::class.java,
        )
            .addCallback(SeedCallback())
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun onCreate_insertsAllSeedExercisesAsBuiltIn() = runTest {
        val exercises = db.exerciseDao().observeActive().first()

        assertEquals(SeedExercises.all.map { it.name }.sorted(), exercises.map { it.name }.sorted())
        assertTrue(exercises.all { it.isBuiltIn && !it.isArchived })
    }

    @Test
    fun onCreate_storesTypeAndMuscleGroups() = runTest {
        val bauchtrainer = db.exerciseDao().observeAll().first().single { it.name == "Bauchtrainer" }

        assertEquals(ExerciseType.MACHINE, bauchtrainer.type)
        assertEquals(setOf(MuscleGroup.CORE), bauchtrainer.muscleGroups)
    }
}
