package com.lyx.phone.note.ui.noteedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lyx.phone.note.data.db.NumberNoteEntity
import com.lyx.phone.note.data.repository.NoteCategoryRepository
import com.lyx.phone.note.data.repository.NumberNoteRepository
import com.lyx.phone.note.domain.CategoryDisplay
import com.lyx.phone.note.domain.NoteImportance
import com.lyx.phone.note.domain.PhoneNumberNormalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NoteEditUiState(
    val id: Long = 0,
    val rawNumber: String = "",
    val normalizedNumber: String = "",
    val last11Digits: String? = null,
    val displayLabel: String = "",
    val note: String = "",
    val category: String = "其他",
    val importance: NoteImportance = NoteImportance.NORMAL,
    val enableIncomingPopup: Boolean = true,
    val rejectIncomingCall: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val error: String? = null,
    val saved: Boolean = false
)

class NoteEditViewModel(
    private val repository: NumberNoteRepository,
    categoryRepository: NoteCategoryRepository,
    rawNumber: String,
    normalizedNumber: String?
) : ViewModel() {
    private val initial = PhoneNumberNormalizer.normalize(normalizedNumber ?: rawNumber)
    private val _state = MutableStateFlow(
        NoteEditUiState(
            rawNumber = rawNumber.ifBlank { initial.rawNumber },
            normalizedNumber = initial.normalizedNumber,
            last11Digits = initial.last11Digits
        )
    )
    val state: StateFlow<NoteEditUiState> = _state.asStateFlow()
    val categories = categoryRepository.observeCategories()
        .map { list -> list.map { it.name }.ifEmpty { categoryRepository.builtInNames } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), categoryRepository.builtInNames)

    init {
        viewModelScope.launch {
            repository.findNoteForIncomingCall(initial.normalizedNumber)?.let { entity ->
                _state.value = _state.value.copy(
                    id = entity.id,
                    rawNumber = entity.rawNumber,
                    normalizedNumber = entity.normalizedNumber,
                    last11Digits = entity.last11Digits,
                    displayLabel = entity.displayLabel,
                    note = entity.note,
                    category = CategoryDisplay.labelOf(entity.category),
                    importance = NoteImportance.fromName(entity.importance),
                    enableIncomingPopup = entity.enableIncomingPopup,
                    rejectIncomingCall = entity.rejectIncomingCall,
                    createdAt = entity.createdAt
                )
            }
        }
    }

    fun updateLabel(value: String) {
        _state.value = _state.value.copy(displayLabel = value.take(30), error = null)
    }

    fun updateNote(value: String) {
        _state.value = _state.value.copy(note = value.take(200), error = null)
    }

    fun updateCategory(value: String) {
        _state.value = _state.value.copy(category = value, error = null)
    }

    fun updateImportance(value: NoteImportance) {
        _state.value = _state.value.copy(importance = value, error = null)
    }

    fun updatePopup(enabled: Boolean) {
        _state.value = _state.value.copy(enableIncomingPopup = enabled)
    }

    fun updateRejectIncomingCall(enabled: Boolean) {
        _state.value = _state.value.copy(rejectIncomingCall = enabled)
    }

    fun save() {
        val current = _state.value
        if (current.displayLabel.isBlank() && current.note.isBlank()) {
            _state.value = current.copy(error = "标签和详细备注至少填写一项")
            return
        }
        val finalLabel = current.displayLabel.trim()
            .ifBlank { current.note.trim().take(20) }
            .ifBlank { current.normalizedNumber }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repository.upsert(
                NumberNoteEntity(
                    id = current.id,
                    rawNumber = current.rawNumber,
                    normalizedNumber = current.normalizedNumber,
                    last11Digits = current.last11Digits,
                    displayLabel = finalLabel,
                    note = current.note.trim(),
                    category = CategoryDisplay.storageValueOf(current.category),
                    importance = current.importance.name,
                    enableIncomingPopup = current.enableIncomingPopup,
                    rejectIncomingCall = current.rejectIncomingCall,
                    createdAt = current.createdAt,
                    updatedAt = now
                )
            )
            _state.value = _state.value.copy(saved = true)
        }
    }

    fun delete() {
        val current = _state.value
        if (current.id == 0L) {
            _state.value = current.copy(saved = true)
            return
        }
        viewModelScope.launch {
            repository.delete(
                NumberNoteEntity(
                    id = current.id,
                    rawNumber = current.rawNumber,
                    normalizedNumber = current.normalizedNumber,
                    last11Digits = current.last11Digits,
                    displayLabel = current.displayLabel,
                    note = current.note,
                    category = CategoryDisplay.storageValueOf(current.category),
                    importance = current.importance.name,
                    enableIncomingPopup = current.enableIncomingPopup,
                    rejectIncomingCall = current.rejectIncomingCall,
                    createdAt = current.createdAt,
                    updatedAt = System.currentTimeMillis()
                )
            )
            _state.value = _state.value.copy(saved = true)
        }
    }

    class Factory(
        private val repository: NumberNoteRepository,
        private val categoryRepository: NoteCategoryRepository,
        private val rawNumber: String,
        private val normalizedNumber: String?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            NoteEditViewModel(repository, categoryRepository, rawNumber, normalizedNumber) as T
    }
}
