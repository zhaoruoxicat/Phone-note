package com.lyx.phone.note.ui.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lyx.phone.note.data.db.NoteCategoryEntity

@Composable
fun CategoryManageScreen(
    viewModel: CategoryManageViewModel
) {
    val categories by viewModel.categories.collectAsState()
    val input by viewModel.input.collectAsState()
    val editing by viewModel.editingCategory.collectAsState()
    val message by viewModel.message.collectAsState()

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("分类管理", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = input,
                onValueChange = viewModel::setInput,
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text(if (editing == null) "新分类" else "编辑分类") }
            )
            Button(onClick = viewModel::save) {
                Text(if (editing == null) "添加" else "保存")
            }
        }
        if (editing != null) {
            OutlinedButton(onClick = viewModel::cancelEdit, modifier = Modifier.fillMaxWidth()) {
                Text("取消编辑")
            }
        }
        message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories, key = { it.id }) { category ->
                CategoryRow(
                    category = category,
                    onEdit = { viewModel.startEdit(category) },
                    onDelete = { viewModel.delete(category) }
                )
            }
        }
    }
}

@Composable
private fun CategoryRow(
    category: NoteCategoryEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(category.name, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (category.isBuiltIn) "内置分类" else "自定义分类",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                    Text("编辑")
                }
                OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f)) {
                    Text("删除")
                }
            }
        }
    }
}
