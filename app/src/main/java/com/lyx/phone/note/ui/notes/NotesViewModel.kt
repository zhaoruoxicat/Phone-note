package com.lyx.phone.note.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lyx.phone.note.data.db.NumberNoteEntity
import com.lyx.phone.note.data.repository.NumberNoteRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModel(private val repository: NumberNoteRepository) : ViewModel() {
    val query = MutableStateFlow("")
    val notes = query.flatMapLatest { repository.searchNotes(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<NumberNoteEntity>())

    fun setQuery(value: String) {
        query.value = value
    }

    class Factory(private val repository: NumberNoteRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = NotesViewModel(repository) as T
    }
}
