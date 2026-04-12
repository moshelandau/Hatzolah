package com.hatzolah.app.data.repository

import com.hatzolah.app.data.database.dao.HospitalDao
import com.hatzolah.app.data.database.entity.Hospital
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HospitalRepository @Inject constructor(
    private val hospitalDao: HospitalDao
) {
    fun getAllHospitals(): Flow<List<Hospital>> = hospitalDao.getAllHospitals()

    fun getByFacilityType(facilityType: String): Flow<List<Hospital>> = hospitalDao.getByFacilityType(facilityType)

    fun searchHospitals(query: String): Flow<List<Hospital>> = hospitalDao.searchHospitals(query)

    fun searchByFacilityType(query: String, facilityType: String): Flow<List<Hospital>> = hospitalDao.searchByFacilityType(query, facilityType)

    suspend fun getHospitalById(id: Long): Hospital? = hospitalDao.getHospitalById(id)

    suspend fun addHospital(hospital: Hospital): Long = hospitalDao.insert(hospital)

    suspend fun updateHospital(hospital: Hospital) = hospitalDao.update(hospital)

    suspend fun deleteHospital(hospital: Hospital) = hospitalDao.delete(hospital)
}
