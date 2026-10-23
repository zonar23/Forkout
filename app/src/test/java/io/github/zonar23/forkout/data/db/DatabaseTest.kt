package io.github.zonar23.forkout.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.zonar23.forkout.data.db.entity.ExerciseEntity
import io.github.zonar23.forkout.data.db.model.ExerciseType
import io.github.zonar23.forkout.data.db.model.MuscleGroup
import org.junit.After
import org.junit.Before

/** Base class for Room tests running on Robolectric with an in-memory database. */
abstract class DatabaseTest {

    protected lateinit var db: ForkoutDatabase

    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ForkoutDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    protected fun exercise(
        name: String,
        type: ExerciseType = ExerciseType.MACHINE,
        muscleGroups: Set<MuscleGroup> = setOf(MuscleGroup.CHEST),
        isArchived: Boolean = false,
    ) = ExerciseEntity(name = name, type = type, muscleGroups = muscleGroups, isArchived = isArchived)
}
