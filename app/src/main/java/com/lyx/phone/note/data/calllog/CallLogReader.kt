package com.lyx.phone.note.data.calllog

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import androidx.core.content.ContextCompat
import com.lyx.phone.note.data.repository.NumberNoteRepository
import com.lyx.phone.note.domain.PhoneNumberNormalizer

class CallLogReader(
    private val context: Context,
    private val repository: NumberNoteRepository
) {
    suspend fun readRecent(limit: Int = 200): CallLogResult {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            return CallLogResult.MissingPermission
        }

        val projection = arrayOf(
            CallLog.Calls.NUMBER,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.PHONE_ACCOUNT_ID
        )
        return runCatching {
            val items = mutableListOf<CallLogItem>()
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                "${CallLog.Calls.DATE} DESC"
            )?.use { cursor ->
                val numberIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                val typeIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
                val dateIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
                val durationIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION)
                val nameIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
                val simIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.PHONE_ACCOUNT_ID)
                while (cursor.moveToNext() && items.size < limit) {
                    val rawNumber = cursor.getString(numberIndex).orEmpty()
                    val normalized = PhoneNumberNormalizer.normalize(rawNumber)
                    val note = repository.findNoteForIncomingCall(normalized.normalizedNumber)
                    items += CallLogItem(
                        rawNumber = rawNumber,
                        normalizedNumber = normalized.normalizedNumber,
                        displayNumber = rawNumber.ifBlank { normalized.normalizedNumber },
                        callType = cursor.getInt(typeIndex),
                        date = cursor.getLong(dateIndex),
                        duration = cursor.getLong(durationIndex),
                        cachedName = cursor.getString(nameIndex),
                        simSlot = cursor.getString(simIndex)?.filter(Char::isDigit)?.toIntOrNull(),
                        note = note
                    )
                }
            }
            CallLogResult.Success(items)
        }.getOrElse { CallLogResult.Error(it.message ?: "读取通话记录失败") }
    }
}
