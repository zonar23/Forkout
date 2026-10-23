package io.github.zonar23.forkout.ui.workout

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.zonar23.forkout.MainDispatcherRule
import io.github.zonar23.forkout.data.db.DatabaseTest
import io.github.zonar23.forkout.data.repository.InMemoryDataStore
import io.github.zonar23.forkout.data.repository.PlanRepository
import io.github.zonar23.forkout.data.repository.SettingsRepository
import io.github.zonar23.forkout.data.repository.WorkoutRepository
import io.github.zonar23.forkout.ui.plans.PlanDayViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock

@RunWith(AndroidJUnit4::class)
class WorkoutViewModelTest : DatabaseTest() {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = Clock.systemUTC()
    private val plans by lazy { PlanRepository(db, clock) }
    private val workouts by lazy { WorkoutRepository(db, clock) }
    private var latPullId = 0L

    /** Plan day with Beinpresse (2 × 10 @ 80 kg) and Latzug (1 × 12 @ 40 kg). */
    private suspend fun planDay(): Long {
        val planId = plans.createPlan("Plan", sessionsPerWeek = 3)
        val dayId = plans.addDay(planId, "Einheit A")
        val legPress = db.exerciseDao().insert(exercise("Beinpresse"))
        latPullId = db.exerciseDao().insert(exercise("Latzug"))
        plans.addExercise(dayId, legPress, setCount = 2, targetReps = 10, targetWeightGrams = 80_000, restSeconds = 90)
        plans.addExercise(dayId, latPullId, setCount = 1, targetReps = 12, targetWeightGrams = 40_000, restSeconds = 60)
        return dayId
    }

    private suspend fun runningWorkout(): WorkoutViewModel {
        workouts.start(planDay())
        return WorkoutViewModel(workouts).also { vm -> vm.uiState.first { it.session != null } }
    }

    @Test
    fun focus_startsOnFirstOpenSet_andMovesOnAfterCompleting() = runTest {
        val vm = runningWorkout()
        val first = vm.uiState.value.exercises[0].sets[0]
        assertEquals(first.id, vm.uiState.value.focusedSetId)

        vm.toggleComplete(first)
        val state = vm.uiState.first { it.completedSets == 1 }

        assertEquals(state.exercises[0].sets[1].id, state.focusedSetId)
        assertNotNull(state.allSets.first { it.id == first.id }.completedAt)
    }

    @Test
    fun steppers_changeActualValuesOnly() = runTest {
        val vm = runningWorkout()
        val set = vm.uiState.value.exercises[0].sets[0]

        vm.stepWeight(set, WorkoutViewModel.WEIGHT_STEP_GRAMS)
        val updated = vm.uiState.first { it.allSets.first().actualWeightGrams == 82_500 }.allSets.first()
        vm.stepReps(updated, -2)
        val final = vm.uiState.first { it.allSets.first().actualReps == 8 }.allSets.first()

        assertEquals(80_000, final.targetWeightGrams)
        assertEquals(10, final.targetReps)
    }

    @Test
    fun weightInput_acceptsTypedValue() = runTest {
        val vm = runningWorkout()
        val set = vm.uiState.value.exercises[1].sets[0]

        vm.openWeightInput(set)
        assertEquals("40", vm.uiState.value.weightInput?.text)
        vm.onWeightInputChange("42,5")
        vm.confirmWeightInput()

        vm.uiState.first { state -> state.weightInput == null && state.allSets.any { it.actualWeightGrams == 42_500 } }
    }

    @Test
    fun requestFinish_withOpenSets_asksFirst_thenFinishes() = runTest {
        val vm = runningWorkout()

        vm.requestFinish()
        assertTrue(vm.uiState.value.showFinishDialog)

        vm.finish()
        val sessionId = vm.uiState.first { it.finishedSessionId != null }.finishedSessionId!!

        assertNull(workouts.observeActive().first())
        assertNotNull(workouts.observeSession(sessionId).first()!!.session.finishedAt)
        assertFalse(vm.uiState.value.isGone)
    }

    @Test
    fun confirmDiscard_removesWorkout_andClosesScreen() = runTest {
        val vm = runningWorkout()

        vm.requestDiscard()
        vm.confirmDiscard()
        vm.uiState.first { it.isGone }

        assertTrue(workouts.observeHistory().first().isEmpty())
    }

    @Test
    fun lastSets_showValuesOfPreviousWorkout() = runTest {
        val dayId = planDay()
        val firstId = workouts.start(dayId)
        val latPullSet = workouts.observeSession(firstId).first()!!.sets.first { it.exercise.id == latPullId }.set
        workouts.updateActual(latPullSet.id, reps = 11, weightGrams = 42_500)
        workouts.setCompleted(latPullSet.id, completed = true)
        workouts.finish(firstId)

        workouts.start(dayId)
        val vm = WorkoutViewModel(workouts)
        val state = vm.uiState.first { state -> state.exercises.any { it.lastSets.isNotEmpty() } }

        val last = state.exercises.first { it.exercise.id == latPullId }.lastSets.single()
        assertEquals(11, last.actualReps)
        assertEquals(42_500, last.actualWeightGrams)
        assertTrue(state.exercises.first { it.exercise.id != latPullId }.lastSets.isEmpty())
    }

    @Test
    fun startWorkout_fromPlanDay_isBlockedWhileAnotherRuns() = runTest {
        val dayId = planDay()
        val settings = SettingsRepository(InMemoryDataStore())
        val dayVm = PlanDayViewModel(SavedStateHandle(mapOf("dayId" to dayId)), plans, settings, workouts)
        dayVm.uiState.first { it.day != null }

        dayVm.startWorkout()
        dayVm.uiState.first { it.openWorkout }
        dayVm.onWorkoutOpened()
        assertNotNull(workouts.observeActive().first())

        dayVm.startWorkout()
        val blocked = dayVm.uiState.first { it.blockingWorkoutName != null }
        assertEquals("Einheit A", blocked.blockingWorkoutName)

        dayVm.resumeBlockingWorkout()
        assertTrue(dayVm.uiState.first { it.openWorkout }.openWorkout)
        // The blocked start did not create a second session.
        assertEquals(3, workouts.observeActive().first()!!.sets.size)
    }
}
