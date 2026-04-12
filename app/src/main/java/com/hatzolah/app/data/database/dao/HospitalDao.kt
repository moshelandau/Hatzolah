package com.hatzolah.app.data.database.dao

import androidx.room.*
import com.hatzolah.app.data.database.entity.Hospital
import kotlinx.coroutines.flow.Flow

@Dao
interface HospitalDao {

    @Query("SELECT * FROM hospitals ORDER BY name ASC")
    fun getAllHospitals(): Flow<List<Hospital>>

    @Query("SELECT * FROM hospitals WHERE facilityType = :facilityType ORDER BY name ASC")
    fun getByFacilityType(facilityType: String): Flow<List<Hospital>>

    @Query("SELECT * FROM hospitals WHERE id = :id")
    suspend fun getHospitalById(id: Long): Hospital?

    @Query("SELECT * FROM hospitals WHERE name LIKE '%' || :query || '%' COLLATE NOCASE OR address LIKE '%' || :query || '%' COLLATE NOCASE")
    fun searchHospitals(query: String): Flow<List<Hospital>>

    @Query("SELECT * FROM hospitals WHERE facilityType = :facilityType AND (name LIKE '%' || :query || '%' COLLATE NOCASE OR address LIKE '%' || :query || '%' COLLATE NOCASE)")
    fun searchByFacilityType(query: String, facilityType: String): Flow<List<Hospital>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(hospital: Hospital): Long

    @Update
    suspend fun update(hospital: Hospital)

    @Delete
    suspend fun delete(hospital: Hospital)
}
