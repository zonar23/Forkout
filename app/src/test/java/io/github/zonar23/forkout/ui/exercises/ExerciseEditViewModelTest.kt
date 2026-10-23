package io.github.zonar23.forkout.ui.exercises

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.zonar23.forkout.MainDispatcherRule
import io.github.zonar23.forkout.data.db.DatabaseTest
import io.github.zonar23.forkout.data.db.model.ExerciseType
import io.github.zonar23.forkout.data.db.model.MuscleGroup
import io.github.zonar23.forkout.data.repository.ExerciseRepository
import io.github.zonar23.forkout.data.repository.WorkoutRepository
import io.github.zonar23.forkout.ui.navigation.ExerciseEditRoute
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock

@RunWith(AndroidJUnit4::class)
class ExerciseEditViewModelTest : DatabaseTest() {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository by lazy { ExerciseRepository(db) }

    private fun viewModel(id: Long = ExerciseEditRoute.NEW_ID) = ExerciseEditViewModel(
        SavedStateHandle(mapOf("exerciseId" to id)),
        repository,
        WorkoutRepository(db, Clock.systemUTC()),
    )

    // Room runs queries on its own executor, so tests wait for the expected state.
    private suspend fun ExerciseEditViewModel.awaitLoaded() = uiState.first { !it.isLoading }

    private suspend fun ExerciseEditViewModel.awaitDone() = uiState.first { it.isDone }

    @Test
    fun save_withBlankName_showsErrorAndDoesNotSave() = runTest {
        val vm = viewModel()

        vm.onNameChange("   ")
        vm.save()

        assertTrue(vm.uiState.value.showNameError)
        assertFalse(vm.uiState.value.isDone)
        assertTrue(repository.observeAll().first().isEmpty())
    }

    @Test
    fun save_newExercise_storesTrimmedValues() = runTest {
        val vm = viewModel()

        vm.onNameChange("  Rudern sitzend ")
        vm.onTypeSelect(ExerciseType.CABLE)
        vm.onMuscleGroupToggle(MuscleGroup.BACK)
        vm.onMuscleGroupToggle(MuscleGroup.BICEPS)
        vm.onMuscleGroupToggle(MuscleGroup.BICEPS)
        vm.onSetupNoteChange("Griff eng ")
        vm.save()
        vm.awaitDone()

        val saved = repository.observeAll().first().single()
        assertEquals("Rudern sitzend", saved.name)
        assertEquals(ExerciseType.CABLE, saved.type)
        assertEquals(setOf(MuscleGroup.BACK), saved.muscleGroups)
        assertEquals("Griff eng", saved.setupNote)
        assertFalse(saved.isBuiltIn)
    }

    @Test
    fun edit_loadsExistingAndKeepsBuiltInFlag() = runTest {
        val id = repository.create(exercise("Beinpresse").copy(isBuiltIn = true))
        val vm = viewModel(id).also { it.awaitLoaded() }

        assertEquals("Beinpresse", vm.uiState.value.name)
        assertFalse(vm.uiState.value.isNew)

        vm.onSetupNoteChange("Sitz Stufe 4")
        vm.save()
        vm.awaitDone()

        val saved = repository.get(id)!!
        assertEquals("Sitz Stufe 4", saved.setupNote)
        assertTrue(saved.isBuiltIn)
    }

    @Test
    fun confirmRemove_archivesBuiltIn_andRestoreUndoes() = runTest {
        val id = repository.create(exercise("Latzug").copy(isBuiltIn = true))
        val vm = viewModel(id).also { it.awaitLoaded() }

        vm.onRemoveClick()
        assertTrue(vm.uiState.value.showRemoveDialog)
        vm.confirmRemove()
        vm.awaitDone()

        assertTrue(repository.get(id)!!.isArchived)

        val reopened = viewModel(id).also { it.awaitLoaded() }
        assertTrue(reopened.uiState.value.isArchived)
        reopened.restore()
        reopened.uiState.first { !it.isArchived }
        assertFalse(repository.get(id)!!.isArchived)
    }

    @Test
    fun unknownId_closesScreen() = runTest {
        viewModel(id = 999).awaitDone()
    }
}
