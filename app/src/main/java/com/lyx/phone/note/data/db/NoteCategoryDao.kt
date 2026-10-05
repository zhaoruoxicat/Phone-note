package com.lyx.phone.note.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteCategoryDao {
    @Query("SELECT * FROM note_categories ORDER BY sortOrder ASC, createdAt ASC")
    fun observeCategories(): Flow<List<NoteCategoryEntity>>

    @Query("SELECT COUNT(*) FROM note_categories")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: NoteCategoryEntity): Long

    @Update
    suspend fun update(category: NoteCategoryEntity)

    @Query("DELETE FROM note_categories WHERE id = :id")
    suspend fun delete(id: Long)
}
