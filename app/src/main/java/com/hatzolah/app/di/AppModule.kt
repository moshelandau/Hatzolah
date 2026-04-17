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
import com.hatzolah.app.util.PrepopulatedMembers
import com.hatzolah.app.util.UrgentCareSeed
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE members ADD COLUMN unitNumber TEXT NOT NULL DEFAULT ''")
        }
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Back-fill real contact info for the 5 pre-populated urgent care
            // facilities that were originally inserted with empty fields, and
            // insert any new ones (Aizer, Carestier, Williamsburg Pediatrics).
            // We only update rows whose fields are still empty, so admins who
            // already entered data in their admin screen are never overwritten.
            seedUrgentCares(db)
        }
    }

    /**
     * Corrects fields for a few rows in the urgent care list:
     *  - Dr. Korngold / Aizer Health / Dr. Wertzberger: rewrite notes to
     *    flag that these aren't really walk-in urgent cares.
     *  - Zelcare: fill in the real street address ("3 Hamaspik Way").
     *
     * Force-rewrites only happen when the current value is empty or still
     * matches the previous seed text, so any admin-edited rows are
     * preserved. New installs pick up the same text via [UrgentCareSeed].
     */
    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // First, make sure the rows exist and have at least the address/
            // phone filled in (handles the case of an install that skipped an
            // earlier migration for any reason).
            seedUrgentCares(db)

            // Then force-update the notes for these specific rows, but only
            // if the current notes are empty or still match the previous seed
            // text — so we don't clobber manually-edited rows.
            val oldKorngold = "Plastic surgery \u00B7 Dr. Jay M Korngold / Dr. Louis Korngold \u00B7 " +
                    "Contact KY-18 Yoely Gross (845-537-1137) to coordinate patient " +
                    "photos and info with Dr. Korngold"
            val oldAizer = "Ext 4000 \u00B7 Mon\u2013Thu 9am\u20138pm \u00B7 Fri 9am\u20135pm \u00B7 Sun 9am\u20135pm \u00B7 Formerly Ezras Choilim"
            val oldWertzberger = "Best Healthcare \u00B7 Pediatrics (Dr. Alan Werzberger)"

            for (entry in UrgentCareSeed.entries) {
                val oldNotes = when (entry.name) {
                    "Dr. Korngold" -> oldKorngold
                    "Aizer Health" -> oldAizer
                    "Dr. Wertzberger" -> oldWertzberger
                    else -> continue
                }
                db.execSQL(
                    "UPDATE hospitals SET additionalNotes = ? " +
                            "WHERE name = ? AND facilityType = ? " +
                            "AND (additionalNotes IS NULL OR additionalNotes = '' OR additionalNotes = ?)",
                    arrayOf(entry.notes, entry.name, Hospital.FACILITY_URGENT_CARE, oldNotes)
                )
            }

            // Force-update Zelcare's address when it still matches the stale
            // "Monroe, NY 10950" placeholder the previous seed wrote. Admin-
            // edited addresses (anything else) are left alone.
            db.execSQL(
                "UPDATE hospitals SET address = ? " +
                        "WHERE name = ? AND facilityType = ? " +
                        "AND (address IS NULL OR address = '' OR address = ?)",
                arrayOf(
                    "3 Hamaspik Way, Monroe, NY 10950",
                    "Zelcare",
                    Hospital.FACILITY_URGENT_CARE,
                    "Monroe, NY 10950"
                )
            )
        }
    }

    /**
     * v6 → v7:
     *  1. Backfills lat/lng for urgent care rows that are still at (0.0, 0.0).
     *  2. Updates St Lukes Newburgh: phone, door code, and more precise coords.
     */
    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. Urgent care coordinates backfill.
            for (entry in UrgentCareSeed.entries) {
                db.execSQL(
                    "UPDATE hospitals SET latitude = ?, longitude = ? " +
                            "WHERE name = ? AND facilityType = ? " +
                            "AND (latitude = 0.0 OR longitude = 0.0)",
                    arrayOf(
                        entry.latitude,
                        entry.longitude,
                        entry.name,
                        Hospital.FACILITY_URGENT_CARE
                    )
                )
            }

            // 2. St Lukes Newburgh: new phone, door code, precise coords.
            db.execSQL(
                "UPDATE hospitals SET " +
                        "mainHotline = ?, erLocation = ?, " +
                        "latitude = ?, longitude = ? " +
                        "WHERE name = ? AND facilityType = ?",
                arrayOf(
                    "845-568-2305", "ER door 357#",
                    41.503706, -74.014901,
                    "St Lukes Newburgh", Hospital.FACILITY_HOSPITAL
                )
            )
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Pre-populate the team roster for existing installs. Skips rows
            // whose phone number is already in the database so we don't
            // duplicate the admin member.
            insertPrepopulatedMembers(db)
            // Make sure the admin member has their unit number set.
            db.execSQL(
                "UPDATE members SET unitNumber = 'KY85' WHERE phoneNumber = ? AND (unitNumber IS NULL OR unitNumber = '')",
                arrayOf(PrepopulatedMembers.ADMIN_PHONE)
            )
        }
    }

    /**
     * Inserts every entry from [PrepopulatedMembers.all] into the members
     * table, skipping any row whose phoneNumber already exists. Called from
     * both `onCreate` (fresh installs) and `MIGRATION_3_4` (existing installs).
     */
    private fun insertPrepopulatedMembers(db: SupportSQLiteDatabase) {
        val now = System.currentTimeMillis()
        // Guard with NOT EXISTS so re-running the migration or mixing with
        // onCreate's admin insert doesn't create duplicates.
        val sql = "INSERT INTO members (name, phoneNumber, whatsappContact, email, unitNumber, isVerified, isAdmin, createdAt) " +
                "SELECT ?, ?, ?, ?, ?, ?, ?, ? " +
                "WHERE NOT EXISTS (SELECT 1 FROM members WHERE phoneNumber = ?)"
        for (entry in PrepopulatedMembers.all) {
            val isAdmin = if (entry.phone == PrepopulatedMembers.ADMIN_PHONE) 1 else 0
            db.execSQL(
                sql,
                arrayOf(entry.name, entry.phone, "", "", entry.unitNumber, 1, isAdmin, now, entry.phone)
            )
        }
    }

    /**
     * Seeds the real urgent care contact info from [UrgentCareSeed]. For each
     * entry:
     *  - If a row with the same name already exists, its empty fields are
     *    filled in (but admin-edited fields are preserved).
     *  - If no row exists, a new one is inserted.
     *
     * Also renames the legacy "Dr. Korengold" row to "Dr. Korngold" so the
     * entries match the real spelling.
     *
     * Called from both onCreate (fresh install) and MIGRATION_4_5 (upgrade).
     */
    private fun seedUrgentCares(db: SupportSQLiteDatabase) {
        // One-time rename of the legacy row created by MIGRATION_1_2.
        db.execSQL(
            "UPDATE hospitals SET name = 'Dr. Korngold' WHERE name = 'Dr. Korengold' AND facilityType = ?",
            arrayOf(Hospital.FACILITY_URGENT_CARE)
        )

        val insertSql = "INSERT INTO hospitals (name, address, erLocation, accessCodes, kosherRoomLocation, patientAssistanceNotes, latitude, longitude, mainHotline, obHotline, departmentHotlines, communicationSystem, bedAvailability, additionalNotes, facilityType) " +
                "SELECT ?, ?, '', '', '', '', ?, ?, ?, '', '', '', '', ?, ? " +
                "WHERE NOT EXISTS (SELECT 1 FROM hospitals WHERE name = ? AND facilityType = ?)"

        // Only fill in empty fields — never clobber admin edits.
        val updateSql = "UPDATE hospitals SET " +
                "address = CASE WHEN address IS NULL OR address = '' THEN ? ELSE address END, " +
                "mainHotline = CASE WHEN mainHotline IS NULL OR mainHotline = '' THEN ? ELSE mainHotline END, " +
                "additionalNotes = CASE WHEN additionalNotes IS NULL OR additionalNotes = '' THEN ? ELSE additionalNotes END, " +
                "latitude = CASE WHEN latitude = 0.0 THEN ? ELSE latitude END, " +
                "longitude = CASE WHEN longitude = 0.0 THEN ? ELSE longitude END " +
                "WHERE name = ? AND facilityType = ?"

        for (entry in UrgentCareSeed.entries) {
            db.execSQL(
                insertSql,
                arrayOf(
                    entry.name, entry.address,
                    entry.latitude, entry.longitude,
                    entry.phone, entry.notes,
                    Hospital.FACILITY_URGENT_CARE,
                    entry.name, Hospital.FACILITY_URGENT_CARE
                )
            )
            db.execSQL(
                updateSql,
                arrayOf(
                    entry.address, entry.phone, entry.notes,
                    entry.latitude, entry.longitude,
                    entry.name, Hospital.FACILITY_URGENT_CARE
                )
            )
        }
    }

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
        ).addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Pre-populate the entire team roster (BLS + Medics) and flag
                // Moshe Yosef Landau (KY85) as the admin.
                insertPrepopulatedMembers(db)
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
                    arrayOf("St Lukes Newburgh", "70 Dubois St, Newburgh, NY 12550", "ER door 357#", "", "", "", "41.503706", "-74.014901", "845-568-2305", "", "", "", "", "", Hospital.FACILITY_HOSPITAL),
                    arrayOf("NewYork-Presbyterian Morgan Stanley Children's Hospital", "3959 Broadway, New York, NY 10032", "", "", "", "", "40.8403", "-73.9418", "", "", "", "", "", "Columbia Pediatrics", Hospital.FACILITY_HOSPITAL),
                    arrayOf("NewYork-Presbyterian Emergency Room", "622 W 168th St, New York, NY 10032", "", "", "", "", "40.8421", "-73.9422", "", "", "", "", "", "Columbia Adults", Hospital.FACILITY_HOSPITAL),
                    arrayOf("Montefiore Medical Center Moses Campus ER", "3415 Bainbridge Ave, Bronx, NY 10467", "", "", "", "", "40.8811", "-73.8814", "718-920-5731", "", "", "", "", "", Hospital.FACILITY_HOSPITAL)
                )
                for (h in hospitals) {
                    db.execSQL(insertSql, h)
                }
                // Urgent care facilities are seeded from the shared list so
                // the data stays in sync with MIGRATION_4_5.
                seedUrgentCares(db)
            }
        }).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
            .fallbackToDestructiveMigration().build()
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
