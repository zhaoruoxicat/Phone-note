package com.lyx.phone.note.ui.notes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lyx.phone.note.data.db.NumberNoteEntity
import com.lyx.phone.note.domain.CategoryDisplay
import com.lyx.phone.note.domain.NoteImportance

@Composable
fun NotesScreen(
    viewModel: NotesViewModel,
    onEdit: (String) -> Unit
) {
    val query by viewModel.query.collectAsState()
    val notes by viewModel.notes.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::setQuery,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("搜索备注") }
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(notes) { note ->
                NoteRow(note, onClick = { onEdit(note.normalizedNumber) })
            }
        }
    }
}

@Composable
private fun NoteRow(note: NumberNoteEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    note.displayLabel,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (note.rejectIncomingCall) {
                    Text(
                        "直接挂断",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Text(note.normalizedNumber, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (note.note.isNotBlank()) Text(note.note, maxLines = 2)
            Text(
                "${CategoryDisplay.labelOf(note.category)} · ${NoteImportance.fromName(note.importance).displayName}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
