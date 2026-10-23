package io.github.zonar23.forkout.ui.today

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.zonar23.forkout.MainDispatcherRule
import io.github.zonar23.forkout.data.db.DatabaseTest
import io.github.zonar23.forkout.data.repository.PlanRepository
import io.github.zonar23.forkout.data.repository.WorkoutRepository
import io.github.zonar23.forkout.ui.history.HistoryDetailViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class TodayViewModelTest : DatabaseTest() {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /** Wednesday, 21 Oct 2026; movable so workouts can lie in different weeks. */
    private var now = Instant.parse("2026-10-21T10:00:00Z")
    private val clock = object : Clock() {
        override fun instant(): Instant = now
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId?) = this
    }
    private val plans by lazy { PlanRepository(db, clock) }
    private val workouts by lazy { WorkoutRepository(db, clock) }
    private var dayA = 0L
    private var dayB = 0L

    private suspend fun setUpPlan() {
        val planId = plans.createPlan("Coach-Plan", sessionsPerWeek = 3)
        dayA = plans.addDay(planId, "Einheit A")
        dayB = plans.addDay(planId, "Einheit B")
        val exerciseId = db.exerciseDao().insert(exercise("Beinpresse"))
        plans.addExercise(dayA, exerciseId, setCount = 1, targetReps = 10, targetWeightGrams = 0, restSeconds = 60)
    }

    private suspend fun finishWorkout(dayId: Long, at: Instant): Long {
        now = at
        val id = workouts.start(dayId)
        now = at.plusSeconds(1_800)
        workouts.finish(id)
        return id
    }

    private fun viewModel() = TodayViewModel(plans, workouts, clock)

    @Test
    fun withoutPlans_hasNothingToSuggest() = runTest {
        val state = viewModel().uiState.first { !it.isLoading }

        assertNull(state.nextUp)
        assertNull(state.weekTarget)
    }

    @Test
    fun suggestsFollowingDay_andCountsOnlyThisWeek() = runTest {
        setUpPlan()
        finishWorkout(dayA, Instant.parse("2026-10-16T10:00:00Z")) // previous week (Friday)
        finishWorkout(dayA, Instant.parse("2026-10-19T10:00:00Z")) // this week (Monday)
        now = Instant.parse("2026-10-21T10:00:00Z")

        val state = viewModel().uiState.first { it.nextUp != null && it.lastWorkout != null }

        assertEquals("Einheit B", state.nextUp?.day?.name)
        assertEquals(0, state.nextExerciseCount)
        assertEquals(1, state.weekDone)
        assertEquals(3, state.weekTarget)
        assertEquals("Einheit A", state.lastWorkout?.session?.planDayName)
    }

    @Test
    fun startOrResume_startsSuggestedDay() = runTest {
        setUpPlan()
        val vm = viewModel()
        assertEquals(1, vm.uiState.first { it.nextUp != null }.nextExerciseCount)

        vm.startOrResume()
        vm.uiState.first { it.openWorkout }

        assertEquals("Einheit A", workouts.observeActive().first()?.session?.planDayName)
        assertEquals("Einheit A", vm.uiState.first { it.activeWorkoutName != null }.activeWorkoutName)
    }

    @Test
    fun historyDetail_deleteRemovesWorkout() = runTest {
        setUpPlan()
        val id = finishWorkout(dayA, now)
        val vm = HistoryDetailViewModel(SavedStateHandle(mapOf("sessionId" to id)), workouts)
        assertNotNull(vm.uiState.first { it.summary != null }.summary)

        vm.onDeleteClick()
        vm.confirmDelete()
        vm.uiState.first { it.isGone }

        assertEquals(0, workouts.observeOverviews().first().size)
    }
}
