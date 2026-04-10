package com.hatzolah.app.data.database.dao

import androidx.room.*
import com.hatzolah.app.data.database.entity.SupplyRequest
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplyRequestDao {

    @Query("SELECT * FROM supply_requests ORDER BY isRestocked ASC, createdAt DESC")
    fun getAll(): Flow<List<SupplyRequest>>

    @Query("SELECT * FROM supply_requests WHERE isRestocked = 0 ORDER BY createdAt DESC")
    fun getPending(): Flow<List<SupplyRequest>>

    @Query("SELECT * FROM supply_requests WHERE isRestocked = 1 ORDER BY restockedAt DESC LIMIT 100")
    fun getRestocked(): Flow<List<SupplyRequest>>

    @Query("SELECT * FROM supply_requests WHERE callLogId = :callLogId ORDER BY createdAt DESC")
    suspend fun getByCallLog(callLogId: Long): List<SupplyRequest>

    @Query("SELECT * FROM supply_requests WHERE callLogId = :callLogId ORDER BY createdAt DESC")
    fun observeByCallLog(callLogId: Long): Flow<List<SupplyRequest>>

    @Query("SELECT COUNT(*) FROM supply_requests WHERE isRestocked = 0")
    fun getPendingCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(request: SupplyRequest): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(requests: List<SupplyRequest>)

    @Update
    suspend fun update(request: SupplyRequest)

    @Delete
    suspend fun delete(request: SupplyRequest)

    @Query("UPDATE supply_requests SET isRestocked = 1, restockedAt = :time WHERE id = :id")
    suspend fun markRestocked(id: Long, time: Long = System.currentTimeMillis())

    @Query("UPDATE supply_requests SET isRestocked = 0, restockedAt = NULL WHERE id = :id")
    suspend fun markPending(id: Long)

    @Query("UPDATE supply_requests SET isRestocked = 1, restockedAt = :time WHERE isRestocked = 0")
    suspend fun markAllRestocked(time: Long = System.currentTimeMillis())
}
