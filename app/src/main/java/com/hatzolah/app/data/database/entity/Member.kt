package com.hatzolah.app.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "members")
data class Member(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phoneNumber: String,
    val whatsappContact: String = "",
    val email: String = "",
    val unitNumber: String = "", // e.g. "KY67" - dispatch unit assignment
    val isVerified: Boolean = false,
    val isAdmin: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
