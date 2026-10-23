package io.github.zonar23.forkout.ui.plans

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.zonar23.forkout.MainDispatcherRule
import io.github.zonar23.forkout.data.db.DatabaseTest
import io.github.zonar23.forkout.data.repository.PlanRepository
import io.github.zonar23.forkout.ui.navigation.PlanEditRoute
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock

@RunWith(AndroidJUnit4::class)
class PlanEditViewModelTest : DatabaseTest() {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository by lazy { PlanRepository(db, Clock.systemUTC()) }

    private fun viewModel(id: Long = PlanEditRoute.NEW_ID) =
        PlanEditViewModel(SavedStateHandle(mapOf("planId" to id)), repository)

    private suspend fun PlanEditViewModel.awaitResult() = uiState.first { it.result != null }.result

    @Test
    fun create_savesTrimmedValuesAndReturnsNewId() = runTest {
        val vm = viewModel()

        vm.onNameChange(" Coach-Plan ")
        vm.onGoalChange("Muskelaufbau ")
        vm.onSessionsChange(+1)
        vm.save()

        val result = vm.awaitResult() as PlanEditResult.Created
        val plan = repository.getPlan(result.planId)!!
        assertEquals("Coach-Plan", plan.name)
        assertEquals("Muskelaufbau", plan.goal)
        assertEquals(4, plan.sessionsPerWeek)
    }

    @Test
    fun sessionsPerWeek_staysBetweenOneAndSeven() = runTest {
        val vm = viewModel()

        repeat(10) { vm.onSessionsChange(-1) }
        assertEquals(1, vm.uiState.value.sessionsPerWeek)
        repeat(10) { vm.onSessionsChange(+1) }
        assertEquals(7, vm.uiState.value.sessionsPerWeek)
    }

    @Test
    fun blankName_showsError() = runTest {
        val vm = viewModel()

        vm.save()

        assertTrue(vm.uiState.value.showNameError)
        assertNull(vm.uiState.value.result)
    }

    @Test
    fun edit_updatesExistingPlan_andDeleteRemovesIt() = runTest {
        val id = repository.createPlan("Alt", sessionsPerWeek = 2)
        val vm = viewModel(id).also { vm -> vm.uiState.first { !it.isLoading } }
        assertEquals("Alt", vm.uiState.value.name)

        vm.onNameChange("Neu")
        vm.save()
        assertEquals(PlanEditResult.Saved, vm.awaitResult())
        assertEquals("Neu", repository.getPlan(id)!!.name)

        val deleting = viewModel(id).also { vm2 -> vm2.uiState.first { !it.isLoading } }
        deleting.onDeleteClick()
        deleting.confirmDelete()
        assertEquals(PlanEditResult.Deleted, deleting.awaitResult())
        assertNull(repository.getPlan(id))
    }
}
