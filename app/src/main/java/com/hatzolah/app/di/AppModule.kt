package com.hatzolah.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.hatzolah.app.data.database.HatzolahDatabase
import com.hatzolah.app.data.database.dao.CallLogDao
import com.hatzolah.app.data.database.dao.HospitalDao
import com.hatzolah.app.data.database.dao.MemberDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HatzolahDatabase {
        return Room.databaseBuilder(
            context,
            HatzolahDatabase::class.java,
            "hatzolah_database"
        ).addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Pre-populate admin member and test member (all NOT NULL columns must be specified)
                db.execSQL("INSERT INTO members (name, phoneNumber, whatsappContact, email, isVerified, isAdmin, createdAt) VALUES ('Moshe Landau', '8455008085', '', '', 1, 1, ${System.currentTimeMillis()})")
                db.execSQL("INSERT INTO members (name, phoneNumber, whatsappContact, email, isVerified, isAdmin, createdAt) VALUES ('Test Number', '8454810055', '', '', 1, 0, ${System.currentTimeMillis()})")

                // Pre-populate hospitals
                // Columns: name, address, erLocation, accessCodes, kosherRoomLocation, patientAssistanceNotes, latitude, longitude, mainHotline, obHotline, departmentHotlines, communicationSystem, bedAvailability, additionalNotes
                val cols = "name, address, erLocation, accessCodes, kosherRoomLocation, patientAssistanceNotes, latitude, longitude, mainHotline, obHotline, departmentHotlines, communicationSystem, bedAvailability, additionalNotes"
                val hospitals = listOf(
                    "('ORMC', '707 E Main St, Middletown, NY 10940', 'ER door 1281, EMS room 3540', 'Door to ER from Shabbos room 354*', '', '', 0.0, 0.0, '845-333-1700', '', '', '', '', '')",
                    "('Good Samaritan Hospital', '257 Lafayette Ave, Suffern, NY 10901', 'ER door 119*, EMS #4394', 'Door to OB Elevator 1254*', '', '', 0.0, 0.0, '845-368-5029', '845-671-0738', '', '', '', '')",
                    "('Westchester Medical Center', '100 Woods Rd, Valhalla, NY 10595', 'ER 803*', '', '', '', 0.0, 0.0, '914-493-7307', '', '{\"Peds\":\"914-493-6001\",\"Adult\":\"914-493-6000\"}', '', '', '')",
                    "('St Anthony Community Hospital', '15 Maple Ave, Warwick, NY 10990', '', '', '', '', 0.0, 0.0, '845-987-5125', '', '', '', '', '')",
                    "('Valley Hospital', '', 'Door# 07652, Door 07450 or 10950', '', '', '', 0.0, 0.0, '201-251-3465', '', '', '', '', '')",
                    "('NYU Langone ER', '', 'ER code 0911', '', '', '', 0.0, 0.0, '', '', '', '', '', '')",
                    "('Harris ER', '', 'ER 796*, disp code 791', '', '', '', 0.0, 0.0, '', '', '', '', '', '')",
                    "('Ellenville Regional Hospital', '', 'Code 552', '', '', '', 0.0, 0.0, '', '', '', '', '', '')",
                    "('Lenox Hill Hospital ER', '', 'ER code 9111', '', '', '', 0.0, 0.0, '', '', '', '', '', '')",
                    "('Hackensack University Medical Center', '', '', '', '', '', 0.0, 0.0, '', '', '{\"Peds\":\"551-996-5430\",\"Peds2\":\"551-996-8912\",\"Adult ER\":\"551-996-2612\"}', '', '', '')",
                    "('Maimonides Medical Center', '', 'Code 0911', '', '', '', 0.0, 0.0, '', '', '', '', '', '')",
                    "('Stamford Hospital', '', 'Code 6-7-8-9', '', '', '', 0.0, 0.0, '', '', '', '', '', '')",
                    "('Englewood Hospital', '', 'ER Door 90211', '', '', '', 0.0, 0.0, '201-894-3960', '', '', '', '', '')",
                    "('St Lukes Newburgh', '70 Dubois St, Newburgh, NY', 'ER door 134#', '', '', '', 0.0, 0.0, '845-561-4400', '', '', '', '', '')",
                    "('NewYork-Presbyterian Morgan Stanley Children''s Hospital', '3959 Broadway, New York, NY 10032', '', '', '', '', 0.0, 0.0, '', '', '', '', '', 'Columbia Pediatrics')",
                    "('NewYork-Presbyterian Emergency Room', '622 W 168th St, New York, NY 10032', '', '', '', '', 0.0, 0.0, '', '', '', '', '', 'Columbia Adults')",
                    "('Montefiore Medical Center Moses Campus ER', '3415 Bainbridge Ave, Bronx, NY', '', '', '', '', 0.0, 0.0, '718-920-5731', '', '', '', '', '')"
                )
                for (h in hospitals) {
                    db.execSQL("INSERT INTO hospitals ($cols) VALUES $h")
                }
            }
        }).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideMemberDao(database: HatzolahDatabase): MemberDao = database.memberDao()

    @Provides
    fun provideHospitalDao(database: HatzolahDatabase): HospitalDao = database.hospitalDao()

    @Provides
    fun provideCallLogDao(database: HatzolahDatabase): CallLogDao = database.callLogDao()
}
