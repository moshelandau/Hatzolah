package com.hatzolah.app.ui.admin

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hatzolah.app.data.database.entity.Hospital
import com.hatzolah.app.data.database.entity.Member
import com.hatzolah.app.data.repository.HospitalRepository
import com.hatzolah.app.data.repository.MemberRepository
import com.hatzolah.app.util.DispatchNotificationHelper
import com.hatzolah.app.util.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.util.Log
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

data class AdminUiState(
    val members: List<Member> = emptyList(),
    val hospitals: List<Hospital> = emptyList(),
    val urgentCares: List<Hospital> = emptyList(),
    val dispatchNumber: String = "",
    val rmaHotline: String = "",
    val activeTab: Int = 0,
    val autoTestRunning: Boolean = false,
    val autoTestIntervalLabel: String = ""
)

@HiltViewModel
class AdminViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val memberRepository: MemberRepository,
    private val hospitalRepository: HospitalRepository,
    private val preferencesManager: PreferencesManager,
    private val dispatchNotificationHelper: DispatchNotificationHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AdminUiState(
            dispatchNumber = preferencesManager.getDispatchNumber(),
            rmaHotline = preferencesManager.getRmaHotline()
        )
    )
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()
    private var autoTestJob: Job? = null

    // Sample test data for fake dispatches
    private val testDispatches = listOf(
        Triple("Cardiac Arrest", "45 M", "123 Main St #4B, Monroe 10950"),
        Triple("Difficulty Breathing", "67 F", "8 Hamaspik Way, Monroe 10950"),
        Triple("Fall", "82 M", "55 Forest Rd #201, Monroe 10950"),
        Triple("Pediatric Emergency", "3 F", "12 Dinev Road #303, Monroe 10950"),
        Triple("Seizure", "28 M", "97 Acres Rd, Monroe 10950"),
        Triple("Choking", "1 M", "3 Taylor Ct #301, Monroe 10950"),
        Triple("Allergic Reaction", "19 F", "7 Berkeley Ct, Monroe 10950"),
        Triple("OB Emergency", "31 F", "15 Austin Rd, Monroe 10950"),
        Triple("Chest Pain", "55 M", "22 Winchester Dr, Monroe 10950"),
        Triple("Diabetic Emergency", "44 F", "6 Van Buren Dr #301, Monroe 10950"),
        Triple("Dislocation", "12 M", "19 Satmar Dr #301, Monroe 10950"),
        Triple("Laceration", "8 F", "5 Van Buren Dr #202, Monroe 10950"),
        Triple("Unconscious", "70 M", "99 Forest Rd #101, Monroe 10950"),
        Triple("Stroke", "62 F", "14 Praag Blvd #403, Monroe 10950"),
    )

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
                hospitalRepository.getByFacilityType(Hospital.FACILITY_HOSPITAL).collect { hospitals ->
                    _uiState.update { it.copy(hospitals = hospitals) }
                }
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error collecting hospitals from database", e)
            }
        }
        viewModelScope.launch {
            try {
                hospitalRepository.getByFacilityType(Hospital.FACILITY_URGENT_CARE).collect { urgentCares ->
                    _uiState.update { it.copy(urgentCares = urgentCares) }
                }
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error collecting urgent cares from database", e)
            }
        }
    }

    fun sendTestDispatch() {
        val (callType, age, address) = testDispatches[Random.nextInt(testDispatches.size)]
        val unit = "KY${Random.nextInt(60, 99)}"
        val cad = "0${Random.nextInt(100, 999)}${Random.nextInt(100000, 999999)}"
        val rawMessage = """
            KJ EMS D- $unit
            $address
            CALL TYPE: $callType
            CALLER: 845${Random.nextInt(1000000, 9999999)}
            CAD: $cad
            UNITS: $unit
            AGE: $age
        """.trimIndent()

        dispatchNotificationHelper.showDispatchNotification(
            context = context,
            address = address,
            callType = callType,
            rawMessage = rawMessage,
            units = unit,
            age = age
        )
    }

    fun startAutoTest(intervalMs: Long) {
        autoTestJob?.cancel()
        val label = if (intervalMs >= 60_000) "${intervalMs / 60_000} min" else "${intervalMs / 1000}s"
        _uiState.update { it.copy(autoTestRunning = true, autoTestIntervalLabel = label) }
        autoTestJob = viewModelScope.launch {
            while (true) {
                delay(intervalMs)
                sendTestDispatch()
            }
        }
    }

    fun stopAutoTest() {
        autoTestJob?.cancel()
        autoTestJob = null
        _uiState.update { it.copy(autoTestRunning = false) }
    }

    override fun onCleared() {
        super.onCleared()
        autoTestJob?.cancel()
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
        val validLat = if (latitude in -90.0..90.0) latitude else 0.0
        val validLng = if (longitude in -180.0..180.0) longitude else 0.0

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

    fun updateHospital(hospital: Hospital) {
        viewModelScope.launch {
            hospitalRepository.updateHospital(hospital)
        }
    }

    fun deleteHospital(hospital: Hospital) {
        viewModelScope.launch {
            hospitalRepository.deleteHospital(hospital)
        }
    }

    fun addUrgentCare(name: String, address: String, phone: String, contactPerson: String, notes: String) {
        viewModelScope.launch {
            hospitalRepository.addHospital(
                Hospital(
                    name = name,
                    address = address,
                    mainHotline = phone,
                    departmentHotlines = if (contactPerson.isNotBlank()) "{\"Contact\":\"$contactPerson\"}" else "",
                    additionalNotes = notes,
                    facilityType = Hospital.FACILITY_URGENT_CARE
                )
            )
        }
    }

    fun updateUrgentCare(hospital: Hospital) {
        viewModelScope.launch {
            hospitalRepository.updateHospital(hospital)
        }
    }

    fun deleteUrgentCare(hospital: Hospital) {
        viewModelScope.launch {
            hospitalRepository.deleteHospital(hospital)
        }
    }
}
