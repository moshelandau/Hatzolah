package com.hatzolah.app.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call_logs")
data class CallLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val memberId: Long,
    val date: Long, // epoch millis
    val patientName: String = "",
    val patientDob: String = "",
    val medicalNotes: String = "",
    val dispatchAddress: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val milesTraveled: Double = 0.0,
    val callDurationMinutes: Int = 0,
    val rmaApprovalStatus: String = "", // approved, denied, transferred, pending
    val outcome: String = "",
    val isDocumented: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
