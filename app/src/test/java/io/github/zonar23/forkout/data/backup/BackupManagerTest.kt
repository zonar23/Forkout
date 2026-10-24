package io.github.zonar23.forkout.data.backup

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.zonar23.forkout.data.db.DatabaseTest
import io.github.zonar23.forkout.data.db.model.ExerciseType
import io.github.zonar23.forkout.data.db.model.MuscleGroup
import io.github.zonar23.forkout.data.repository.InMemoryDataStore
import io.github.zonar23.forkout.data.repository.PlanRepository
import io.github.zonar23.forkout.data.repository.SettingsRepository
import io.github.zonar23.forkout.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class BackupManagerTest : DatabaseTest() {

    private val clock = Clock.fixed(Instant.parse("2026-10-24T09:00:00Z"), ZoneOffset.UTC)
    private val settings = SettingsRepository(InMemoryDataStore())
    private val plans by lazy { PlanRepository(db, clock) }
    private val workouts by lazy { WorkoutRepository(db, clock) }
    private val backup by lazy { BackupManager(db, settings, clock) }

    /** Plan with two days, an archived exercise, a finished workout and custom settings. */
    private suspend fun createData() {
        val legPress = db.exerciseDao().insert(
            exercise("Beinpresse", muscleGroups = setOf(MuscleGroup.LEGS, MuscleGroup.GLUTES)).copy(setupNote = "Sitz 4")
        )
        val latPull = db.exerciseDao().insert(exercise("Latzug", type = ExerciseType.CABLE))
        db.exerciseDao().insert(exercise("Alt", isArchived = true))
        val planId = plans.createPlan("Coach-Plan", sessionsPerWeek = 3, goal = "Muskelaufbau")
        val dayA = plans.addDay(planId, "Einheit A")
        plans.addDay(planId, "Einheit B")
        plans.addExercise(dayA, legPress, setCount = 3, targetReps = 12, targetWeightGrams = 82_500, restSeconds = 90)
        plans.addExercise(dayA, latPull, setCount = 2, targetReps = 10, targetWeightGrams = 40_000, restSeconds = 60)

        val sessionId = workouts.start(dayA)
        val firstSet = workouts.observeSession(sessionId).first()!!.sets.first().set
        workouts.updateActual(firstSet.id, reps = 11, weightGrams = 85_000)
        workouts.setCompleted(firstSet.id, completed = true)
        workouts.finish(sessionId)

        settings.setAmoledBlack(true)
        settings.setDefaultRestSeconds(120)
    }

    private fun BackupFile.withoutTimestamp() = copy(exportedAt = "")

    @Test
    fun exportThenImport_restoresIdenticalData() = runTest {
        createData()
        val exported = backup.export()

        // Change everything, then restore from the JSON text.
        db.exerciseDao().insert(exercise("Neu"))
        plans.deletePlan(plans.observePlans().first().single())
        settings.setAmoledBlack(false)
        backup.import(backup.decode(backup.encode(exported)))

        assertEquals(exported.withoutTimestamp(), backup.export().withoutTimestamp())
        assertTrue(settings.settings.first().amoledBlack)
        assertEquals(120, settings.settings.first().defaultRestSeconds)
    }

    @Test
    fun export_containsAllContent() = runTest {
        createData()

        val file = backup.export()

        assertEquals(BackupFile.FORMAT_VERSION, file.formatVersion)
        assertEquals("2026-10-24T09:00:00Z", file.exportedAt)
        assertEquals(3, file.exercises.size)
        assertEquals(listOf("LEGS", "GLUTES"), file.exercises.first { it.name == "Beinpresse" }.muscleGroups)
        val plan = file.plans.single()
        assertEquals(listOf("Einheit A", "Einheit B"), plan.days.map { it.name })
        assertEquals(listOf(3, 2), plan.days[0].exercises.map { it.sets.size })
        assertEquals(82_500, plan.days[0].exercises[0].sets[0].targetWeightGrams)
        val workout = file.workouts.single()
        assertEquals(85_000, workout.sets.first().actualWeightGrams)
        assertEquals(5, workout.sets.size)
    }

    @Test
    fun import_replacesExistingData() = runTest {
        createData()
        val small = BackupFile(
            exportedAt = "2026-01-01T00:00:00Z",
            exercises = listOf(BackupExercise(id = 1, name = "Kniebeuge", type = "FREE_WEIGHT", muscleGroups = listOf("LEGS"))),
        )

        backup.import(small)

        assertEquals(listOf("Kniebeuge"), db.exerciseDao().observeAll().first().map { it.name })
        assertTrue(plans.observePlans().first().isEmpty())
        assertTrue(workouts.observeOverviews().first().isEmpty())
    }

    @Test
    fun decode_rejectsInvalidForeignAndFutureFiles() {
        assertThrows(BackupException.InvalidFile::class.java) { backup.decode("not json") }
        assertThrows(BackupException.InvalidFile::class.java) {
            backup.decode("""{"app":"other","exportedAt":"2026-01-01T00:00:00Z"}""")
        }
        val future = assertThrows(BackupException.UnsupportedVersion::class.java) {
            backup.decode("""{"formatVersion":99,"app":"forkout","exportedAt":"2026-01-01T00:00:00Z"}""")
        }
        assertEquals(99, future.version)
    }

    @Test
    fun decode_ignoresUnknownFields() {
        val file = backup.decode(
            """{"formatVersion":1,"app":"forkout","exportedAt":"2026-01-01T00:00:00Z","futureField":true}"""
        )

        assertTrue(file.exercises.isEmpty())
    }

    @Test
    fun brokenReference_failsAndKeepsOldData() = runTest {
        createData()
        val before = backup.export()
        val broken = BackupFile(
            exportedAt = "2026-01-01T00:00:00Z",
            plans = listOf(
                BackupPlan(
                    id = 1, name = "Plan", sessionsPerWeek = 2, createdAt = "2026-01-01T00:00:00Z",
                    days = listOf(BackupPlanDay(id = 1, name = "A", exercises = listOf(BackupPlanExercise(exerciseId = 42)))),
                ),
            ),
        )

        assertThrows(BackupException.InvalidFile::class.java) {
            kotlinx.coroutines.runBlocking { backup.import(broken) }
        }

        assertEquals(before.withoutTimestamp(), backup.export().withoutTimestamp())
    }

    @Test
    fun workoutWithUnknownDay_isKeptWithoutLink() = runTest {
        val file = BackupFile(
            exportedAt = "2026-01-01T00:00:00Z",
            workouts = listOf(
                BackupWorkout(
                    planDayId = 7,
                    planDayName = "Gelöschte Einheit",
                    startedAt = "2026-01-01T10:00:00Z",
                    finishedAt = "2026-01-01T11:00:00Z",
                ),
            ),
        )

        backup.import(file)

        val session = workouts.observeOverviews().first().single().session
        assertNull(session.planDayId)
        assertEquals("Gelöschte Einheit", session.planDayName)
    }
}
