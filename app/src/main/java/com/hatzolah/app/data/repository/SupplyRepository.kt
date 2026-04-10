package com.hatzolah.app.data.repository

import com.hatzolah.app.data.database.dao.SupplyRequestDao
import com.hatzolah.app.data.database.entity.SupplyRequest
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupplyRepository @Inject constructor(
    private val dao: SupplyRequestDao
) {
    fun getAll(): Flow<List<SupplyRequest>> = dao.getAll()
    fun getPending(): Flow<List<SupplyRequest>> = dao.getPending()
    fun getRestocked(): Flow<List<SupplyRequest>> = dao.getRestocked()
    fun getPendingCount(): Flow<Int> = dao.getPendingCount()

    suspend fun getByCallLog(callLogId: Long): List<SupplyRequest> = dao.getByCallLog(callLogId)
    fun observeByCallLog(callLogId: Long): Flow<List<SupplyRequest>> = dao.observeByCallLog(callLogId)

    suspend fun add(request: SupplyRequest): Long = dao.insert(request)

    suspend fun addMultiple(items: List<SupplyRequest>) = dao.insertAll(items)

    /**
     * Parses a multi-line note like:
     *   "2x gauze\n1 IV kit\nmask"
     * into individual supply requests.
     */
    suspend fun addFromNote(note: String, callLogId: Long?, memberId: Long) {
        if (note.isBlank()) return
        val items = note.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { line ->
                // Try to extract quantity from patterns like "2x gauze", "2 IV kits", "x2 mask"
                val qtyMatch = Regex("^(?:(\\d+)\\s*x?|x\\s*(\\d+))\\s+(.+)", RegexOption.IGNORE_CASE).find(line)
                if (qtyMatch != null) {
                    val qty = (qtyMatch.groupValues[1].ifBlank { qtyMatch.groupValues[2] }).toIntOrNull() ?: 1
                    val name = qtyMatch.groupValues[3].trim()
                    SupplyRequest(
                        itemName = name,
                        quantity = qty,
                        callLogId = callLogId,
                        memberId = memberId
                    )
                } else {
                    SupplyRequest(
                        itemName = line,
                        quantity = 1,
                        callLogId = callLogId,
                        memberId = memberId
                    )
                }
            }
        if (items.isNotEmpty()) dao.insertAll(items)
    }

    suspend fun update(request: SupplyRequest) = dao.update(request)
    suspend fun delete(request: SupplyRequest) = dao.delete(request)
    suspend fun markRestocked(id: Long) = dao.markRestocked(id)
    suspend fun markPending(id: Long) = dao.markPending(id)
    suspend fun markAllRestocked() = dao.markAllRestocked()
}
