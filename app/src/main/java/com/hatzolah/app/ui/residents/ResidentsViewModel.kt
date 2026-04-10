package com.hatzolah.app.ui.residents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hatzolah.app.data.database.entity.Resident
import com.hatzolah.app.data.repository.ResidentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResidentsUiState(
    val query: String = "",
    val residents: List<Resident> = emptyList(),
    val totalCount: Int = 0,
    val selected: ResidentDetails? = null,
    val isLoading: Boolean = false,
    val importMessage: String? = null
)

data class ResidentDetails(
    val resident: Resident,
    val father: Resident? = null,
    val fatherInLaw: Resident? = null,
    val sons: List<Resident> = emptyList(),
    val sonsInLaw: List<Resident> = emptyList()
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class ResidentsViewModel @Inject constructor(
    private val repository: ResidentRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _selected = MutableStateFlow<ResidentDetails?>(null)
    private val _importMessage = MutableStateFlow<String?>(null)
    private val _count = MutableStateFlow(0)

    init {
        viewModelScope.launch { _count.value = repository.count() }
    }

    // Debounce search by 250ms so typing fast doesn't hammer the DB
    private val residentsFlow: Flow<List<Resident>> = _query
        .debounce(250)
        .flatMapLatest { q ->
            if (q.isBlank()) repository.getAllResidents()
            else repository.search(q)
        }

    val uiState: StateFlow<ResidentsUiState> = combine(
        _query,
        residentsFlow,
        _selected,
        _importMessage,
        _count
    ) { query, residents, selected, msg, count ->
        ResidentsUiState(
            query = query,
            residents = residents,
            totalCount = count,
            selected = selected,
            importMessage = msg
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ResidentsUiState()
    )

    fun onQueryChanged(q: String) {
        _query.value = q
    }

    fun selectResident(resident: Resident) {
        viewModelScope.launch {
            val father = resident.fatherId?.let { repository.getResidentById(it) }
            val fatherInLaw = resident.fatherInLawId?.let { repository.getResidentById(it) }
            val sons = repository.getSonsOf(resident.id)
            val sonsInLaw = repository.getSonsInLawOf(resident.id)
            _selected.value = ResidentDetails(resident, father, fatherInLaw, sons, sonsInLaw)
        }
    }

    fun closeDetails() {
        _selected.value = null
    }

    fun importCsv(csv: String) {
        viewModelScope.launch {
            try {
                val added = repository.importFromCsv(csv)
                _count.value = repository.count()
                _importMessage.value = "Imported $added residents"
            } catch (e: Throwable) {
                _importMessage.value = "Import failed: ${e.message}"
            }
        }
    }

    fun clearImportMessage() {
        _importMessage.value = null
    }

    fun deleteAll() {
        viewModelScope.launch {
            repository.deleteAll()
            _count.value = 0
        }
    }
}
