package com.hatzolah.app.ui.hospital

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hatzolah.app.data.database.entity.Hospital
import com.hatzolah.app.data.repository.HospitalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class HospitalDirectoryUiState(
    val hospitals: List<Hospital> = emptyList(),
    val searchQuery: String = ""
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HospitalDirectoryViewModel @Inject constructor(
    private val hospitalRepository: HospitalRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<HospitalDirectoryUiState> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                hospitalRepository.getAllHospitals()
            } else {
                hospitalRepository.searchHospitals(query)
            }
        }
        .map { hospitals ->
            HospitalDirectoryUiState(
                hospitals = hospitals,
                searchQuery = _searchQuery.value
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HospitalDirectoryUiState())

    fun onSearchChanged(query: String) {
        _searchQuery.value = query
    }
}
