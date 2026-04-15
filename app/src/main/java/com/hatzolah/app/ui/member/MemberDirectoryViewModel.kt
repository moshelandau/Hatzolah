package com.hatzolah.app.ui.member

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hatzolah.app.data.database.entity.Member
import com.hatzolah.app.data.repository.MemberRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * Sort options for the member directory.
 *
 * - [NAME] sorts alphabetically by the member's display name.
 * - [UNIT] sorts by the numeric part of the unit number (KY-1, KY-2, …, KY-85,
 *   …). Members without a unit number sort to the bottom.
 */
enum class MemberSortOption { NAME, UNIT }

data class MemberDirectoryUiState(
    val members: List<Member> = emptyList(),
    val searchQuery: String = "",
    val sortOption: MemberSortOption = MemberSortOption.NAME
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MemberDirectoryViewModel @Inject constructor(
    private val memberRepository: MemberRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _sortOption = MutableStateFlow(MemberSortOption.NAME)

    val uiState: StateFlow<MemberDirectoryUiState> = combine(
        _searchQuery,
        _sortOption
    ) { query, sort -> query to sort }
        .flatMapLatest { (query, sort) ->
            val base = if (query.isBlank()) {
                memberRepository.getVerifiedMembers()
            } else {
                memberRepository.searchMembers(query)
            }
            base.map { members ->
                MemberDirectoryUiState(
                    members = orderMembers(members, query, sort),
                    searchQuery = query,
                    sortOption = sort
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MemberDirectoryUiState())

    fun onSearchChanged(query: String) {
        _searchQuery.value = query
    }

    fun onSortChanged(sort: MemberSortOption) {
        _sortOption.value = sort
    }

    /**
     * Orders the incoming list by the current sort option, but **also**
     * floats unit-number matches to the top whenever the user is searching.
     *
     * Rationale: typing "89" should show KY-89 first, even if the user has
     * "Name" selected as the sort mode — otherwise operators would have to
     * switch sort modes every time they want to look a unit up.
     */
    private fun orderMembers(
        members: List<Member>,
        query: String,
        sort: MemberSortOption
    ): List<Member> {
        val nameComparator = compareBy<Member>(String.CASE_INSENSITIVE_ORDER) { it.name }
        val unitComparator = compareBy<Member>(
            { unitSortKey(it.unitNumber) },
            { it.unitNumber.lowercase() },
            { it.name.lowercase() }
        )
        val baseComparator = when (sort) {
            MemberSortOption.NAME -> nameComparator
            MemberSortOption.UNIT -> unitComparator
        }

        if (query.isBlank()) {
            return members.sortedWith(baseComparator)
        }

        // When searching, rank by how well each member matches, then fall back
        // to the selected sort for ties. Lower rank = higher in the list.
        val q = query.trim().lowercase()
        val qDigits = q.filter { it.isDigit() }
        fun rank(m: Member): Int {
            val unit = m.unitNumber.lowercase()
            val unitDigits = unit.filter { it.isDigit() }
            val name = m.name.lowercase()
            return when {
                // Exact unit-number hit (e.g. "ky89" == "ky89")
                unit.isNotBlank() && unit == q -> 0
                // Digit-only query that matches the unit's digits (e.g. "89"
                // -> "ky89"). This is the specific case the operator asked
                // for: typing a bare number should find that unit first.
                qDigits.isNotEmpty() && qDigits == unitDigits -> 1
                // Unit starts with the query (e.g. "ky8" -> "ky85")
                unit.isNotBlank() && unit.startsWith(q) -> 2
                // Unit's digit part starts with the query's digit part
                qDigits.isNotEmpty() && unitDigits.startsWith(qDigits) -> 3
                // Substring hit inside the unit number
                unit.contains(q) -> 4
                // Name starts with the query
                name.startsWith(q) -> 5
                // Name substring
                name.contains(q) -> 6
                else -> 7
            }
        }
        return members.sortedWith(compareBy<Member> { rank(it) }.then(baseComparator))
    }

    /**
     * Extracts the numeric part of a unit number so "KY-9" sorts before
     * "KY-10". Returns [Int.MAX_VALUE] for unit numbers with no digits so
     * they sort to the bottom of the list.
     */
    private fun unitSortKey(unit: String): Int {
        val digits = unit.filter { it.isDigit() }
        return digits.toIntOrNull() ?: Int.MAX_VALUE
    }
}
