package com.lyx.phone.note.data.repository

import com.lyx.phone.note.data.db.NumberNoteDao
import com.lyx.phone.note.data.db.NumberNoteEntity
import com.lyx.phone.note.domain.PhoneNumberNormalizer
import kotlinx.coroutines.flow.Flow

interface NumberNoteRepository {
    fun observeAllNotes(): Flow<List<NumberNoteEntity>>
    fun observeNoteByNumber(normalizedNumber: String): Flow<NumberNoteEntity?>
    fun searchNotes(query: String): Flow<List<NumberNoteEntity>>
    suspend fun findNoteForIncomingCall(rawOrNormalizedNumber: String): NumberNoteEntity?
    suspend fun upsert(note: NumberNoteEntity): Long
    suspend fun delete(note: NumberNoteEntity)
    suspend fun archive(id: Long)
    suspend fun getAllForBackup(): List<NumberNoteEntity>
}

class RoomNumberNoteRepository(
    private val dao: NumberNoteDao
) : NumberNoteRepository {
    override fun observeAllNotes(): Flow<List<NumberNoteEntity>> = dao.observeAllNotes()

    override fun observeNoteByNumber(normalizedNumber: String): Flow<NumberNoteEntity?> =
        dao.observeNoteByNumber(normalizedNumber)

    override fun searchNotes(query: String): Flow<List<NumberNoteEntity>> =
        if (query.isBlank()) dao.observeAllNotes() else dao.searchNotes(query.trim())

    override suspend fun findNoteForIncomingCall(rawOrNormalizedNumber: String): NumberNoteEntity? {
        val normalized = PhoneNumberNormalizer.normalize(rawOrNormalizedNumber)
        return dao.findByNormalizedNumber(normalized.normalizedNumber)
            ?: normalized.last11Digits?.let { dao.findByLast11Digits(it) }
    }

    override suspend fun upsert(note: NumberNoteEntity): Long = dao.insert(note)

    override suspend fun delete(note: NumberNoteEntity) = dao.delete(note)

    override suspend fun archive(id: Long) = dao.archive(id, System.currentTimeMillis())

    override suspend fun getAllForBackup(): List<NumberNoteEntity> = dao.getAllForBackup()
}
