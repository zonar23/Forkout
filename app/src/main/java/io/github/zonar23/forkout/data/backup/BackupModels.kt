package io.github.zonar23.forkout.data.backup

import kotlinx.serialization.Serializable

/*
 * JSON backup format, documented in docs/BACKUP_FORMAT.md.
 * Rules: weights in grams, times as ISO-8601 instants (UTC), ids only link entries within one file.
 * Bump FORMAT_VERSION for breaking changes and keep reading older versions.
 */

@Serializable
data class BackupFile(
    val formatVersion: Int = FORMAT_VERSION,
    val app: String = APP_ID,
    val exportedAt: String,
    val settings: BackupSettings? = null,
    val exercises: List<BackupExercise> = emptyList(),
    val plans: List<BackupPlan> = emptyList(),
    val workouts: List<BackupWorkout> = emptyList(),
) {
    companion object {
        const val FORMAT_VERSION = 1
        const val APP_ID = "forkout"
    }
}

@Serializable
data class BackupSettings(
    val amoledBlack: Boolean,
    val defaultRestSeconds: Int,
    val weightUnit: String,
)

@Serializable
data class BackupExercise(
    val id: Long,
    val name: String,
    val type: String,
    val muscleGroups: List<String> = emptyList(),
    val setupNote: String = "",
    val isBuiltIn: Boolean = false,
    val isArchived: Boolean = false,
)

@Serializable
data class BackupPlan(
    val id: Long,
    val name: String,
    val goal: String = "",
    val sessionsPerWeek: Int,
    val note: String = "",
    val createdAt: String,
    val isArchived: Boolean = false,
    val days: List<BackupPlanDay> = emptyList(),
)

@Serializable
data class BackupPlanDay(
    val id: Long,
    val name: String,
    val exercises: List<BackupPlanExercise> = emptyList(),
)

@Serializable
data class BackupPlanExercise(
    val exerciseId: Long,
    val note: String = "",
    val sets: List<BackupPlanSet> = emptyList(),
)

@Serializable
data class BackupPlanSet(
    val targetReps: Int,
    val targetWeightGrams: Int,
    val restSeconds: Int,
)

@Serializable
data class BackupWorkout(
    val planDayId: Long? = null,
    val planDayName: String,
    val startedAt: String,
    val finishedAt: String? = null,
    val note: String = "",
    val sets: List<BackupWorkoutSet> = emptyList(),
)

@Serializable
data class BackupWorkoutSet(
    val exerciseId: Long,
    val exercisePosition: Int,
    val setIndex: Int,
    val targetReps: Int? = null,
    val targetWeightGrams: Int? = null,
    val restSeconds: Int,
    val actualReps: Int? = null,
    val actualWeightGrams: Int? = null,
    val rir: Int? = null,
    val completedAt: String? = null,
)
