package com.lyx.phone.note.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lyx.phone.note.data.db.NoteCategoryEntity
import com.lyx.phone.note.data.repository.NoteCategoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoryManageViewModel(
    private val repository: NoteCategoryRepository
) : ViewModel() {
    val categories = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<NoteCategoryEntity>())

    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input.asStateFlow()

    private val _editingCategory = MutableStateFlow<NoteCategoryEntity?>(null)
    val editingCategory: StateFlow<NoteCategoryEntity?> = _editingCategory.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun setInput(value: String) {
        _input.value = value.take(20)
        _message.value = null
    }

    fun startEdit(category: NoteCategoryEntity) {
        _editingCategory.value = category
        _input.value = category.name
        _message.value = null
    }

    fun cancelEdit() {
        _editingCategory.value = null
        _input.value = ""
        _message.value = null
    }

    fun save() {
        val name = _input.value.trim()
        if (name.isBlank()) {
            _message.value = "分类名称不能为空"
            return
        }
        val editing = _editingCategory.value
        viewModelScope.launch {
            val ok = if (editing == null) {
                repository.addCategory(name)
            } else {
                repository.updateCategory(editing, name)
            }
            _message.value = when {
                editing == null && ok -> "已添加分类"
                editing == null -> "分类已存在"
                ok -> "已保存分类"
                else -> "保存失败"
            }
            if (ok) {
                _editingCategory.value = null
                _input.value = ""
            }
        }
    }

    fun delete(category: NoteCategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category.id)
            if (_editingCategory.value?.id == category.id) {
                cancelEdit()
            }
        }
    }

    class Factory(private val repository: NoteCategoryRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = CategoryManageViewModel(repository) as T
    }
}
