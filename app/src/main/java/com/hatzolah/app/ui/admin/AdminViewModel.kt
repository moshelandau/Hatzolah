package com.hatzolah.app.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hatzolah.app.data.database.entity.Hospital
import com.hatzolah.app.data.database.entity.Member
import com.hatzolah.app.data.repository.HospitalRepository
import com.hatzolah.app.data.repository.MemberRepository
import com.hatzolah.app.util.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import android.util.Log
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminUiState(
    val members: List<Member> = emptyList(),
    val hospitals: List<Hospital> = emptyList(),
    val dispatchNumber: String = "",
    val rmaHotline: String = "",
    val activeTab: Int = 0 // 0=Settings, 1=Members, 2=Hospitals
)

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val memberRepository: MemberRepository,
    private val hospitalRepository: HospitalRepository,
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
            try {
                memberRepository.getAllMembers().collect { members ->
                    _uiState.update { it.copy(members = members) }
                }
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error collecting members from database", e)
            }
        }
        viewModelScope.launch {
            try {
                hospitalRepository.getAllHospitals().collect { hospitals ->
                    _uiState.update { it.copy(hospitals = hospitals) }
                }
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error collecting hospitals from database", e)
            }
        }
    }

    fun onTabChanged(tab: Int) {
        _uiState.update { it.copy(activeTab = tab) }
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

    fun addHospital(
        name: String,
        address: String,
        erLocation: String,
        accessCodes: String,
        kosherRoom: String,
        patientAssistance: String,
        latitude: Double,
        longitude: Double,
        mainHotline: String,
        obHotline: String,
        departmentHotlines: String,
        communicationSystem: String,
        bedAvailability: String
    ) {
        // Validate lat/lng ranges; reset to 0.0 if out of bounds
        val validLat = if (latitude in -90.0..90.0) latitude else 0.0
        val validLng = if (longitude in -180.0..180.0) longitude else 0.0

        // Intentionally allowing duplicate hospital names: a hospital may have multiple
        // entries for different ERs, campuses, or departments at the same facility.
        viewModelScope.launch {
            hospitalRepository.addHospital(
                Hospital(
                    name = name,
                    address = address,
                    erLocation = erLocation,
                    accessCodes = accessCodes,
                    kosherRoomLocation = kosherRoom,
                    patientAssistanceNotes = patientAssistance,
                    latitude = validLat,
                    longitude = validLng,
                    mainHotline = mainHotline,
                    obHotline = obHotline,
                    departmentHotlines = departmentHotlines,
                    communicationSystem = communicationSystem,
                    bedAvailability = bedAvailability
                )
            )
        }
    }

    fun deleteHospital(hospital: Hospital) {
        viewModelScope.launch {
            hospitalRepository.deleteHospital(hospital)
        }
    }
}
