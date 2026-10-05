package com.lyx.phone.note.ui.noteedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lyx.phone.note.domain.NoteImportance

@Composable
fun NoteEditScreen(
    viewModel: NoteEditViewModel,
    onDone: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val categories by viewModel.categories.collectAsState()
    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("编辑本地备注", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = state.normalizedNumber,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            label = { Text("号码") }
        )
        OutlinedTextField(
            value = state.displayLabel,
            onValueChange = viewModel::updateLabel,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("标签") },
            supportingText = { Text("可不填；标签和详细备注至少填写一项") },
            isError = state.error != null
        )
        OutlinedTextField(
            value = state.note,
            onValueChange = viewModel::updateNote,
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            label = { Text("详细备注") },
            supportingText = { Text("${state.note.length}/200") }
        )
        CategoryPicker(state.category, categories, viewModel::updateCategory)
        ImportancePicker(state.importance, viewModel::updateImportance)
        SwitchRow(
            title = "来电时显示悬浮提示",
            desc = "仅本地匹配，不影响电话响铃。",
            checked = state.enableIncomingPopup,
            onCheckedChange = viewModel::updatePopup
        )
        SwitchRow(
            title = "来电直接挂断",
            desc = "仅对这个号码生效。开启后匹配来电会被拒接，不显示悬浮提示。",
            checked = state.rejectIncomingCall,
            onCheckedChange = viewModel::updateRejectIncomingCall
        )
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = viewModel::save, modifier = Modifier.weight(1f)) {
                Text("保存")
            }
            OutlinedButton(onClick = viewModel::delete, modifier = Modifier.weight(1f)) {
                Text("删除")
            }
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryPicker(value: String, categories: List<String>, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            label = { Text("分类") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            categories.forEach {
                DropdownMenuItem(
                    text = { Text(it) },
                    onClick = {
                        onChange(it)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportancePicker(value: NoteImportance, onChange: (NoteImportance) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = value.displayName,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            label = { Text("重要程度") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            NoteImportance.entries.forEach {
                DropdownMenuItem(
                    text = { Text(it.displayName) },
                    onClick = {
                        onChange(it)
                        expanded = false
                    }
                )
            }
        }
    }
}
