package com.hatzolah.app.data.repository

import com.hatzolah.app.data.database.dao.ResidentDao
import com.hatzolah.app.data.database.entity.Resident
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResidentRepository @Inject constructor(
    private val residentDao: ResidentDao
) {
    fun getAllResidents(): Flow<List<Resident>> = residentDao.getAllResidents()

    fun search(query: String): Flow<List<Resident>> = residentDao.search(query)

    suspend fun getResidentById(id: Long): Resident? = residentDao.getResidentById(id)

    suspend fun getSonsOf(fatherId: Long): List<Resident> = residentDao.getSonsOf(fatherId)

    suspend fun getSonsInLawOf(fatherInLawId: Long): List<Resident> =
        residentDao.getSonsInLawOf(fatherInLawId)

    suspend fun addResident(resident: Resident): Long = residentDao.insert(resident)

    suspend fun addAll(residents: List<Resident>): List<Long> = residentDao.insertAll(residents)

    suspend fun updateResident(resident: Resident) = residentDao.update(resident)

    suspend fun deleteResident(resident: Resident) = residentDao.delete(resident)

    suspend fun deleteAll() = residentDao.deleteAll()

    suspend fun count(): Int = residentDao.count()

    /**
     * Import residents from a CSV string.
     * Expected CSV columns (pipe-separated for flexibility with commas in addresses):
     *   phone|address|firstNameEn|lastNameEn|firstNameHe|lastNameHe|nicknameEn|nicknameHe|notes
     *
     * Lines starting with # are skipped as comments.
     * Empty fields are allowed.
     */
    suspend fun importFromCsv(csv: String): Int {
        val residents = csv.lines()
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .mapNotNull { line ->
                val parts = line.split("|")
                if (parts.isEmpty() || parts.size < 2) return@mapNotNull null
                Resident(
                    phoneNumber = parts.getOrNull(0)?.trim().orEmpty(),
                    address = parts.getOrNull(1)?.trim().orEmpty(),
                    firstNameEn = parts.getOrNull(2)?.trim().orEmpty(),
                    lastNameEn = parts.getOrNull(3)?.trim().orEmpty(),
                    firstNameHe = parts.getOrNull(4)?.trim().orEmpty(),
                    lastNameHe = parts.getOrNull(5)?.trim().orEmpty(),
                    nicknameEn = parts.getOrNull(6)?.trim().orEmpty(),
                    nicknameHe = parts.getOrNull(7)?.trim().orEmpty(),
                    notes = parts.getOrNull(8)?.trim().orEmpty()
                )
            }
            .toList()
        if (residents.isNotEmpty()) {
            residentDao.insertAll(residents)
        }
        return residents.size
    }
}
