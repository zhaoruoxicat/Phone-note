package com.lyx.phone.note

import android.app.Application
import com.lyx.phone.note.data.calllog.CallLogReader
import com.lyx.phone.note.data.db.AppDatabase
import com.lyx.phone.note.data.repository.NoteCategoryRepository
import com.lyx.phone.note.data.repository.RoomNumberNoteRepository

class LocalCallNoteApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val repository: RoomNumberNoteRepository by lazy { RoomNumberNoteRepository(database.numberNoteDao()) }
    val categoryRepository: NoteCategoryRepository by lazy { NoteCategoryRepository(database.noteCategoryDao()) }
    val callLogReader: CallLogReader by lazy { CallLogReader(this, repository) }
}
