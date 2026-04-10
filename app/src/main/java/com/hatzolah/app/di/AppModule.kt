package com.hatzolah.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.hatzolah.app.data.database.HatzolahDatabase
import com.hatzolah.app.data.database.dao.CallLogDao
import com.hatzolah.app.data.database.dao.HospitalDao
import com.hatzolah.app.data.database.dao.MemberDao
import com.hatzolah.app.data.database.dao.ResidentDao
import com.hatzolah.app.data.database.dao.SupplyRequestDao
import com.hatzolah.app.data.database.entity.Hospital
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE hospitals ADD COLUMN facilityType TEXT NOT NULL DEFAULT 'HOSPITAL'")
            // Insert pre-populated urgent care facilities
            val insertSql = "INSERT INTO hospitals (name, address, erLocation, accessCodes, kosherRoomLocation, patientAssistanceNotes, latitude, longitude, mainHotline, obHotline, departmentHotlines, communicationSystem, bedAvailability, additionalNotes, facilityType) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
            val urgentCares = listOf(
                arrayOf("Rambam Urgent Care", "", "", "", "", "", "0.0", "0.0", "", "", "", "", "", "", Hospital.FACILITY_URGENT_CARE),
                arrayOf("Zelcare", "", "", "", "", "", "0.0", "0.0", "", "", "", "", "", "", Hospital.FACILITY_URGENT_CARE),
                arrayOf("Nestwell", "", "", "", "", "", "0.0", "0.0", "", "", "", "", "", "", Hospital.FACILITY_URGENT_CARE),
                arrayOf("Dr. Korengold", "", "", "", "", "", "0.0", "0.0", "", "", "", "", "", "", Hospital.FACILITY_URGENT_CARE),
                arrayOf("Dr. Wertzberger", "", "", "", "", "", "0.0", "0.0", "", "", "", "", "", "", Hospital.FACILITY_URGENT_CARE)
            )
            for (uc in urgentCares) {
                db.execSQL(insertSql, uc)
            }
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HatzolahDatabase {
        return Room.databaseBuilder(
            context,
            HatzolahDatabase::class.java,
            "hatzolah_db_v1b"
        ).addMigrations(MIGRATION_1_2).addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Pre-populate admin member and test member (all NOT NULL columns must be specified)
                db.execSQL("INSERT INTO members (name, phoneNumber, whatsappContact, email, isVerified, isAdmin, createdAt) VALUES (?, ?, ?, ?, ?, ?, ?)", arrayOf("Moshe Landau", "8455008085", "", "", 1, 1, System.currentTimeMillis()))
                // Test number 8454810055 is configured as dispatch_number in SharedPreferences for testing

                // Pre-populate hospitals using parameterized queries to avoid SQL injection issues
                // (e.g. hospital names containing apostrophes like "Children's Hospital")
                val insertSql = "INSERT INTO hospitals (name, address, erLocation, accessCodes, kosherRoomLocation, patientAssistanceNotes, latitude, longitude, mainHotline, obHotline, departmentHotlines, communicationSystem, bedAvailability, additionalNotes, facilityType) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                val hospitals = listOf(
                    // Hospitals
                    arrayOf("ORMC", "707 E Main St, Middletown, NY 10940", "ER door 1281, EMS room 3540", "Door to ER from Shabbos room 354*", "", "", "41.4459", "-74.4229", "845-333-1700", "", "", "", "", "", Hospital.FACILITY_HOSPITAL),
                    arrayOf("Good Samaritan Hospital", "257 Lafayette Ave, Suffern, NY 10901", "ER door 119*, EMS #4394", "Door to OB Elevator 1254*", "", "", "41.1148", "-74.1496", "845-368-5029", "845-671-0738", "", "", "", "", Hospital.FACILITY_HOSPITAL),
                    arrayOf("Westchester Medical Center", "100 Woods Rd, Valhalla, NY 10595", "ER 803*", "", "", "", "41.0759", "-73.7787", "914-493-7307", "", "{\"Peds\":\"914-493-6001\",\"Adult\":\"914-493-6000\"}", "", "", "", Hospital.FACILITY_HOSPITAL),
                    arrayOf("St Anthony Community Hospital", "15 Maple Ave, Warwick, NY 10990", "", "", "", "", "41.2565", "-74.3560", "845-987-5125", "", "", "", "", "", Hospital.FACILITY_HOSPITAL),
                    arrayOf("Valley Hospital", "223 N Van Dien Ave, Ridgewood, NJ 07450", "Door# 07652, Door 07450 or 10950", "", "", "", "40.9793", "-74.1166", "201-251-3465", "", "", "", "", "", Hospital.FACILITY_HOSPITAL),
                    arrayOf("NYU Langone ER", "550 1st Ave, New York, NY 10016", "ER code 0911", "", "", "", "40.7421", "-73.9739", "", "", "", "", "", "", Hospital.FACILITY_HOSPITAL),
                    arrayOf("Harris ER", "8118 13th Ave, Brooklyn, NY 11228", "ER 796*, disp code 791", "", "", "", "40.6198", "-74.0003", "", "", "", "", "", "", Hospital.FACILITY_HOSPITAL),
                    arrayOf("Ellenville Regional Hospital", "10 Healthy Way, Ellenville, NY 12428", "Code 552", "", "", "", "41.7172", "-74.3969", "", "", "", "", "", "", Hospital.FACILITY_HOSPITAL),
                    arrayOf("Lenox Hill Hospital ER", "100 E 77th St, New York, NY 10075", "ER code 9111", "", "", "", "40.7731", "-73.9622", "", "", "", "", "", "", Hospital.FACILITY_HOSPITAL),
                    arrayOf("Hackensack University Medical Center", "30 Prospect Ave, Hackensack, NJ 07601", "", "", "", "", "40.8856", "-74.0637", "", "", "{\"Peds\":\"551-996-5430\",\"Peds2\":\"551-996-8912\",\"Adult ER\":\"551-996-2612\"}", "", "", "", Hospital.FACILITY_HOSPITAL),
                    arrayOf("Maimonides Medical Center", "4802 10th Ave, Brooklyn, NY 11219", "Code 0911", "", "", "", "40.6354", "-73.9864", "", "", "", "", "", "", Hospital.FACILITY_HOSPITAL),
                    arrayOf("Stamford Hospital", "1 Hospital Plaza, Stamford, CT 06902", "Code 6-7-8-9", "", "", "", "41.0534", "-73.5387", "", "", "", "", "", "", Hospital.FACILITY_HOSPITAL),
                    arrayOf("Englewood Hospital", "350 Engle St, Englewood, NJ 07631", "ER Door 90211", "", "", "", "40.8932", "-73.9726", "201-894-3960", "", "", "", "", "", Hospital.FACILITY_HOSPITAL),
                    arrayOf("St Lukes Newburgh", "70 Dubois St, Newburgh, NY 12550", "ER door 134#", "", "", "", "41.5034", "-74.0104", "845-561-4400", "", "", "", "", "", Hospital.FACILITY_HOSPITAL),
                    arrayOf("NewYork-Presbyterian Morgan Stanley Children's Hospital", "3959 Broadway, New York, NY 10032", "", "", "", "", "40.8403", "-73.9418", "", "", "", "", "", "Columbia Pediatrics", Hospital.FACILITY_HOSPITAL),
                    arrayOf("NewYork-Presbyterian Emergency Room", "622 W 168th St, New York, NY 10032", "", "", "", "", "40.8421", "-73.9422", "", "", "", "", "", "Columbia Adults", Hospital.FACILITY_HOSPITAL),
                    arrayOf("Montefiore Medical Center Moses Campus ER", "3415 Bainbridge Ave, Bronx, NY 10467", "", "", "", "", "40.8811", "-73.8814", "718-920-5731", "", "", "", "", "", Hospital.FACILITY_HOSPITAL),
                    // Urgent Care Facilities
                    arrayOf("Rambam Urgent Care", "", "", "", "", "", "0.0", "0.0", "", "", "", "", "", "", Hospital.FACILITY_URGENT_CARE),
                    arrayOf("Zelcare", "", "", "", "", "", "0.0", "0.0", "", "", "", "", "", "", Hospital.FACILITY_URGENT_CARE),
                    arrayOf("Nestwell", "", "", "", "", "", "0.0", "0.0", "", "", "", "", "", "", Hospital.FACILITY_URGENT_CARE),
                    arrayOf("Dr. Korengold", "", "", "", "", "", "0.0", "0.0", "", "", "", "", "", "", Hospital.FACILITY_URGENT_CARE),
                    arrayOf("Dr. Wertzberger", "", "", "", "", "", "0.0", "0.0", "", "", "", "", "", "", Hospital.FACILITY_URGENT_CARE)
                )
                for (h in hospitals) {
                    db.execSQL(insertSql, h)
                }
            }
        }).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideMemberDao(database: HatzolahDatabase): MemberDao = database.memberDao()

    @Provides
    fun provideHospitalDao(database: HatzolahDatabase): HospitalDao = database.hospitalDao()

    @Provides
    fun provideResidentDao(database: HatzolahDatabase): ResidentDao = database.residentDao()

    @Provides
    fun provideSupplyRequestDao(database: HatzolahDatabase): SupplyRequestDao = database.supplyRequestDao()

    @Provides
    fun provideCallLogDao(database: HatzolahDatabase): CallLogDao = database.callLogDao()
}
