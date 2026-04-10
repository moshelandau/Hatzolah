package com.hatzolah.app.data.database.dao

import androidx.room.*
import com.hatzolah.app.data.database.entity.Resident
import kotlinx.coroutines.flow.Flow

@Dao
interface ResidentDao {

    @Query("SELECT * FROM residents ORDER BY lastNameEn, firstNameEn LIMIT 500")
    fun getAllResidents(): Flow<List<Resident>>

    @Query("SELECT * FROM residents WHERE id = :id")
    suspend fun getResidentById(id: Long): Resident?

    /**
     * Bi-lingual search: matches Hebrew OR English in any name/phone/address field.
     * Results limited to 200 for performance.
     */
    @Query("""
        SELECT * FROM residents
        WHERE firstNameEn LIKE '%' || :query || '%'
           OR lastNameEn LIKE '%' || :query || '%'
           OR firstNameHe LIKE '%' || :query || '%'
           OR lastNameHe LIKE '%' || :query || '%'
           OR nicknameEn LIKE '%' || :query || '%'
           OR nicknameHe LIKE '%' || :query || '%'
           OR phoneNumber LIKE '%' || :query || '%'
           OR address LIKE '%' || :query || '%'
        ORDER BY lastNameEn, firstNameEn
        LIMIT 200
    """)
    fun search(query: String): Flow<List<Resident>>

    @Query("SELECT * FROM residents WHERE fatherId = :fatherId ORDER BY firstNameEn")
    suspend fun getSonsOf(fatherId: Long): List<Resident>

    @Query("SELECT * FROM residents WHERE fatherInLawId = :fatherInLawId ORDER BY firstNameEn")
    suspend fun getSonsInLawOf(fatherInLawId: Long): List<Resident>

    @Query("SELECT COUNT(*) FROM residents")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(resident: Resident): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(residents: List<Resident>): List<Long>

    @Update
    suspend fun update(resident: Resident)

    @Delete
    suspend fun delete(resident: Resident)

    @Query("DELETE FROM residents")
    suspend fun deleteAll()
}
