package com.hatzolah.app.data.database.dao

import androidx.room.*
import com.hatzolah.app.data.database.entity.Member
import kotlinx.coroutines.flow.Flow

@Dao
interface MemberDao {

    @Query("SELECT * FROM members ORDER BY name ASC")
    fun getAllMembers(): Flow<List<Member>>

    @Query("SELECT * FROM members WHERE id = :id")
    suspend fun getMemberById(id: Long): Member?

    @Query("SELECT * FROM members WHERE phoneNumber = :phone LIMIT 1")
    suspend fun getMemberByPhone(phone: String): Member?

    @Query("SELECT * FROM members WHERE isVerified = 1 ORDER BY name ASC")
    fun getVerifiedMembers(): Flow<List<Member>>

    @Query("SELECT * FROM members WHERE name LIKE '%' || :query || '%' OR phoneNumber LIKE '%' || :query || '%'")
    fun searchMembers(query: String): Flow<List<Member>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(member: Member): Long

    @Update
    suspend fun update(member: Member)

    @Delete
    suspend fun delete(member: Member)

    @Query("UPDATE members SET isVerified = :verified WHERE id = :memberId")
    suspend fun setVerified(memberId: Long, verified: Boolean)
}
