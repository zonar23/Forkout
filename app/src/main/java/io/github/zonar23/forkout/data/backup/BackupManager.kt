package io.github.zonar23.forkout.data.backup

import androidx.room.withTransaction
import io.github.zonar23.forkout.data.db.ForkoutDatabase
import io.github.zonar23.forkout.data.db.entity.ExerciseEntity
import io.github.zonar23.forkout.data.db.entity.PlanDayEntity
import io.github.zonar23.forkout.data.db.entity.PlanEntity
import io.github.zonar23.forkout.data.db.entity.PlanExerciseEntity
import io.github.zonar23.forkout.data.db.entity.PlanSetEntity
import io.github.zonar23.forkout.data.db.entity.WorkoutSessionEntity
import io.github.zonar23.forkout.data.db.entity.WorkoutSetEntity
import io.github.zonar23.forkout.data.db.model.ExerciseType
import io.github.zonar23.forkout.data.db.model.MuscleGroup
import io.github.zonar23.forkout.data.repository.SettingsRepository
import io.github.zonar23.forkout.data.repository.WeightUnit
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.time.Clock
import java.time.Instant
import java.time.format.DateTimeParseException
import javax.inject.Inject
import javax.inject.Singleton

/** Why a backup file could not be read or imported. */
sealed class BackupException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class InvalidFile(cause: Throwable? = null) : BackupException("Not a valid Forkout backup", cause)
    class UnsupportedVersion(val version: Int) : BackupException("Unsupported backup format version $version")
}

/** Counts shown before an import replaces the current data. */
data class BackupSummary(
    val exercises: Int,
    val plans: Int,
    val workouts: Int,
)

fun BackupFile.summary() = BackupSummary(exercises.size, plans.size, workouts.size)

/**
 * Converts all app data to and from the JSON backup format.
 * Importing replaces everything in a single transaction; on any error the old data stays.
 */
@Singleton
class BackupManager @Inject constructor(
    private val db: ForkoutDatabase,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock,
) {
    private val dao = db.backupDao()

    suspend fun export(): BackupFile = db.withTransaction {
        val days = dao.getPlanDays().groupBy { it.planId }
        val planExercises = dao.getPlanExercises().groupBy { it.planDayId }
        val planSets = dao.getPlanSets().groupBy { it.planExerciseId }
        val workoutSets = dao.getWorkoutSets().groupBy { it.sessionId }
        val settings = settingsRepository.settings.first()

        BackupFile(
            exportedAt = clock.instant().toString(),
            settings = BackupSettings(
                amoledBlack = settings.amoledBlack,
                defaultRestSeconds = settings.defaultRestSeconds,
                weightUnit = settings.weightUnit.name,
            ),
            exercises = dao.getExercises().map { it.toBackup() },
            plans = dao.getPlans().map { plan ->
                plan.toBackup(
                    days = days[plan.id].orEmpty().map { day ->
                        BackupPlanDay(
                            id = day.id,
                            name = day.name,
                            exercises = planExercises[day.id].orEmpty().map { item ->
                                BackupPlanExercise(
                                    exerciseId = item.exerciseId,
                                    note = item.note,
                                    sets = planSets[item.id].orEmpty().map { it.toBackup() },
                                )
                            },
                        )
                    },
                )
            },
            workouts = dao.getSessions().map { session ->
                session.toBackup(workoutSets[session.id].orEmpty().map { it.toBackup() })
            },
        )
    }

    fun encode(file: BackupFile): String = json.encodeToString(BackupFile.serializer(), file)

    /** @throws BackupException if the text is not a readable Forkout backup. */
    fun decode(text: String): BackupFile {
        val file = try {
            json.decodeFromString(BackupFile.serializer(), text)
        } catch (e: SerializationException) {
            throw BackupException.InvalidFile(e)
        } catch (e: IllegalArgumentException) {
            throw BackupException.InvalidFile(e)
        }
        if (file.app != BackupFile.APP_ID) throw BackupException.InvalidFile()
        if (file.formatVersion > BackupFile.FORMAT_VERSION) throw BackupException.UnsupportedVersion(file.formatVersion)
        return file
    }

    /**
     * Replaces all data with the content of [file].
     * @throws BackupException.InvalidFile if entries are inconsistent, e.g. unknown exercise ids.
     */
    suspend fun import(file: BackupFile) {
        try {
            db.withTransaction {
                dao.deleteAll()
                importInto(file)
            }
        } catch (e: BackupException) {
            throw e
        } catch (e: DateTimeParseException) {
            throw BackupException.InvalidFile(e)
        } catch (e: android.database.SQLException) {
            // Broken references or duplicate ids violate the database constraints.
            throw BackupException.InvalidFile(e)
        }
        file.settings?.let { settings ->
            settingsRepository.setAmoledBlack(settings.amoledBlack)
            settingsRepository.setDefaultRestSeconds(settings.defaultRestSeconds.coerceAtLeast(0))
            WeightUnit.entries.firstOrNull { it.name == settings.weightUnit }?.let { settingsRepository.setWeightUnit(it) }
        }
    }

    /** Must run inside a transaction. */
    private suspend fun importInto(file: BackupFile) {
        dao.insertExercises(file.exercises.map { it.toEntity() })
        dao.insertPlans(file.plans.map { it.toEntity() })
        dao.insertPlanDays(
            file.plans.flatMap { plan ->
                plan.days.mapIndexed { index, day -> PlanDayEntity(day.id, plan.id, day.name, index) }
            }
        )
        file.plans.flatMap { it.days }.forEach { day ->
            day.exercises.forEachIndexed { index, item ->
                val planExerciseId = dao.insertPlanExercise(
                    PlanExerciseEntity(planDayId = day.id, exerciseId = item.exerciseId, position = index, note = item.note)
                )
                dao.insertPlanSets(
                    item.sets.mapIndexed { setIndex, set ->
                        PlanSetEntity(
                            planExerciseId = planExerciseId,
                            position = setIndex,
                            targetReps = set.targetReps,
                            targetWeightGrams = set.targetWeightGrams,
                            restSeconds = set.restSeconds,
                        )
                    }
                )
            }
        }
        val dayIds = file.plans.flatMap { plan -> plan.days.map { it.id } }.toSet()
        file.workouts.forEach { workout ->
            // A workout may outlive its plan day; keep it with the copied name only.
            val sessionId = dao.insertSession(workout.toEntity(planDayId = workout.planDayId?.takeIf { it in dayIds }))
            dao.insertWorkoutSets(workout.sets.map { it.toEntity(sessionId) })
        }
    }

    private companion object {
        val json = Json {
            prettyPrint = true
            encodeDefaults = true
            // Newer app versions may add fields; older files may lack optional ones.
            ignoreUnknownKeys = true
        }
    }
}

private fun ExerciseEntity.toBackup() = BackupExercise(
    id = id,
    name = name,
    type = type.name,
    muscleGroups = muscleGroups.sortedBy { it.ordinal }.map { it.name },
    setupNote = setupNote,
    isBuiltIn = isBuiltIn,
    isArchived = isArchived,
)

private fun BackupExercise.toEntity() = ExerciseEntity(
    id = id,
    name = name,
    type = ExerciseType.entries.firstOrNull { it.name == type } ?: throw BackupException.InvalidFile(),
    // Unknown muscle groups from newer versions are dropped instead of failing the import.
    muscleGroups = muscleGroups.mapNotNull { name -> MuscleGroup.entries.firstOrNull { it.name == name } }.toSet(),
    setupNote = setupNote,
    isBuiltIn = isBuiltIn,
    isArchived = isArchived,
)

private fun PlanEntity.toBackup(days: List<BackupPlanDay>) = BackupPlan(
    id = id,
    name = name,
    goal = goal,
    sessionsPerWeek = sessionsPerWeek,
    note = note,
    createdAt = createdAt.toString(),
    isArchived = isArchived,
    days = days,
)

private fun BackupPlan.toEntity() = PlanEntity(
    id = id,
    name = name,
    goal = goal,
    sessionsPerWeek = sessionsPerWeek,
    note = note,
    createdAt = Instant.parse(createdAt),
    isArchived = isArchived,
)

private fun PlanSetEntity.toBackup() = BackupPlanSet(targetReps, targetWeightGrams, restSeconds)

private fun WorkoutSessionEntity.toBackup(sets: List<BackupWorkoutSet>) = BackupWorkout(
    planDayId = planDayId,
    planDayName = planDayName,
    startedAt = startedAt.toString(),
    finishedAt = finishedAt?.toString(),
    note = note,
    sets = sets,
)

private fun BackupWorkout.toEntity(planDayId: Long?) = WorkoutSessionEntity(
    planDayId = planDayId,
    planDayName = planDayName,
    startedAt = Instant.parse(startedAt),
    finishedAt = finishedAt?.let(Instant::parse),
    note = note,
)

private fun WorkoutSetEntity.toBackup() = BackupWorkoutSet(
    exerciseId = exerciseId,
    exercisePosition = exercisePosition,
    setIndex = setIndex,
    targetReps = targetReps,
    targetWeightGrams = targetWeightGrams,
    restSeconds = restSeconds,
    actualReps = actualReps,
    actualWeightGrams = actualWeightGrams,
    rir = rir,
    completedAt = completedAt?.toString(),
)

private fun BackupWorkoutSet.toEntity(sessionId: Long) = WorkoutSetEntity(
    sessionId = sessionId,
    exerciseId = exerciseId,
    exercisePosition = exercisePosition,
    setIndex = setIndex,
    targetReps = targetReps,
    targetWeightGrams = targetWeightGrams,
    restSeconds = restSeconds,
    actualReps = actualReps,
    actualWeightGrams = actualWeightGrams,
    rir = rir,
    completedAt = completedAt?.let(Instant::parse),
)
