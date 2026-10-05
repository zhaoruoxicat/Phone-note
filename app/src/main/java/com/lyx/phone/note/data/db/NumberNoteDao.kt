package com.lyx.phone.note.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NumberNoteDao {
    @Query("SELECT * FROM number_notes WHERE isArchived = 0 ORDER BY updatedAt DESC")
    fun observeAllNotes(): Flow<List<NumberNoteEntity>>

    @Query("SELECT * FROM number_notes WHERE normalizedNumber = :normalizedNumber LIMIT 1")
    fun observeNoteByNumber(normalizedNumber: String): Flow<NumberNoteEntity?>

    @Query("SELECT * FROM number_notes WHERE normalizedNumber = :normalizedNumber LIMIT 1")
    suspend fun findByNormalizedNumber(normalizedNumber: String): NumberNoteEntity?

    @Query("SELECT * FROM number_notes WHERE last11Digits = :last11Digits ORDER BY updatedAt DESC LIMIT 1")
    suspend fun findByLast11Digits(last11Digits: String): NumberNoteEntity?

    @Query(
        "SELECT * FROM number_notes WHERE isArchived = 0 AND " +
            "(rawNumber LIKE '%' || :query || '%' OR normalizedNumber LIKE '%' || :query || '%' OR " +
            "displayLabel LIKE '%' || :query || '%' OR note LIKE '%' || :query || '%') " +
            "ORDER BY updatedAt DESC"
    )
    fun searchNotes(query: String): Flow<List<NumberNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NumberNoteEntity): Long

    @Update
    suspend fun update(note: NumberNoteEntity)

    @Delete
    suspend fun delete(note: NumberNoteEntity)

    @Query("UPDATE number_notes SET isArchived = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun archive(id: Long, updatedAt: Long)

    @Query("SELECT * FROM number_notes ORDER BY updatedAt DESC")
    suspend fun getAllForBackup(): List<NumberNoteEntity>
}
