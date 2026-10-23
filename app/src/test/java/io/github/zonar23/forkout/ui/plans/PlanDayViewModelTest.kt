package io.github.zonar23.forkout.ui.plans

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.zonar23.forkout.MainDispatcherRule
import io.github.zonar23.forkout.data.db.DatabaseTest
import io.github.zonar23.forkout.data.repository.ExerciseRepository
import io.github.zonar23.forkout.data.repository.InMemoryDataStore
import io.github.zonar23.forkout.data.repository.PlanRepository
import io.github.zonar23.forkout.data.repository.SettingsRepository
import io.github.zonar23.forkout.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock

@RunWith(AndroidJUnit4::class)
class PlanDayViewModelTest : DatabaseTest() {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val plans by lazy { PlanRepository(db, Clock.systemUTC()) }
    private val settings = SettingsRepository(InMemoryDataStore())
    private val workouts by lazy { WorkoutRepository(db, Clock.systemUTC()) }

    private suspend fun dayWithLegPress(): Long {
        val planId = plans.createPlan("Plan", sessionsPerWeek = 3)
        val dayId = plans.addDay(planId, "Einheit A")
        val exerciseId = db.exerciseDao().insert(exercise("Beinpresse"))
        plans.addExercise(dayId, exerciseId, setCount = 3, targetReps = 12, targetWeightGrams = 80_000, restSeconds = 90)
        return dayId
    }

    private fun dayViewModel(dayId: Long) =
        PlanDayViewModel(SavedStateHandle(mapOf("dayId" to dayId)), plans, settings, workouts)

    /** Keeps uiState collected, since it only updates while subscribed. */
    private suspend fun PlanDayViewModel.loaded() = uiState.first { it.day != null && it.exercises.isNotEmpty() }

    @Test
    fun setEditor_stepsAndValidatesValues() = runTest {
        val vm = dayViewModel(dayWithLegPress())
        val set = vm.loaded().exercises[0].sets[0]

        vm.openSet(set, setNumber = 1, setCount = 3)
        vm.stepWeight(2_500)
        vm.stepReps(-2)
        vm.stepRest(15)

        val editor = vm.uiState.value.setEditor!!
        assertEquals("82,5", editor.weightText)
        assertEquals(10, editor.reps)
        assertEquals(105, editor.restSeconds)
        assertTrue(editor.isValid)

        vm.onWeightChange("abc")
        assertFalse(vm.uiState.value.setEditor!!.isValid)
    }

    @Test
    fun saveSet_withApplyToAll_updatesEverySet() = runTest {
        val dayId = dayWithLegPress()
        val vm = dayViewModel(dayId)
        val set = vm.loaded().exercises[0].sets[1]

        vm.openSet(set, setNumber = 2, setCount = 3)
        vm.onWeightChange("85")
        vm.onApplyToAllChange(true)
        vm.saveSet()
        vm.uiState.first { it.setEditor == null }

        val sets = plans.observeDay(dayId).first()!!.exercises[0].sets
        assertEquals(listOf(85_000, 85_000, 85_000), sets.map { it.targetWeightGrams })
    }

    @Test
    fun saveSet_withoutApplyToAll_updatesOnlyThatSet() = runTest {
        val dayId = dayWithLegPress()
        val vm = dayViewModel(dayId)
        val set = vm.loaded().exercises[0].sets[2]

        vm.openSet(set, setNumber = 3, setCount = 3)
        vm.onRepsChange("8")
        vm.saveSet()
        vm.uiState.first { it.setEditor == null }

        val sets = plans.observeDay(dayId).first()!!.exercises[0].sets
        assertEquals(listOf(12, 12, 8), sets.map { it.targetReps })
    }

    @Test
    fun deleteSet_fromEditor_removesIt() = runTest {
        val dayId = dayWithLegPress()
        val vm = dayViewModel(dayId)
        val set = vm.loaded().exercises[0].sets[0]

        vm.openSet(set, setNumber = 1, setCount = 3)
        vm.deleteSet()
        vm.uiState.first { it.setEditor == null && it.exercises.firstOrNull()?.sets?.size == 2 }

        assertNull(vm.uiState.value.setEditor)
    }

    @Test
    fun picker_addsExerciseWithDefaultSetsAndRestFromSettings() = runTest {
        settings.setDefaultRestSeconds(120)
        val planId = plans.createPlan("Plan", sessionsPerWeek = 3)
        val dayId = plans.addDay(planId, "Einheit A")
        val exerciseId = db.exerciseDao().insert(exercise("Latzug"))
        val picker = ExercisePickerViewModel(
            SavedStateHandle(mapOf("dayId" to dayId)),
            ExerciseRepository(db),
            plans,
            settings,
        )

        picker.pick(exerciseId)
        picker.uiState.first { it.isDone }

        val added = plans.observeDay(dayId).first()!!.exercises.single()
        assertEquals("Latzug", added.exercise.name)
        assertEquals(List(PlanDayViewModel.DEFAULT_SETS) { 120 }, added.sets.map { it.restSeconds })
        assertEquals(List(PlanDayViewModel.DEFAULT_SETS) { PlanDayViewModel.DEFAULT_REPS }, added.sets.map { it.targetReps })
    }

    @Test
    fun nextDayLabel_usesLettersThenNumbers() {
        assertEquals("A", nextDayLabel(0))
        assertEquals("C", nextDayLabel(2))
        assertEquals("Z", nextDayLabel(25))
        assertEquals("27", nextDayLabel(26))
    }
}
