package com.hatzolah.app.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hospitals")
data class Hospital(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val address: String,
    val erLocation: String = "",
    val accessCodes: String = "",
    val kosherRoomLocation: String = "",
    val patientAssistanceNotes: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val mainHotline: String = "",
    val obHotline: String = "",
    val departmentHotlines: String = "", // JSON string of department -> hotline mappings
    val communicationSystem: String = "", // Tiger Connect, phone, etc.
    val bedAvailability: String = "",
    val additionalNotes: String = "",
    val facilityType: String = FACILITY_HOSPITAL // "HOSPITAL" or "URGENT_CARE"
) {
    companion object {
        const val FACILITY_HOSPITAL = "HOSPITAL"
        const val FACILITY_URGENT_CARE = "URGENT_CARE"
    }
}
