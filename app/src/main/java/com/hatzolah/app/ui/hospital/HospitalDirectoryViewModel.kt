package com.hatzolah.app.ui.hospital

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hatzolah.app.data.database.entity.Hospital
import com.hatzolah.app.data.repository.HospitalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class HospitalWithDistance(
    val hospital: Hospital,
    val distanceMiles: Double? = null
)

data class HospitalDirectoryUiState(
    val hospitals: List<HospitalWithDistance> = emptyList(),
    val searchQuery: String = "",
    val sortByDistance: Boolean = true,
    val selectedTab: Int = 0 // 0 = Hospitals, 1 = Urgent Care
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class HospitalDirectoryViewModel @Inject constructor(
    private val hospitalRepository: HospitalRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _userLat = MutableStateFlow(0.0)
    private val _userLng = MutableStateFlow(0.0)
    private val _sortByDistance = MutableStateFlow(true)
    private val _selectedTab = MutableStateFlow(0)

    fun updateLocation(lat: Double, lng: Double) {
        _userLat.value = lat
        _userLng.value = lng
    }

    val uiState: StateFlow<HospitalDirectoryUiState> = combine(
        _searchQuery.debounce(200),
        _userLat,
        _userLng,
        _sortByDistance,
        _selectedTab
    ) { query, lat, lng, sortDist, tab ->
        data class Params(val query: String, val lat: Double, val lng: Double, val sortDist: Boolean, val tab: Int)
        Params(query, lat, lng, sortDist, tab)
    }
        .flatMapLatest { params ->
            val facilityType = if (params.tab == 0) Hospital.FACILITY_HOSPITAL else Hospital.FACILITY_URGENT_CARE
            val hospitalsFlow = if (params.query.isBlank()) {
                hospitalRepository.getByFacilityType(facilityType)
            } else {
                hospitalRepository.searchByFacilityType(params.query, facilityType)
            }
            hospitalsFlow.map { hospitals ->
                val withDistance = hospitals.map { h ->
                    val dist = if (params.lat != 0.0 && params.lng != 0.0
                        && h.latitude != 0.0 && h.longitude != 0.0
                        && h.latitude in -90.0..90.0 && h.longitude in -180.0..180.0
                    ) {
                        val results = FloatArray(1)
                        Location.distanceBetween(params.lat, params.lng, h.latitude, h.longitude, results)
                        (results[0] / 1609.344)
                    } else null
                    HospitalWithDistance(h, dist)
                }
                val sorted = if (params.sortDist) {
                    withDistance.sortedWith(compareBy(nullsLast()) { it.distanceMiles })
                } else {
                    withDistance.sortedBy { it.hospital.name }
                }
                HospitalDirectoryUiState(sorted, params.query, params.sortDist, params.tab)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HospitalDirectoryUiState())

    fun onSearchChanged(query: String) {
        _searchQuery.value = query
    }

    fun toggleSort() {
        _sortByDistance.value = !_sortByDistance.value
    }

    fun onTabChanged(tab: Int) {
        _selectedTab.value = tab
    }
}
