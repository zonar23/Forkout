package io.github.zonar23.forkout.ui.components

import androidx.annotation.StringRes
import io.github.zonar23.forkout.R
import io.github.zonar23.forkout.data.db.model.ExerciseType
import io.github.zonar23.forkout.data.db.model.MuscleGroup

@get:StringRes
val ExerciseType.labelRes: Int
    get() = when (this) {
        ExerciseType.MACHINE -> R.string.exercise_type_machine
        ExerciseType.CABLE -> R.string.exercise_type_cable
        ExerciseType.FREE_WEIGHT -> R.string.exercise_type_free_weight
        ExerciseType.BODYWEIGHT -> R.string.exercise_type_bodyweight
    }

@get:StringRes
val MuscleGroup.labelRes: Int
    get() = when (this) {
        MuscleGroup.CHEST -> R.string.muscle_chest
        MuscleGroup.BACK -> R.string.muscle_back
        MuscleGroup.SHOULDERS -> R.string.muscle_shoulders
        MuscleGroup.BICEPS -> R.string.muscle_biceps
        MuscleGroup.TRICEPS -> R.string.muscle_triceps
        MuscleGroup.CORE -> R.string.muscle_core
        MuscleGroup.LEGS -> R.string.muscle_legs
        MuscleGroup.HAMSTRINGS -> R.string.muscle_hamstrings
        MuscleGroup.GLUTES -> R.string.muscle_glutes
        MuscleGroup.CALVES -> R.string.muscle_calves
    }
