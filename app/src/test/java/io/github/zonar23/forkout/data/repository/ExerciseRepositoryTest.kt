package io.github.zonar23.forkout.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.zonar23.forkout.data.db.DatabaseTest
import io.github.zonar23.forkout.data.db.entity.PlanDayEntity
import io.github.zonar23.forkout.data.db.entity.PlanEntity
import io.github.zonar23.forkout.data.db.entity.PlanExerciseEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class ExerciseRepositoryTest : DatabaseTest() {

    private val repository by lazy { ExerciseRepository(db) }

    @Test
    fun remove_deletesUnusedCustomExercise() = runTest {
        val id = repository.create(exercise("Eigene Übung"))

        assertEquals(RemoveResult.DELETED, repository.remove(id))
        assertNull(repository.observe(id).first())
    }

    @Test
    fun remove_archivesExerciseUsedInPlan() = runTest {
        val id = repository.create(exercise("Latzug"))
        val planId = db.planDao().insertPlan(PlanEntity(name = "Plan", sessionsPerWeek = 2, createdAt = Instant.now()))
        val dayId = db.planDao().insertDay(PlanDayEntity(planId = planId, name = "A", position = 0))
        db.planDao().insertPlanExercise(PlanExerciseEntity(planDayId = dayId, exerciseId = id, position = 0))

        assertEquals(RemoveResult.ARCHIVED, repository.remove(id))
        assertTrue(repository.observe(id).first()!!.isArchived)
        assertTrue(repository.observeActive().first().isEmpty())
    }

    @Test
    fun remove_archivesBuiltInExercise_andRestoreBringsItBack() = runTest {
        val id = repository.create(exercise("Beinpresse").copy(isBuiltIn = true))

        assertEquals(RemoveResult.ARCHIVED, repository.remove(id))
        repository.restore(id)

        assertFalse(repository.observe(id).first()!!.isArchived)
    }
}
