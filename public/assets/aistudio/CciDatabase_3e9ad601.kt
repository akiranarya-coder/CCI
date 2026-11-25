package com.evonith.cci.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.evonith.cci.core.constants.AppConstants
import com.evonith.cci.core.constants.UserRole
import com.evonith.cci.core.security.PasswordHasher
import com.evonith.cci.data.local.dao.AlertDao
import com.evonith.cci.data.local.dao.AuditLogDao
import com.evonith.cci.data.local.dao.ChangeoverDao
import com.evonith.cci.data.local.dao.CrusherStatusDao
import com.evonith.cci.data.local.dao.MaintenanceDao
import com.evonith.cci.data.local.dao.TestDao
import com.evonith.cci.data.local.dao.UserDao
import com.evonith.cci.data.local.entity.AlertEntity
import com.evonith.cci.data.local.entity.AuditLogEntity
import com.evonith.cci.data.local.entity.CciTestEntity
import com.evonith.cci.data.local.entity.ChangeoverEntity
import com.evonith.cci.data.local.entity.CrusherStatusEntity
import com.evonith.cci.data.local.entity.MaintenanceEntity
import com.evonith.cci.data.local.entity.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Database(
    entities = [
        UserEntity::class,
        CciTestEntity::class,
        CrusherStatusEntity::class,
        ChangeoverEntity::class,
        MaintenanceEntity::class,
        AuditLogEntity::class,
        AlertEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class CciDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun testDao(): TestDao
    abstract fun crusherStatusDao(): CrusherStatusDao
    abstract fun changeoverDao(): ChangeoverDao
    abstract fun maintenanceDao(): MaintenanceDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun alertDao(): AlertDao

    companion object {
        @Volatile
        private var INSTANCE: CciDatabase? = null

        fun getInstance(context: Context): CciDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CciDatabase::class.java,
                    "evonith_cci.db"
                )
                    .addCallback(SeedDatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
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

        private suspend fun seedInitialData(db: CciDatabase) {
            val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))
            val today = LocalDate.now()

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
                val initDt = today.minusDays(5).toString() + "T08:00"
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
                val demoTests = listOf(
                    Triple(today.minusDays(4), "A", AppConstants.LINE_02) to Triple(2.50, 2.00, 1.50) to Pair(14.00, 4.10),
                    Triple(today.minusDays(3), "B", AppConstants.LINE_02) to Triple(2.40, 2.10, 1.70) to Pair(13.80, 4.25),
                    Triple(today.minusDays(2), "C", AppConstants.LINE_01) to Triple(2.20, 1.80, 1.40) to Pair(14.60, 3.80),
                    Triple(today.minusDays(1), "A", AppConstants.LINE_01) to Triple(2.30, 1.90, 1.60) to Pair(14.20, 4.05),
                    Triple(today, "B", AppConstants.LINE_01) to Triple(2.15, 1.85, 1.55) to Pair(14.45, 3.95)
                )
                demoTests.forEachIndexed { idx, (metaAndBefore, afterPair) ->
                    val (meta, beforeF) = metaAndBefore
                    val (d, shift, line) = meta
                    val dateStr = d.toString()
                    val dtStr = "${dateStr}T0${8 + idx}:30"
                    val bw = listOf(beforeF.first, beforeF.second, beforeF.third, afterPair.first)
                    val bTotal = bw.sum()
                    val aw = listOf(afterPair.second, 2.45, 2.10, 11.50)
                    val aTotal = aw.sum()
                    val bf = (bw.take(3).sum() / bTotal) * 100.0
                    val af = (aw.take(3).sum() / aTotal) * 100.0
                    val ci = af
                    val inc = af - bf
                    db.testDao().insert(
                        CciTestEntity(
                            sampleId = "CCI-${dateStr.replace("-", "")}-0${idx + 1}",
                            testDateTime = dtStr,
                            testDate = dateStr,
                            shift = shift,
                            crusherLine = line,
                            cokeType = "Breeze Coke",
                            cokeSource = if (idx % 2 == 0) "BF Stock House Bay-1" else "BF Stock House Bay-2",
                            operatorName = "operator",
                            beforeFrac050 = bw[0],
                            beforeFrac100 = bw[1],
                            beforeFrac300 = bw[2],
                            beforeFracPlus3 = bw[3],
                            beforeTotal = bTotal,
                            afterFrac050 = aw[0],
                            afterFrac100 = aw[1],
                            afterFrac300 = aw[2],
                            afterFracPlus3 = aw[3],
                            afterTotal = aTotal,
                            beforeFinesPct = bf,
                            afterFinesPct = af,
                            crushingIndex = ci,
                            finesIncreasePp = inc,
                            remarks = if (idx == 1) "Slightly higher moisture" else "Normal plant sample",
                            createdAt = dtStr,
                            createdBy = "operator",
                            updatedAt = dtStr,
                            updatedBy = "operator"
                        )
                    )
                }
            }

            db.maintenanceDao().insert(
                MaintenanceEntity(
                    crusherLine = AppConstants.LINE_02,
                    activityDate = today.minusDays(6).toString(),
                    activityType = "Roller Turning",
                    details = "Routine roller surface turning and gap calibration to 3.0 mm.",
                    measurements = "Gap: 3.0 mm | Roll Dia: 998 mm",
                    rollerCondition = "Good after turning",
                    completedBy = "maintenance",
                    approvedBy = "shiftincharge",
                    nextDueDate = today.plusDays(24).toString(),
                    createdAt = now,
                    createdBy = "maintenance",
                    updatedAt = now,
                    updatedBy = "shiftincharge"
                )
            )
        }
    }
}
