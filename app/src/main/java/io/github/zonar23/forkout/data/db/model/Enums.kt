package io.github.zonar23.forkout.data.db.model

/** Stored by name in the database. Never rename existing entries without a migration. */
enum class ExerciseType {
    MACHINE,
    CABLE,
    FREE_WEIGHT,
    BODYWEIGHT,
}

/** Fixed list of muscle groups. Stored by name; new entries can be appended freely. */
enum class MuscleGroup {
    CHEST,
    BACK,
    SHOULDERS,
    BICEPS,
    TRICEPS,
    CORE,
    LEGS,
    HAMSTRINGS,
    GLUTES,
    CALVES,
}
