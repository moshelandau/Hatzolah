package com.hatzolah.app.ui.member

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hatzolah.app.data.database.entity.Member
import com.hatzolah.app.data.repository.MemberRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class MemberDirectoryUiState(
    val members: List<Member> = emptyList(),
    val searchQuery: String = ""
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MemberDirectoryViewModel @Inject constructor(
    private val memberRepository: MemberRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<MemberDirectoryUiState> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                memberRepository.getVerifiedMembers()
            } else {
                memberRepository.searchMembers(query)
            }
        }
        .map { members ->
            MemberDirectoryUiState(members = members, searchQuery = _searchQuery.value)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MemberDirectoryUiState())

    fun onSearchChanged(query: String) {
        _searchQuery.value = query
    }
}
