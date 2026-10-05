package com.lyx.phone.note.ui

import android.provider.CallLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatCallType(type: Int): String = when (type) {
    CallLog.Calls.INCOMING_TYPE -> "来电"
    CallLog.Calls.OUTGOING_TYPE -> "去电"
    CallLog.Calls.MISSED_TYPE -> "未接"
    CallLog.Calls.REJECTED_TYPE -> "已拒接"
    else -> "通话"
}

fun formatDate(time: Long): String =
    SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(time))

fun formatDuration(seconds: Long): String =
    if (seconds <= 0) "未接通" else "${seconds / 60}分${seconds % 60}秒"
