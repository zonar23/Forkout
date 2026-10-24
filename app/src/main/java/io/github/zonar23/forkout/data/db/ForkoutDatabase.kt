package io.github.zonar23.forkout.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.github.zonar23.forkout.data.db.dao.BackupDao
import io.github.zonar23.forkout.data.db.dao.ExerciseDao
import io.github.zonar23.forkout.data.db.dao.PlanDao
import io.github.zonar23.forkout.data.db.dao.WorkoutDao
import io.github.zonar23.forkout.data.db.entity.ExerciseEntity
import io.github.zonar23.forkout.data.db.entity.PlanDayEntity
import io.github.zonar23.forkout.data.db.entity.PlanEntity
import io.github.zonar23.forkout.data.db.entity.PlanExerciseEntity
import io.github.zonar23.forkout.data.db.entity.PlanSetEntity
import io.github.zonar23.forkout.data.db.entity.WorkoutSessionEntity
import io.github.zonar23.forkout.data.db.entity.WorkoutSetEntity

@Database(
    entities = [
        ExerciseEntity::class,
        PlanEntity::class,
        PlanDayEntity::class,
        PlanExerciseEntity::class,
        PlanSetEntity::class,
        WorkoutSessionEntity::class,
        WorkoutSetEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class ForkoutDatabase : RoomDatabase() {

    abstract fun exerciseDao(): ExerciseDao

    abstract fun planDao(): PlanDao

    abstract fun workoutDao(): WorkoutDao

    abstract fun backupDao(): BackupDao

    companion object {
        const val NAME = "forkout.db"
    }
}
