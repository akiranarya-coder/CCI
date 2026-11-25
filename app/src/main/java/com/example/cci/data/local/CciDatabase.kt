package com.example.cci.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.cci.core.constants.AppConstants
import com.example.cci.core.constants.UserRole
import com.example.cci.core.security.PasswordHasher
import com.example.cci.core.utils.DateTimeUtils
import com.example.cci.data.local.dao.AlertDao
import com.example.cci.data.local.dao.AuditLogDao
import com.example.cci.data.local.dao.ChangeoverDao
import com.example.cci.data.local.dao.CrusherStatusDao
import com.example.cci.data.local.dao.DailyReportDao
import com.example.cci.data.local.dao.MaintenanceDao
import com.example.cci.data.local.dao.TestDao
import com.example.cci.data.local.dao.UserDao
import com.example.cci.data.local.entity.AlertEntity
import com.example.cci.data.local.entity.AuditLogEntity
import com.example.cci.data.local.entity.CciTestEntity
import com.example.cci.data.local.entity.ChangeoverEntity
import com.example.cci.data.local.entity.CrusherStatusEntity
import com.example.cci.data.local.entity.DailyReport
import com.example.cci.data.local.entity.MaintenanceEntity
import com.example.cci.data.local.entity.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        CciTestEntity::class,
        CrusherStatusEntity::class,
        ChangeoverEntity::class,
        MaintenanceEntity::class,
        AuditLogEntity::class,
        AlertEntity::class,
        DailyReport::class
    ],
    version = 2,
    exportSchema = false
)
abstract class CciDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun testDao(): TestDao
    abstract fun crusherStatusDao(): CrusherStatusDao
    abstract fun changeoverDao(): ChangeoverDao
    abstract fun maintenanceDao(): MaintenanceDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun alertDao(): AlertDao
    abstract fun dailyReportDao(): DailyReportDao

    companion object {
        @Volatile
        private var INSTANCE: CciDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `daily_reports` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `reportDate` TEXT NOT NULL,
                        `shiftScope` TEXT NOT NULL,
                        `runningLine` TEXT NOT NULL,
                        `standbyLine` TEXT NOT NULL,
                        `rollerGapMm` REAL NOT NULL,
                        `feedRateTph` REAL NOT NULL,
                        `rollerPressureBar` REAL NOT NULL,
                        `cokeMoisturePct` REAL NOT NULL,
                        `sampleCount` INTEGER NOT NULL,
                        `avgBeforeFrac050` REAL NOT NULL,
                        `avgBeforeFrac100` REAL NOT NULL,
                        `avgBeforeFrac300` REAL NOT NULL,
                        `avgBeforeFracPlus3` REAL NOT NULL,
                        `avgAfterFrac050` REAL NOT NULL,
                        `avgAfterFrac100` REAL NOT NULL,
                        `avgAfterFrac300` REAL NOT NULL,
                        `avgAfterFracPlus3` REAL NOT NULL,
                        `avgBeforeFinesPct` REAL NOT NULL,
                        `avgAfterFinesPct` REAL NOT NULL,
                        `avgCrushingIndex` REAL NOT NULL,
                        `minCrushingIndex` REAL NOT NULL,
                        `maxCrushingIndex` REAL NOT NULL,
                        `avgFinesIncreasePp` REAL NOT NULL,
                        `targetCompliancePct` REAL NOT NULL,
                        `changeoverCount` INTEGER NOT NULL,
                        `maintenanceCount` INTEGER NOT NULL,
                        `remarks` TEXT NOT NULL,
                        `summaryText` TEXT NOT NULL,
                        `createdAt` TEXT NOT NULL,
                        `createdBy` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_reports_reportDate` ON `daily_reports` (`reportDate`)")
            }
        }

        fun getInstance(context: Context): CciDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CciDatabase::class.java,
                    "evonith_cci.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .addCallback(SeedDatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun ensureSeeded(db: CciDatabase) {
            seedInitialData(db)
        }

        private suspend fun seedInitialData(db: CciDatabase) {
            val now = DateTimeUtils.nowDateTimeIso()
            val today = DateTimeUtils.todayIso()

            if (db.userDao().getCount() == 0) {
                val defaultUsers = listOf(
                    UserEntity(
                        username = "admin",
                        fullName = "Plant System Administrator",
                        passwordHash = PasswordHasher.hashPassword("admin123"),
                        role = UserRole.ADMIN.name,
                        createdAt = now,
                        createdBy = "system",
                        updatedAt = now,
                        updatedBy = "system"
                    ),
                    UserEntity(
                        username = "shiftincharge",
                        fullName = "Sinter Shift In-Charge",
                        passwordHash = PasswordHasher.hashPassword("shift123"),
                        role = UserRole.SHIFT_IN_CHARGE.name,
                        createdAt = now,
                        createdBy = "system",
                        updatedAt = now,
                        updatedBy = "system"
                    ),
                    UserEntity(
                        username = "operator",
                        fullName = "Lab Technician / Operator",
                        passwordHash = PasswordHasher.hashPassword("oper123"),
                        role = UserRole.OPERATOR.name,
                        createdAt = now,
                        createdBy = "system",
                        updatedAt = now,
                        updatedBy = "system"
                    ),
                    UserEntity(
                        username = "maintenance",
                        fullName = "Crusher Maintenance Engineer",
                        passwordHash = PasswordHasher.hashPassword("maint123"),
                        role = UserRole.MAINTENANCE.name,
                        createdAt = now,
                        createdBy = "system",
                        updatedAt = now,
                        updatedBy = "system"
                    ),
                    UserEntity(
                        username = "management",
                        fullName = "Sinter Plant Management",
                        passwordHash = PasswordHasher.hashPassword("mgmt123"),
                        role = UserRole.MANAGEMENT.name,
                        createdAt = now,
                        createdBy = "system",
                        updatedAt = now,
                        updatedBy = "system"
                    )
                )
                defaultUsers.forEach { db.userDao().insert(it) }
            }

            if (db.crusherStatusDao().getStatus() == null) {
                val initDt = "${DateTimeUtils.daysAgoIso(5)}T08:00"
                db.crusherStatusDao().upsert(
                    CrusherStatusEntity(
                        id = 1,
                        runningLine = AppConstants.LINE_01,
                        standbyLine = AppConstants.LINE_02,
                        lastChangeoverAt = initDt,
                        updatedAt = initDt,
                        updatedBy = "shiftincharge"
                    )
                )
                db.changeoverDao().insert(
                    ChangeoverEntity(
                        eventDateTime = initDt,
                        previousLine = AppConstants.LINE_02,
                        newLine = AppConstants.LINE_01,
                        newStandbyLine = AppConstants.LINE_02,
                        reason = "Scheduled Monthly Changeover",
                        manualOverride = false,
                        authorizedBy = "shiftincharge",
                        remarks = "Initial scheduled monthly rotation confirmed.",
                        createdAt = initDt,
                        createdBy = "shiftincharge",
                        updatedAt = initDt,
                        updatedBy = "shiftincharge"
                    )
                )
            }

            if (db.testDao().getActiveCount() == 0) {
                data class DemoSeed(
                    val daysAgo: Int,
                    val time: String,
                    val shift: String,
                    val line: String,
                    val source: String,
                    val b05: Double,
                    val b10: Double,
                    val b30: Double,
                    val bPlus: Double,
                    val a05: Double,
                    val a10: Double,
                    val a30: Double,
                    val aPlus: Double,
                    val remarks: String
                )

                val seeds = listOf(
                    DemoSeed(4, "08:30", "A", AppConstants.LINE_02, "BF Stock House Bay-1", 2.50, 2.00, 1.50, 14.00, 4.10, 2.45, 2.10, 11.35, "Normal plant sample"),
                    DemoSeed(3, "15:15", "B", AppConstants.LINE_02, "BF Stock House Bay-2", 2.40, 2.10, 1.70, 13.80, 3.55, 2.20, 1.95, 12.30, "Slightly higher moisture — roller check noted"),
                    DemoSeed(2, "23:10", "C", AppConstants.LINE_01, "BF Stock House Bay-1", 2.20, 1.80, 1.40, 14.60, 4.20, 2.55, 2.20, 11.05, "Post-changeover stabilization"),
                    DemoSeed(1, "09:20", "A", AppConstants.LINE_01, "BF Stock House Bay-2", 2.30, 1.90, 1.60, 14.20, 4.05, 2.50, 2.15, 11.30, "Consistent crushing output"),
                    DemoSeed(0, "08:45", "A", AppConstants.LINE_01, "BF Stock House Bay-1", 2.15, 1.85, 1.55, 14.45, 4.15, 2.60, 2.25, 11.00, "Morning shift sample within target"),
                    DemoSeed(0, "15:30", "B", AppConstants.LINE_01, "BF Stock House Bay-2", 2.25, 1.95, 1.50, 14.30, 3.95, 2.45, 2.15, 11.45, "Steady feed rate, dry breeze coke")
                )

                val dateCounters = mutableMapOf<String, Int>()
                seeds.forEach { s ->
                    val dateStr = DateTimeUtils.daysAgoIso(s.daysAgo)
                    val seq = (dateCounters[dateStr] ?: 0) + 1
                    dateCounters[dateStr] = seq
                    val dtStr = "${dateStr}T${s.time}"
                    val bTotal = s.b05 + s.b10 + s.b30 + s.bPlus
                    val aTotal = s.a05 + s.a10 + s.a30 + s.aPlus
                    val bf = ((s.b05 + s.b10 + s.b30) / bTotal) * 100.0
                    val af = ((s.a05 + s.a10 + s.a30) / aTotal) * 100.0
                    val ci = af
                    val inc = af - bf

                    db.testDao().insert(
                        CciTestEntity(
                            sampleId = DateTimeUtils.generateSampleId(dateStr, seq),
                            testDateTime = dtStr,
                            testDate = dateStr,
                            shift = s.shift,
                            crusherLine = s.line,
                            cokeType = "Breeze Coke",
                            cokeSource = s.source,
                            operatorName = "operator",
                            beforeFrac050 = s.b05,
                            beforeFrac100 = s.b10,
                            beforeFrac300 = s.b30,
                            beforeFracPlus3 = s.bPlus,
                            beforeTotal = bTotal,
                            afterFrac050 = s.a05,
                            afterFrac100 = s.a10,
                            afterFrac300 = s.a30,
                            afterFracPlus3 = s.aPlus,
                            afterTotal = aTotal,
                            beforeFinesPct = bf,
                            afterFinesPct = af,
                            crushingIndex = ci,
                            finesIncreasePp = inc,
                            remarks = s.remarks,
                            createdAt = dtStr,
                            createdBy = "operator",
                            updatedAt = dtStr,
                            updatedBy = "operator"
                        )
                    )
                }

                db.maintenanceDao().insert(
                    MaintenanceEntity(
                        crusherLine = AppConstants.LINE_02,
                        activityDate = DateTimeUtils.daysAgoIso(6),
                        activityType = "Roller Turning",
                        details = "Routine roller surface turning and gap calibration to 3.0 mm.",
                        measurements = "Gap: 3.0 mm | Roll Dia: 998 mm",
                        rollerCondition = "Good after turning",
                        completedBy = "maintenance",
                        approvedBy = "shiftincharge",
                        nextDueDate = DateTimeUtils.daysFromNowIso(24),
                        createdAt = now,
                        createdBy = "maintenance",
                        updatedAt = now,
                        updatedBy = "shiftincharge"
                    )
                )
                db.maintenanceDao().insert(
                    MaintenanceEntity(
                        crusherLine = AppConstants.LINE_01,
                        activityDate = today,
                        activityType = "Gap Calibration",
                        details = "Mid-run roller nip inspection and hydraulic tension check.",
                        measurements = "Gap: 2.9 mm | Pressure: 145 bar",
                        rollerCondition = "Good / Uniform Surface",
                        completedBy = "maintenance",
                        approvedBy = "",
                        nextDueDate = DateTimeUtils.daysFromNowIso(14),
                        createdAt = now,
                        createdBy = "maintenance",
                        updatedAt = now,
                        updatedBy = "maintenance"
                    )
                )

                db.alertDao().insert(
                    AlertEntity(
                        timestamp = "${DateTimeUtils.daysAgoIso(3)}T15:18",
                        severity = "WARNING",
                        title = "CCI Below Target on Line-02",
                        message = "Shift B sample recorded 38.50% CCI (target >= 40.0%). Inspect roller gap and feed moisture.",
                        crusherLine = AppConstants.LINE_02,
                        isAcknowledged = false
                    )
                )

                db.auditLogDao().insert(
                    AuditLogEntity(
                        timestamp = now,
                        username = "system",
                        action = "INIT_SEED",
                        entityType = "DATABASE",
                        details = "Initialized Evonith CCI plant database with baseline records."
                    )
                )
            }

            if (db.dailyReportDao().getCount() == 0) {
                val yesterday = DateTimeUtils.daysAgoIso(1)
                db.dailyReportDao().insert(
                    DailyReport(
                        reportDate = yesterday,
                        shiftScope = "ALL",
                        runningLine = AppConstants.LINE_01,
                        standbyLine = AppConstants.LINE_02,
                        rollerGapMm = 2.9,
                        feedRateTph = 44.0,
                        rollerPressureBar = 145.0,
                        cokeMoisturePct = 5.4,
                        sampleCount = 1,
                        avgBeforeFrac050 = 2.30,
                        avgBeforeFrac100 = 1.90,
                        avgBeforeFrac300 = 1.60,
                        avgBeforeFracPlus3 = 14.20,
                        avgAfterFrac050 = 4.05,
                        avgAfterFrac100 = 2.50,
                        avgAfterFrac300 = 2.15,
                        avgAfterFracPlus3 = 11.30,
                        avgBeforeFinesPct = 29.00,
                        avgAfterFinesPct = 43.50,
                        avgCrushingIndex = 43.50,
                        minCrushingIndex = 43.50,
                        maxCrushingIndex = 43.50,
                        avgFinesIncreasePp = 14.50,
                        targetCompliancePct = 100.0,
                        changeoverCount = 0,
                        maintenanceCount = 0,
                        remarks = "Consistent crushing output on Line-01 across shifts.",
                        summaryText = "",
                        createdAt = "${yesterday}T22:00",
                        createdBy = "shiftincharge"
                    )
                )
            }
        }
    }

    private class SeedDatabaseCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    seedInitialData(database)
                }
            }
        }
    }
}
