package com.hatzolah.app.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A supply item that needs restocking - typically added after a call
 * where supplies were used.
 */
@Entity(
    tableName = "supply_requests",
    indices = [
        Index(value = ["isRestocked"]),
        Index(value = ["callLogId"]),
        Index(value = ["createdAt"])
    ]
)
data class SupplyRequest(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val itemName: String,
    val quantity: Int = 1,

    // Optional link back to the call this was used on
    val callLogId: Long? = null,
    val memberId: Long = -1,

    // Status
    val isRestocked: Boolean = false,
    val restockedAt: Long? = null,
    val notes: String = "",

    val createdAt: Long = System.currentTimeMillis()
)
