package io.github.zonar23.forkout.data.seed

import io.github.zonar23.forkout.data.db.model.ExerciseType
import io.github.zonar23.forkout.data.db.model.ExerciseType.CABLE
import io.github.zonar23.forkout.data.db.model.ExerciseType.MACHINE
import io.github.zonar23.forkout.data.db.model.MuscleGroup
import io.github.zonar23.forkout.data.db.model.MuscleGroup.BACK
import io.github.zonar23.forkout.data.db.model.MuscleGroup.BICEPS
import io.github.zonar23.forkout.data.db.model.MuscleGroup.CHEST
import io.github.zonar23.forkout.data.db.model.MuscleGroup.CORE
import io.github.zonar23.forkout.data.db.model.MuscleGroup.GLUTES
import io.github.zonar23.forkout.data.db.model.MuscleGroup.HAMSTRINGS
import io.github.zonar23.forkout.data.db.model.MuscleGroup.LEGS
import io.github.zonar23.forkout.data.db.model.MuscleGroup.SHOULDERS
import io.github.zonar23.forkout.data.db.model.MuscleGroup.TRICEPS

data class SeedExercise(
    val name: String,
    val type: ExerciseType,
    val muscleGroups: Set<MuscleGroup>,
)

/**
 * Built-in exercises inserted on first start. Names are German by design and are stored as data,
 * not translated. Use generic names only, never manufacturer or device brand names.
 */
object SeedExercises {
    val all: List<SeedExercise> = listOf(
        SeedExercise("Kabelzug Push-Pull (stehend)", CABLE, setOf(CHEST, BACK, CORE)),
        SeedExercise("Latzug", MACHINE, setOf(BACK, BICEPS)),
        SeedExercise("Rumpfrotation", MACHINE, setOf(CORE)),
        SeedExercise("Beinpresse", MACHINE, setOf(LEGS, GLUTES)),
        SeedExercise("Beinbeuger", MACHINE, setOf(HAMSTRINGS)),
        SeedExercise("Brustpresse", MACHINE, setOf(CHEST, TRICEPS, SHOULDERS)),
        SeedExercise("Kabelzug tief mit Rotation", CABLE, setOf(CORE, BACK)),
        SeedExercise("Bauchtrainer", MACHINE, setOf(CORE)),
    )
}
