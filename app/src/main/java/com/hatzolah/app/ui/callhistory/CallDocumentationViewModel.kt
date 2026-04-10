package com.hatzolah.app.ui.callhistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hatzolah.app.data.repository.CallLogRepository
import com.hatzolah.app.data.repository.SupplyRepository
import com.hatzolah.app.util.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CallDocumentationUiState(
    val patientName: String = "",
    val patientDob: String = "",
    val medicalNotes: String = "",
    val isLoading: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CallDocumentationViewModel @Inject constructor(
    private val callLogRepository: CallLogRepository,
    private val supplyRepository: SupplyRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CallDocumentationUiState())
    val uiState: StateFlow<CallDocumentationUiState> = _uiState.asStateFlow()

    private var callLogId: Long = -1L
    private var loadJob: Job? = null

    fun loadCallLog(id: Long) {
        if (id <= 0L) {
            _uiState.update { it.copy(error = "Invalid call ID") }
            return
        }
        if (callLogId == id && _uiState.value.patientName.isNotBlank()) {
            // Already loaded - avoid refetching on recomposition
            return
        }
        callLogId = id
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val call = callLogRepository.getCallLogById(id)
                if (call != null) {
                    _uiState.update {
                        it.copy(
                            patientName = call.patientName,
                            patientDob = call.patientDob,
                            medicalNotes = call.medicalNotes,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Call not found") }
                }
            } catch (e: Throwable) {
                _uiState.update { it.copy(isLoading = false, error = "Failed to load: ${e.message}") }
            }
        }
    }

    fun onPatientNameChanged(name: String) = _uiState.update { it.copy(patientName = name, error = null) }
    fun onPatientDobChanged(dob: String) = _uiState.update { it.copy(patientDob = dob, error = null) }
    fun onMedicalNotesChanged(notes: String) = _uiState.update { it.copy(medicalNotes = notes, error = null) }

    fun saveDocumentation() {
        if (callLogId <= 0L) {
            _uiState.update { it.copy(error = "Cannot save - no active call") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val call = callLogRepository.getCallLogById(callLogId)
                if (call == null) {
                    _uiState.update { it.copy(isLoading = false, error = "Call record no longer exists") }
                    return@launch
                }
                callLogRepository.updateCallLog(
                    call.copy(
                        patientName = _uiState.value.patientName,
                        patientDob = _uiState.value.patientDob,
                        medicalNotes = _uiState.value.medicalNotes,
                        isDocumented = true
                    )
                )
                _uiState.update { it.copy(isLoading = false, saved = true) }
            } catch (e: Throwable) {
                _uiState.update { it.copy(isLoading = false, error = "Save failed: ${e.message}") }
            }
        }
    }

    fun addSupplies(text: String, callLogId: Long) {
        viewModelScope.launch {
            try {
                supplyRepository.addFromNote(text, callLogId, preferencesManager.getLoggedInMemberId())
            } catch (_: Throwable) { /* don't crash */ }
        }
    }
}
