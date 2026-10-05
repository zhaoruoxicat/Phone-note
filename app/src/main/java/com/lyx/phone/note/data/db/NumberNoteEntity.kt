package com.lyx.phone.note.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "number_notes",
    indices = [
        Index(value = ["normalizedNumber"], unique = true),
        Index(value = ["last11Digits"]),
        Index(value = ["category"]),
        Index(value = ["updatedAt"])
    ]
)
data class NumberNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val rawNumber: String,
    val normalizedNumber: String,
    val last11Digits: String?,
    val displayLabel: String,
    val note: String,
    val category: String,
    val importance: String,
    val enableIncomingPopup: Boolean = true,
    val rejectIncomingCall: Boolean = false,
    val lastCallTime: Long? = null,
    val lastCallType: Int? = null,
    val callCount: Int = 0,
    val simSlot: Int? = null,
    val isArchived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)
