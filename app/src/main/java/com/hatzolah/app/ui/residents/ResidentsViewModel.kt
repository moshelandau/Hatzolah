package com.hatzolah.app.ui.residents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hatzolah.app.data.database.entity.Member
import com.hatzolah.app.data.database.entity.Resident
import com.hatzolah.app.data.repository.MemberRepository
import com.hatzolah.app.data.repository.ResidentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResidentsUiState(
    val query: String = "",
    val activeTab: Int = 0, // 0 = Residents, 1 = Members
    val residents: List<Resident> = emptyList(),
    val members: List<Member> = emptyList(),
    val totalCount: Int = 0,
    val memberCount: Int = 0,
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
    private val repository: ResidentRepository,
    private val memberRepository: MemberRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _activeTab = MutableStateFlow(0)
    private val _selected = MutableStateFlow<ResidentDetails?>(null)
    private val _importMessage = MutableStateFlow<String?>(null)
    private val _count = MutableStateFlow(0)

    init {
        viewModelScope.launch { refreshCount() }
    }

    private suspend fun refreshCount() {
        _count.value = repository.count()
    }

    // Debounce search by 250ms so typing fast doesn't hammer the DB
    private val residentsFlow: Flow<List<Resident>> = _query
        .debounce(250)
        .flatMapLatest { q ->
            if (q.isBlank()) repository.getAllResidents()
            else repository.search(q)
        }

    // All members (filtered client-side on the query for simplicity).
    private val membersFlow: Flow<List<Member>> = combine(
        _query.debounce(150),
        memberRepository.getAllMembers()
    ) { q, members ->
        val needle = q.trim().lowercase()
        if (needle.isBlank()) members
        else members.filter { m ->
            m.name.lowercase().contains(needle) ||
                m.phoneNumber.contains(needle) ||
                m.unitNumber.lowercase().contains(needle)
        }
    }

    val uiState: StateFlow<ResidentsUiState> = combine(
        combine(_query, _activeTab, residentsFlow, membersFlow) { q, tab, residents, members ->
            ResidentsUiState(
                query = q,
                activeTab = tab,
                residents = residents,
                members = members,
                memberCount = members.size
            )
        },
        _selected,
        _importMessage,
        _count
    ) { base, selected, msg, count ->
        base.copy(
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

    fun onTabChanged(tab: Int) {
        _activeTab.value = tab
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
                refreshCount()
                if (added < 0) {
                    _importMessage.value = "No valid residents found in CSV data"
                } else {
                    _importMessage.value = "Imported $added residents"
                }
            } catch (e: Throwable) {
                _importMessage.value = "Import partially failed: ${e.message}. Some records may have been imported."
            }
        }
    }

    fun clearImportMessage() {
        _importMessage.value = null
    }

    fun deleteAll() {
        viewModelScope.launch {
            repository.deleteAll()
            refreshCount()
        }
    }
}
