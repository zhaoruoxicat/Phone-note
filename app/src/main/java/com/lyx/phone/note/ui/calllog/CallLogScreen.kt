package com.lyx.phone.note.ui.calllog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyx.phone.note.data.calllog.CallLogItem
import com.lyx.phone.note.ui.formatCallType
import com.lyx.phone.note.ui.formatDate
import com.lyx.phone.note.ui.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallLogScreen(
    viewModel: CallLogViewModel,
    onEditNumber: (rawNumber: String, normalizedNumber: String) -> Unit,
    onRequestPermission: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val visibleItems = state.items.filter { item ->
        state.query.isBlank() ||
            item.rawNumber.contains(state.query) ||
            item.normalizedNumber.contains(state.query) ||
            item.note?.displayLabel?.contains(state.query, ignoreCase = true) == true ||
            item.note?.note?.contains(state.query, ignoreCase = true) == true
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::setQuery,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("搜索号码或备注") }
        )
        Spacer(Modifier.height(10.dp))
        state.message?.let {
            PermissionCard(message = it, onRequestPermission = onRequestPermission)
        }
        if (state.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(visibleItems) { item ->
                    CallLogRow(item = item, onClick = { onEditNumber(item.rawNumber, item.normalizedNumber) })
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(message: String, onRequestPermission: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(message, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            AssistChip(onClick = onRequestPermission, label = { Text("去授权") })
        }
    }
}

@Composable
private fun CallLogRow(item: CallLogItem, onClick: () -> Unit) {
    val formattedNumber = buildCallLogNumberText(
        rawNumber = item.rawNumber,
        normalizedNumber = item.normalizedNumber,
        weakColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
        strongColor = MaterialTheme.colorScheme.primary
    )
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            if (item.note == null) {
                Text(
                    text = formattedNumber,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Text(
                    text = item.note.displayLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = formattedNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item.note?.note?.takeIf { it.isNotBlank() }?.let {
                Text(it, maxLines = 2, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                "${formatDate(item.date)} · ${formatCallType(item.callType)} · ${formatDuration(item.duration)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun buildCallLogNumberText(
    rawNumber: String,
    normalizedNumber: String,
    weakColor: Color,
    strongColor: Color
): AnnotatedString = buildAnnotatedString {
    val raw = rawNumber.trim()
    val normalized = normalizedNumber.trim()
    val displaySource = raw.ifBlank { normalized }
    val normalizedDigits = normalized.filter(Char::isDigit)

    if (normalized.startsWith("+86") && normalizedDigits.length >= 13) {
        withStyle(SpanStyle(color = weakColor, fontSize = 12.sp, fontWeight = FontWeight.Normal)) {
            append("+86 ")
        }
        append(formatMainlandNumber(normalizedDigits.drop(2)))
        return@buildAnnotatedString
    }

    val mainlandDigits = when {
        displaySource.startsWith("+86") -> displaySource.filter(Char::isDigit).drop(2)
        normalized.startsWith("+86") -> normalizedDigits.drop(2)
        else -> displaySource.filter(Char::isDigit)
    }
    if (isMainlandLandline(mainlandDigits)) {
        withStyle(SpanStyle(color = weakColor, fontSize = 12.sp, fontWeight = FontWeight.Normal)) {
            append("+86 ")
        }
        append(formatMainlandNumber(mainlandDigits))
        return@buildAnnotatedString
    }

    val internationalSource = when {
        displaySource.startsWith("+") -> displaySource
        normalized.startsWith("+") -> normalized
        else -> null
    }
    if (internationalSource != null && !internationalSource.startsWith("+86")) {
        val split = splitInternationalCode(internationalSource)
        withStyle(SpanStyle(color = strongColor, fontWeight = FontWeight.Bold)) {
            append(split.first)
        }
        if (split.second.isNotBlank()) {
            append(" ")
            append(groupForeignSubscriber(split.second))
        }
        return@buildAnnotatedString
    }

    append(formatMainlandNumber(displaySource.filter(Char::isDigit).ifBlank { displaySource }))
}

private fun formatMainlandNumber(value: String): String {
    val digits = value.filter(Char::isDigit)
    return when {
        digits.startsWith("400") && digits.length >= 7 -> listOf(
            digits.take(3),
            digits.drop(3).take(3),
            digits.drop(6)
        ).filter { it.isNotBlank() }.joinToString(" ")
        digits.startsWith("0") && digits.length >= 10 -> {
            val areaCodeLength = if (digits.startsWith("010") || digits.startsWith("021") ||
                digits.startsWith("022") || digits.startsWith("023")
            ) 3 else 4
            "${digits.take(areaCodeLength)} ${digits.drop(areaCodeLength)}"
        }
        else -> value
    }
}

private fun isMainlandLandline(digits: String): Boolean =
    digits.startsWith("0") && digits.length >= 10

private fun splitInternationalCode(source: String): Pair<String, String> {
    val digits = source.filter(Char::isDigit)
    val code = knownCountryCodes.firstOrNull { digits.startsWith(it) } ?: digits.take(3)
    return "+$code" to digits.drop(code.length)
}

private fun groupForeignSubscriber(value: String): String =
    if (value.length <= 4) value else value.chunked(4).joinToString(" ")

private val knownCountryCodes = listOf(
    "971", "966", "965", "964", "963", "962", "961", "960",
    "886", "880", "852", "853", "855", "856",
    "421", "420", "351", "358",
    "234", "233", "230", "222", "212",
    "995", "994", "993", "992", "977", "976", "974", "973", "972", "970",
    "852", "853", "886",
    "91", "90", "86", "84", "82", "81", "66", "65", "64", "63", "62", "61", "60",
    "55", "54", "52", "49", "48", "47", "46", "45", "44", "43", "41", "39", "34",
    "33", "32", "31", "30", "27", "20", "7", "1"
)
