package com.lyx.phone.note.data.calllog

import com.lyx.phone.note.data.db.NumberNoteEntity

data class CallLogItem(
    val rawNumber: String,
    val normalizedNumber: String,
    val displayNumber: String,
    val callType: Int,
    val date: Long,
    val duration: Long,
    val cachedName: String?,
    val simSlot: Int?,
    val note: NumberNoteEntity?
)

sealed interface CallLogResult {
    data class Success(val items: List<CallLogItem>) : CallLogResult
    data object MissingPermission : CallLogResult
    data class Error(val message: String) : CallLogResult
}
