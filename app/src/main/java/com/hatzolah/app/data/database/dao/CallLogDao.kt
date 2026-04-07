package com.hatzolah.app.data.database.dao

import androidx.room.*
import com.hatzolah.app.data.database.entity.CallLog
import kotlinx.coroutines.flow.Flow

@Dao
interface CallLogDao {

    @Query("SELECT * FROM call_logs ORDER BY date DESC")
    fun getAllCallLogs(): Flow<List<CallLog>>

    @Query("SELECT * FROM call_logs WHERE memberId = :memberId ORDER BY date DESC")
    fun getCallLogsByMember(memberId: Long): Flow<List<CallLog>>

    @Query("SELECT * FROM call_logs WHERE id = :id")
    suspend fun getCallLogById(id: Long): CallLog?

    @Query("SELECT * FROM call_logs ORDER BY date DESC LIMIT 1")
    fun getMostRecentCall(): Flow<CallLog?>

    @Query("SELECT * FROM call_logs WHERE memberId = :memberId ORDER BY date DESC LIMIT 1")
    fun getMostRecentCallByMember(memberId: Long): Flow<CallLog?>

    // Aggregate team statistics
    @Query("SELECT COUNT(*) FROM call_logs")
    fun getTotalCallCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(milesTraveled), 0.0) FROM call_logs")
    fun getTotalTeamMiles(): Flow<Double>

    @Query("SELECT COALESCE(AVG(callDurationMinutes), 0) FROM call_logs WHERE callDurationMinutes > 0")
    fun getAverageResponseTime(): Flow<Int>

    // Personal statistics
    @Query("SELECT COUNT(*) FROM call_logs WHERE memberId = :memberId")
    fun getPersonalCallCount(memberId: Long): Flow<Int>

    @Query("SELECT COALESCE(SUM(milesTraveled), 0.0) FROM call_logs WHERE memberId = :memberId")
    fun getPersonalTotalMiles(memberId: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(callDurationMinutes), 0) FROM call_logs WHERE memberId = :memberId")
    fun getPersonalTotalMinutes(memberId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM call_logs WHERE memberId = :memberId AND date >= :startOfMonth AND date < :endOfMonth")
    fun getPersonalCallsThisMonth(memberId: Long, startOfMonth: Long, endOfMonth: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(callLog: CallLog): Long

    @Update
    suspend fun update(callLog: CallLog)

    @Delete
    suspend fun delete(callLog: CallLog)
}
