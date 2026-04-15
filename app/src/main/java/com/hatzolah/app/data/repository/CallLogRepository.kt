package com.hatzolah.app.data.repository

import com.hatzolah.app.data.database.dao.CallLogDao
import com.hatzolah.app.data.database.entity.CallLog
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallLogRepository @Inject constructor(
    private val callLogDao: CallLogDao
) {
    fun getAllCallLogs(): Flow<List<CallLog>> = callLogDao.getAllCallLogs()

    fun getCallLogsByMember(memberId: Long): Flow<List<CallLog>> =
        callLogDao.getCallLogsByMember(memberId)

    fun getMostRecentCall(): Flow<CallLog?> = callLogDao.getMostRecentCall()

    fun getMostRecentCallByMember(memberId: Long): Flow<CallLog?> =
        callLogDao.getMostRecentCallByMember(memberId)

    // Team stats
    fun getTotalCallCount(): Flow<Int> = callLogDao.getTotalCallCount()
    fun getTotalTeamMiles(): Flow<Double> = callLogDao.getTotalTeamMiles()
    fun getAverageResponseTime(): Flow<Int> = callLogDao.getAverageResponseTime()

    // Personal stats
    fun getPersonalCallCount(memberId: Long): Flow<Int> =
        callLogDao.getPersonalCallCount(memberId)
    fun getPersonalTotalMiles(memberId: Long): Flow<Double> =
        callLogDao.getPersonalTotalMiles(memberId)
    fun getPersonalTotalMinutes(memberId: Long): Flow<Int> =
        callLogDao.getPersonalTotalMinutes(memberId)
    fun getPersonalCallsThisMonth(memberId: Long, startOfMonth: Long, endOfMonth: Long): Flow<Int> =
        callLogDao.getPersonalCallsThisMonth(memberId, startOfMonth, endOfMonth)

    fun getCallLogsByDateRange(startMs: Long, endMs: Long): Flow<List<CallLog>> =
        callLogDao.getCallLogsByDateRange(startMs, endMs)

    suspend fun getCallLogById(id: Long): CallLog? = callLogDao.getCallLogById(id)

    suspend fun existsByRawMessage(rawMessage: String): Boolean =
        callLogDao.countByRawMessage(rawMessage) > 0

    suspend fun getUndocumentedCallLogs(): List<CallLog> =
        callLogDao.getUndocumentedCallLogs()

    suspend fun addCallLog(callLog: CallLog): Long = callLogDao.insert(callLog)

    suspend fun updateCallLog(callLog: CallLog) = callLogDao.update(callLog)

    suspend fun deleteCallLog(callLog: CallLog) = callLogDao.delete(callLog)
}
