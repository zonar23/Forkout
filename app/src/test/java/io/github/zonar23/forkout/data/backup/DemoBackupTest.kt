package io.github.zonar23.forkout.data.backup

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.zonar23.forkout.data.db.DatabaseTest
import io.github.zonar23.forkout.data.repository.InMemoryDataStore
import io.github.zonar23.forkout.data.repository.SettingsRepository
import io.github.zonar23.forkout.data.repository.WorkoutRepository
import io.github.zonar23.forkout.data.seed.SeedExercises
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Clock

/** Keeps docs/demo/demo-backup.json (used for screenshots) importable. */
@RunWith(AndroidJUnit4::class)
class DemoBackupTest : DatabaseTest() {

    // Unit tests run with the app module as working directory.
    private val demoFile = File("../docs/demo/demo-backup.json")

    @Test
    fun demoBackup_importsCompletely() = runTest {
        val backup = BackupManager(db, SettingsRepository(InMemoryDataStore()), Clock.systemUTC())

        backup.import(backup.decode(demoFile.readText()))

        val exercises = db.exerciseDao().observeAll().first()
        assertEquals(SeedExercises.all.map { it.name }.sorted(), exercises.map { it.name }.sorted())
        val overviews = WorkoutRepository(db, Clock.systemUTC()).observeOverviews().first()
        assertEquals(3, overviews.size)
        assertEquals(listOf(12, 12, 12), overviews.map { it.completedSets })
    }
}
