package com.hatzolah.app.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hatzolah.app.data.database.entity.Member
import com.hatzolah.app.data.repository.MemberRepository
import com.hatzolah.app.util.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminUiState(
    val members: List<Member> = emptyList(),
    val dispatchNumber: String = "",
    val rmaHotline: String = ""
)

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val memberRepository: MemberRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AdminUiState(
            dispatchNumber = preferencesManager.getDispatchNumber(),
            rmaHotline = preferencesManager.getRmaHotline()
        )
    )
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            memberRepository.getAllMembers().collect { members ->
                _uiState.update { it.copy(members = members) }
            }
        }
    }

    fun onDispatchNumberChanged(number: String) {
        _uiState.update { it.copy(dispatchNumber = number) }
    }

    fun onRmaHotlineChanged(number: String) {
        _uiState.update { it.copy(rmaHotline = number) }
    }

    fun saveSettings() {
        preferencesManager.setDispatchNumber(_uiState.value.dispatchNumber)
        preferencesManager.setRmaHotline(_uiState.value.rmaHotline)
    }

    fun addMember(name: String, phone: String, whatsapp: String, email: String) {
        viewModelScope.launch {
            memberRepository.addMember(
                Member(
                    name = name,
                    phoneNumber = phone,
                    whatsappContact = whatsapp,
                    email = email
                )
            )
        }
    }

    fun deleteMember(member: Member) {
        viewModelScope.launch {
            memberRepository.deleteMember(member)
        }
    }
}
