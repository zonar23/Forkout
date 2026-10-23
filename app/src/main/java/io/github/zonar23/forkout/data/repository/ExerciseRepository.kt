package io.github.zonar23.forkout.data.repository

import androidx.room.withTransaction
import io.github.zonar23.forkout.data.db.ForkoutDatabase
import io.github.zonar23.forkout.data.db.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

enum class RemoveResult { DELETED, ARCHIVED }

@Singleton
class ExerciseRepository @Inject constructor(
    private val db: ForkoutDatabase,
) {
    private val dao = db.exerciseDao()

    fun observeActive(): Flow<List<ExerciseEntity>> = dao.observeActive()

    fun observeAll(): Flow<List<ExerciseEntity>> = dao.observeAll()

    fun observe(id: Long): Flow<ExerciseEntity?> = dao.observeById(id)

    suspend fun get(id: Long): ExerciseEntity? = dao.getById(id)

    suspend fun create(exercise: ExerciseEntity): Long = dao.insert(exercise.copy(id = 0))

    suspend fun update(exercise: ExerciseEntity) = dao.update(exercise)

    /**
     * Deletes the exercise if nothing refers to it, otherwise archives it so that plans and
     * history stay intact. Built-in exercises are always archived so they can be restored.
     */
    suspend fun remove(id: Long): RemoveResult = db.withTransaction {
        val exercise = dao.getById(id) ?: return@withTransaction RemoveResult.DELETED
        if (exercise.isBuiltIn || dao.isReferenced(id)) {
            dao.setArchived(id, archived = true)
            RemoveResult.ARCHIVED
        } else {
            dao.delete(exercise)
            RemoveResult.DELETED
        }
    }

    suspend fun restore(id: Long) = dao.setArchived(id, archived = false)
}
