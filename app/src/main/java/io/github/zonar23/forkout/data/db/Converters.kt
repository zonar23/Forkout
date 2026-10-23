package io.github.zonar23.forkout.data.db

import androidx.room.TypeConverter
import io.github.zonar23.forkout.data.db.model.MuscleGroup
import java.time.Instant

class Converters {

    @TypeConverter
    fun instantToMillis(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun millisToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun muscleGroupsToString(value: Set<MuscleGroup>): String = encodeMuscleGroups(value)

    @TypeConverter
    fun stringToMuscleGroups(value: String): Set<MuscleGroup> =
        value.split(SEPARATOR)
            .filter { it.isNotBlank() }
            .mapNotNull { name -> MuscleGroup.entries.firstOrNull { it.name == name } }
            .toSet()

    companion object {
        private const val SEPARATOR = ","

        /** Shared with the seed callback, which writes rows without going through Room. */
        fun encodeMuscleGroups(value: Set<MuscleGroup>): String =
            value.sortedBy { it.ordinal }.joinToString(SEPARATOR) { it.name }
    }
}
