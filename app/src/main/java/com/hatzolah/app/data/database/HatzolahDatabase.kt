package com.hatzolah.app.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.hatzolah.app.data.database.dao.CallLogDao
import com.hatzolah.app.data.database.dao.HospitalDao
import com.hatzolah.app.data.database.dao.MemberDao
import com.hatzolah.app.data.database.dao.ResidentDao
import com.hatzolah.app.data.database.dao.SupplyRequestDao
import com.hatzolah.app.data.database.entity.CallLog
import com.hatzolah.app.data.database.entity.Hospital
import com.hatzolah.app.data.database.entity.Member
import com.hatzolah.app.data.database.entity.Resident
import com.hatzolah.app.data.database.entity.SupplyRequest

@Database(
    entities = [
        Member::class,
        Hospital::class,
        CallLog::class,
        Resident::class,
        SupplyRequest::class
    ],
    version = 2,
    exportSchema = false
)
abstract class HatzolahDatabase : RoomDatabase() {
    abstract fun memberDao(): MemberDao
    abstract fun hospitalDao(): HospitalDao
    abstract fun callLogDao(): CallLogDao
    abstract fun residentDao(): ResidentDao
    abstract fun supplyRequestDao(): SupplyRequestDao
}
