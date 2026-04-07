package com.hatzolah.app.ui.callhistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hatzolah.app.data.repository.CallLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val saved: Boolean = false
)

@HiltViewModel
class CallDocumentationViewModel @Inject constructor(
    private val callLogRepository: CallLogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CallDocumentationUiState())
    val uiState: StateFlow<CallDocumentationUiState> = _uiState.asStateFlow()

    private var callLogId: Long = -1

    fun loadCallLog(id: Long) {
        callLogId = id
        viewModelScope.launch {
            callLogRepository.getCallLogById(id)?.let { call ->
                _uiState.update {
                    it.copy(
                        patientName = call.patientName,
                        patientDob = call.patientDob,
                        medicalNotes = call.medicalNotes
                    )
                }
            }
        }
    }

    fun onPatientNameChanged(name: String) = _uiState.update { it.copy(patientName = name) }
    fun onPatientDobChanged(dob: String) = _uiState.update { it.copy(patientDob = dob) }
    fun onMedicalNotesChanged(notes: String) = _uiState.update { it.copy(medicalNotes = notes) }

    fun saveDocumentation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            callLogRepository.getCallLogById(callLogId)?.let { call ->
                callLogRepository.updateCallLog(
                    call.copy(
                        patientName = _uiState.value.patientName,
                        patientDob = _uiState.value.patientDob,
                        medicalNotes = _uiState.value.medicalNotes,
                        isDocumented = true
                    )
                )
            }
            _uiState.update { it.copy(isLoading = false, saved = true) }
        }
    }
}
