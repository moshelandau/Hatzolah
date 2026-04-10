package com.hatzolah.app.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A resident entry in the Kiryas Yoel phone book.
 * Supports Hebrew + English names, addresses, and family relationships.
 */
@Entity(
    tableName = "residents",
    indices = [
        Index(value = ["phoneNumber"]),
        Index(value = ["lastNameEn"]),
        Index(value = ["lastNameHe"]),
        Index(value = ["fatherId"]),
        Index(value = ["fatherInLawId"])
    ]
)
data class Resident(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // English (Latin script)
    val firstNameEn: String = "",
    val lastNameEn: String = "",

    // Hebrew script
    val firstNameHe: String = "",
    val lastNameHe: String = "",

    // Nickname / descriptor (e.g. "Kolel, Son of X")
    val nicknameEn: String = "",
    val nicknameHe: String = "",

    // Contact
    val phoneNumber: String = "",
    val address: String = "",

    // Family relationships - foreign keys to other residents (nullable for root records)
    val fatherId: Long? = null,       // father
    val fatherInLawId: Long? = null,  // שווער (father-in-law)

    // Extra metadata
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
