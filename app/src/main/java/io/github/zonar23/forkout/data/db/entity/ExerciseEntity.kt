package io.github.zonar23.forkout.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.zonar23.forkout.data.db.model.ExerciseType
import io.github.zonar23.forkout.data.db.model.MuscleGroup

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: ExerciseType,
    @ColumnInfo(name = "muscle_groups") val muscleGroups: Set<MuscleGroup>,
    /** Machine setup shown prominently during a workout, e.g. "Seat level 4". */
    @ColumnInfo(name = "setup_note") val setupNote: String = "",
    @ColumnInfo(name = "is_built_in") val isBuiltIn: Boolean = false,
    @ColumnInfo(name = "is_archived") val isArchived: Boolean = false,
)
