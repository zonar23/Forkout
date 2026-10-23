package io.github.zonar23.forkout.ui.exercises

import io.github.zonar23.forkout.data.db.entity.ExerciseEntity
import io.github.zonar23.forkout.data.db.model.ExerciseType
import io.github.zonar23.forkout.data.db.model.MuscleGroup
import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseFilterTest {

    private val exercises = listOf(
        ExerciseEntity(1, "Beinpresse", ExerciseType.MACHINE, setOf(MuscleGroup.LEGS, MuscleGroup.GLUTES)),
        ExerciseEntity(2, "Kabelzug tief mit Rotation", ExerciseType.CABLE, setOf(MuscleGroup.CORE, MuscleGroup.BACK)),
        ExerciseEntity(3, "Latzug", ExerciseType.MACHINE, setOf(MuscleGroup.BACK, MuscleGroup.BICEPS)),
        ExerciseEntity(4, "Alte Übung", ExerciseType.FREE_WEIGHT, setOf(MuscleGroup.BACK), isArchived = true),
    )

    private fun ids(filter: ExerciseFilter) = exercises.applyFilter(filter).map { it.id }

    @Test
    fun defaultFilter_hidesArchived() {
        assertEquals(listOf(1L, 2L, 3L), ids(ExerciseFilter()))
    }

    @Test
    fun showArchived_includesArchived() {
        assertEquals(listOf(1L, 2L, 3L, 4L), ids(ExerciseFilter(showArchived = true)))
    }

    @Test
    fun query_matchesPartOfNameIgnoringCaseAndWhitespace() {
        assertEquals(listOf(2L, 3L), ids(ExerciseFilter(query = "  ZUG ")))
    }

    @Test
    fun typeAndMuscleGroup_combine() {
        assertEquals(listOf(3L), ids(ExerciseFilter(type = ExerciseType.MACHINE, muscleGroup = MuscleGroup.BACK)))
        assertEquals(listOf(2L), ids(ExerciseFilter(type = ExerciseType.CABLE)))
        assertEquals(emptyList<Long>(), ids(ExerciseFilter(type = ExerciseType.BODYWEIGHT)))
    }
}
