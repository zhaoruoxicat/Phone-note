package com.lyx.phone.note.ui.calllog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lyx.phone.note.data.calllog.CallLogItem
import com.lyx.phone.note.data.calllog.CallLogReader
import com.lyx.phone.note.data.calllog.CallLogResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CallLogUiState(
    val isLoading: Boolean = false,
    val items: List<CallLogItem> = emptyList(),
    val query: String = "",
    val message: String? = null
)

class CallLogViewModel(
    private val reader: CallLogReader
) : ViewModel() {
    private val _state = MutableStateFlow(CallLogUiState(isLoading = true))
    val state: StateFlow<CallLogUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isLoading = true, message = null)
            when (val result = reader.readRecent()) {
                is CallLogResult.Success -> _state.value = _state.value.copy(isLoading = false, items = result.items)
                CallLogResult.MissingPermission -> _state.value = _state.value.copy(
                    isLoading = false,
                    items = emptyList(),
                    message = "需要通话记录权限后才能显示最近通话"
                )
                is CallLogResult.Error -> _state.value = _state.value.copy(isLoading = false, message = result.message)
            }
        }
    }

    fun setQuery(query: String) {
        _state.value = _state.value.copy(query = query)
    }

    class Factory(private val reader: CallLogReader) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = CallLogViewModel(reader) as T
    }
}
