package io.github.zonar23.forkout.data.db

import android.database.sqlite.SQLiteConstraintException
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.zonar23.forkout.data.db.entity.WorkoutSessionEntity
import io.github.zonar23.forkout.data.db.entity.WorkoutSetEntity
import io.github.zonar23.forkout.data.db.model.MuscleGroup
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class ExerciseDaoTest : DatabaseTest() {

    private val dao get() = db.exerciseDao()

    @Test
    fun observeActive_excludesArchivedAndSortsByName() = runTest {
        dao.insert(exercise("latzug"))
        dao.insert(exercise("Beinpresse"))
        dao.insert(exercise("Archiviert", isArchived = true))

        val names = dao.observeActive().first().map { it.name }

        assertEquals(listOf("Beinpresse", "latzug"), names)
        assertEquals(3, dao.observeAll().first().size)
    }

    @Test
    fun insert_storesMuscleGroups() = runTest {
        val groups = setOf(MuscleGroup.BACK, MuscleGroup.BICEPS)
        val id = dao.insert(exercise("Latzug", muscleGroups = groups))

        assertEquals(groups, dao.getById(id)?.muscleGroups)
    }

    @Test
    fun setArchived_hidesExercise() = runTest {
        val id = dao.insert(exercise("Latzug"))

        dao.setArchived(id, archived = true)

        assertTrue(dao.observeActive().first().isEmpty())
        assertTrue(dao.getById(id)!!.isArchived)
    }

    @Test
    fun exerciseWithHistory_cannotBeDeleted() = runTest {
        val id = dao.insert(exercise("Latzug"))
        val sessionId = db.workoutDao().insertSession(
            WorkoutSessionEntity(planDayId = null, planDayName = "Frei", startedAt = Instant.now())
        )
        db.workoutDao().insertSets(
            listOf(
                WorkoutSetEntity(
                    sessionId = sessionId,
                    exerciseId = id,
                    exercisePosition = 0,
                    setIndex = 0,
                    targetReps = 10,
                    targetWeightGrams = 40_000,
                    restSeconds = 90,
                    actualReps = 10,
                    actualWeightGrams = 40_000,
                )
            )
        )

        assertTrue(dao.isReferenced(id))
        assertThrows(SQLiteConstraintException::class.java) {
            kotlinx.coroutines.runBlocking { dao.delete(dao.getById(id)!!) }
        }
    }

    @Test
    fun unreferencedExercise_canBeDeleted() = runTest {
        val id = dao.insert(exercise("Latzug"))

        assertFalse(dao.isReferenced(id))
        dao.delete(dao.getById(id)!!)

        assertTrue(dao.observeAll().first().isEmpty())
    }
}
