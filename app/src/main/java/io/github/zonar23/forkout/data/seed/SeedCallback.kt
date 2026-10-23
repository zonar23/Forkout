package io.github.zonar23.forkout.data.seed

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.zonar23.forkout.data.db.Converters

/**
 * Inserts [SeedExercises] when the database file is first created. Runs inside Room's
 * creation transaction, so the app never sees an empty library.
 */
class SeedCallback : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        SeedExercises.all.forEach { seed ->
            val values = ContentValues().apply {
                put("name", seed.name)
                put("type", seed.type.name)
                put("muscle_groups", Converters.encodeMuscleGroups(seed.muscleGroups))
                put("setup_note", "")
                put("is_built_in", 1)
                put("is_archived", 0)
            }
            db.insert("exercises", SQLiteDatabase.CONFLICT_ABORT, values)
        }
    }
}
