package com.hatzolah.app.ui.supplies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hatzolah.app.data.database.entity.SupplyRequest
import com.hatzolah.app.data.repository.SupplyRepository
import com.hatzolah.app.util.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SuppliesUiState(
    val pending: List<SupplyRequest> = emptyList(),
    val restocked: List<SupplyRequest> = emptyList(),
    val pendingCount: Int = 0,
    val tab: Int = 0 // 0 = pending, 1 = history
)

@HiltViewModel
class SuppliesViewModel @Inject constructor(
    private val repository: SupplyRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _tab = MutableStateFlow(0)

    val uiState: StateFlow<SuppliesUiState> = combine(
        repository.getPending(),
        repository.getRestocked(),
        repository.getPendingCount(),
        _tab
    ) { pending, restocked, count, tab ->
        SuppliesUiState(
            pending = pending,
            restocked = restocked,
            pendingCount = count,
            tab = tab
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SuppliesUiState()
    )

    fun onTabChanged(tab: Int) {
        _tab.value = tab
    }

    fun addItem(name: String, quantity: Int, callLogId: Long? = null) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.add(
                SupplyRequest(
                    itemName = name.trim(),
                    quantity = quantity.coerceAtLeast(1),
                    callLogId = callLogId,
                    memberId = preferencesManager.getLoggedInMemberId()
                )
            )
        }
    }

    fun addFromMultiLine(text: String, callLogId: Long? = null) {
        viewModelScope.launch {
            repository.addFromNote(text, callLogId, preferencesManager.getLoggedInMemberId())
        }
    }

    fun markRestocked(item: SupplyRequest) {
        viewModelScope.launch { repository.markRestocked(item.id) }
    }

    fun markPending(item: SupplyRequest) {
        viewModelScope.launch { repository.markPending(item.id) }
    }

    fun delete(item: SupplyRequest) {
        viewModelScope.launch { repository.delete(item) }
    }

    fun markAllRestocked() {
        viewModelScope.launch { repository.markAllRestocked() }
    }
}
