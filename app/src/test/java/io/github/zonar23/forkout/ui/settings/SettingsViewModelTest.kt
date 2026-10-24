package io.github.zonar23.forkout.ui.settings

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.zonar23.forkout.MainDispatcherRule
import io.github.zonar23.forkout.data.backup.BackupFileStore
import io.github.zonar23.forkout.data.backup.BackupManager
import io.github.zonar23.forkout.data.db.DatabaseTest
import io.github.zonar23.forkout.data.repository.InMemoryDataStore
import io.github.zonar23.forkout.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class SettingsViewModelTest : DatabaseTest() {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val tmp = TemporaryFolder()

    private val clock = Clock.fixed(Instant.parse("2026-10-24T09:00:00Z"), ZoneOffset.UTC)
    private val settings = SettingsRepository(InMemoryDataStore())

    private fun viewModel() = SettingsViewModel(
        settings,
        BackupManager(db, settings, clock),
        BackupFileStore(ApplicationProvider.getApplicationContext()),
        clock,
    )

    private suspend fun SettingsViewModel.awaitIdle() = uiState.first { !it.isBusy }

    @Test
    fun exportThenImport_asksForConfirmationAndRestores() = runTest {
        db.exerciseDao().insert(exercise("Beinpresse"))
        val file = tmp.newFile("backup.json")
        val uri = Uri.fromFile(file)
        val vm = viewModel()

        vm.export(uri)
        assertEquals(SettingsMessage.EXPORT_DONE, vm.uiState.first { it.message != null }.message)
        assertTrue(file.readText().contains("\"name\": \"Beinpresse\""))
        vm.dismissMessage()

        db.exerciseDao().insert(exercise("Wird ersetzt"))
        vm.readImport(uri)
        val pending = vm.uiState.first { it.pendingImport != null }.pendingImport!!
        assertEquals(1, pending.summary.exercises)
        // Nothing is replaced before the user confirms.
        assertEquals(2, db.exerciseDao().observeAll().first().size)

        vm.confirmImport()
        assertEquals(SettingsMessage.IMPORT_DONE, vm.uiState.first { it.message != null }.message)
        assertEquals(listOf("Beinpresse"), db.exerciseDao().observeAll().first().map { it.name })
    }

    @Test
    fun readImport_withInvalidFile_showsErrorAndKeepsData() = runTest {
        db.exerciseDao().insert(exercise("Beinpresse"))
        val file = tmp.newFile("notes.txt").apply { writeText("Einkaufsliste") }
        val vm = viewModel()

        vm.readImport(Uri.fromFile(file))
        val state = vm.uiState.first { it.message != null }

        assertEquals(SettingsMessage.INVALID_FILE, state.message)
        assertNull(state.pendingImport)
        assertEquals(1, db.exerciseDao().observeAll().first().size)
    }

    @Test
    fun readImport_withMissingFile_showsIoError() = runTest {
        val vm = viewModel()

        vm.readImport(Uri.fromFile(tmp.root.resolve("missing.json")))

        assertEquals(SettingsMessage.IO_ERROR, vm.uiState.first { it.message != null }.message)
    }

    @Test
    fun defaultRest_stepsWithinBounds() = runTest {
        val vm = viewModel()
        vm.uiState.first()

        vm.stepDefaultRest(SettingsViewModel.REST_STEP_SECONDS)
        assertEquals(105, vm.uiState.first { it.defaultRestSeconds != 90 }.defaultRestSeconds)

        repeat(10) { vm.stepDefaultRest(-SettingsViewModel.REST_STEP_SECONDS) }
        assertEquals(0, vm.uiState.first { it.defaultRestSeconds == 0 }.defaultRestSeconds)
        assertEquals("2026-10-24", vm.backupDate())
        vm.awaitIdle()
    }
}
