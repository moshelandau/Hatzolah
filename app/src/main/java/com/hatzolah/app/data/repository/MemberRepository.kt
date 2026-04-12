package com.hatzolah.app.data.repository

import com.hatzolah.app.data.database.dao.MemberDao
import com.hatzolah.app.data.database.entity.Member
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemberRepository @Inject constructor(
    private val memberDao: MemberDao
) {
    fun getAllMembers(): Flow<List<Member>> = memberDao.getAllMembers()

    fun getVerifiedMembers(): Flow<List<Member>> = memberDao.getVerifiedMembers()

    fun searchMembers(query: String): Flow<List<Member>> = memberDao.searchMembers(query)

    suspend fun getMemberById(id: Long): Member? = memberDao.getMemberById(id)

    suspend fun getMemberByPhone(phone: String): Member? = memberDao.getMemberByPhone(phone)

    suspend fun getMemberByUnit(unit: String): Member? = memberDao.getMemberByUnit(unit)

    suspend fun addMember(member: Member): Long = memberDao.insert(member)

    suspend fun updateMember(member: Member) = memberDao.update(member)

    suspend fun deleteMember(member: Member) = memberDao.delete(member)

    suspend fun verifyMember(memberId: Long) = memberDao.setVerified(memberId, true)
}
