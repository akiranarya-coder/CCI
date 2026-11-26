package com.example.cci.data.repository

import androidx.room.withTransaction
import com.example.cci.core.constants.UserRole
import com.example.cci.core.security.ActiveSession
import com.example.cci.core.security.PasswordHasher
import com.example.cci.core.security.SessionManager
import com.example.cci.core.utils.DateTimeUtils
import com.example.cci.data.datastore.AppSettingsDataStore
import com.example.cci.data.datastore.PlantSettings
import com.example.cci.data.local.CciDatabase
import com.example.cci.data.local.dao.AlertDao
import com.example.cci.data.local.dao.AuditLogDao
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Locale

class AuthRepository(
    private val userDao: UserDao,
    private val auditLogDao: AuditLogDao,
    private val settingsDataStore: AppSettingsDataStore,
    private val sessionManager: SessionManager
) {
    val allUsers: Flow<List<UserEntity>> = userDao.observeAllUsers()

    suspend fun restoreSession() {
        val savedUsername = settingsDataStore.settingsFlow.first().loggedInUsername
        val user = userDao.findByUsername(savedUsername) ?: userDao.findByUsername("shiftincharge")
        if (user != null && user.isActive) {
            sessionManager.setSession(
                ActiveSession(
                    userId = user.id,
                    username = user.username,
                    fullName = user.fullName,
                    role = UserRole.fromName(user.role)
                )
            )
        }
    }

    suspend fun login(username: String, password: String): Result<ActiveSession> {
        val cleanUser = username.trim().lowercase(Locale.US)
        val user = userDao.findByUsername(cleanUser)
            ?: return Result.failure(IllegalArgumentException("User '$cleanUser' not found"))
        if (!user.isActive) {
            return Result.failure(IllegalStateException("Account is deactivated. Contact Admin."))
        }
        if (!PasswordHasher.verifyPassword(password, user.passwordHash)) {
            return Result.failure(IllegalArgumentException("Incorrect password"))
        }
        val session = ActiveSession(
            userId = user.id,
            username = user.username,
            fullName = user.fullName,
            role = UserRole.fromName(user.role)
        )
        sessionManager.setSession(session)
        settingsDataStore.setLoggedInUsername(user.username)
        auditLogDao.insert(
            AuditLogEntity(
                timestamp = DateTimeUtils.nowDateTimeIso(),
                username = user.username,
                action = "LOGIN",
                entityType = "USER",
                details = "Signed in as ${session.role.displayName}"
            )
        )
        return Result.success(session)
    }

    suspend fun createUser(
        username: String,
        fullName: String,
        password: String,
        role: UserRole
    ): Result<Unit> {
        val cleanUser = username.trim().lowercase(Locale.US)
        if (cleanUser.length < 3) return Result.failure(IllegalArgumentException("Username must be at least 3 characters"))
        if (password.length < 4) return Result.failure(IllegalArgumentException("Password must be at least 4 characters"))
        if (userDao.findByUsername(cleanUser) != null) {
            return Result.failure(IllegalArgumentException("Username '$cleanUser' already exists"))
        }
        val now = DateTimeUtils.nowDateTimeIso()
        val actor = sessionManager.currentUsername()
        userDao.insert(
            UserEntity(
                username = cleanUser,
                fullName = fullName.trim().ifBlank { cleanUser },
                passwordHash = PasswordHasher.hashPassword(password),
                role = role.name,
                isActive = true,
                createdAt = now,
                createdBy = actor,
                updatedAt = now,
                updatedBy = actor
            )
        )
        auditLogDao.insert(
            AuditLogEntity(
                timestamp = now,
                username = actor,
                action = "CREATE_USER",
                entityType = "USER",
                details = "Added user $cleanUser (${role.displayName})"
            )
        )
        return Result.success(Unit)
    }

    suspend fun toggleUserActive(user: UserEntity): Result<Unit> {
        if (user.username == sessionManager.currentUsername()) {
            return Result.failure(IllegalStateException("Cannot deactivate the currently active account"))
        }
        val now = DateTimeUtils.nowDateTimeIso()
        val actor = sessionManager.currentUsername()
        userDao.update(user.copy(isActive = !user.isActive, updatedAt = now, updatedBy = actor))
        auditLogDao.insert(
            AuditLogEntity(
                timestamp = now,
                username = actor,
                action = if (user.isActive) "DEACTIVATE_USER" else "ACTIVATE_USER",
                entityType = "USER",
                details = "Updated status for ${user.username}"
            )
        )
        return Result.success(Unit)
    }
}

class TestRepository(
    private val testDao: TestDao,
    private val crusherStatusDao: CrusherStatusDao,
    private val alertDao: AlertDao,
    private val auditLogDao: AuditLogDao,
    private val settingsDataStore: AppSettingsDataStore,
    private val sessionManager: SessionManager
) {
    val activeTests: Flow<List<CciTestEntity>> = testDao.observeActiveTests()
    val activeAlerts: Flow<List<AlertEntity>> = alertDao.observeActiveAlerts()

    suspend fun nextSampleId(dateIso: String): String {
        val count = testDao.getCountForDate(dateIso)
        return DateTimeUtils.generateSampleId(dateIso, count + 1)
    }

    suspend fun addTest(
        sampleId: String,
        testDate: String,
        shift: String,
        crusherLine: String,
        cokeType: String,
        cokeSource: String,
        operatorName: String,
        before050: Double,
        before100: Double,
        before300: Double,
        beforePlus3: Double,
        after050: Double,
        after100: Double,
        after300: Double,
        afterPlus3: Double,
        remarks: String
    ): Result<CciTestEntity> {
        val bTotal = before050 + before100 + before300 + beforePlus3
        val aTotal = after050 + after100 + after300 + afterPlus3
        if (bTotal <= 0.0 || aTotal <= 0.0) {
            return Result.failure(IllegalArgumentException("Before and After total weights must be greater than zero"))
        }

        val bf = ((before050 + before100 + before300) / bTotal) * 100.0
        val af = ((after050 + after100 + after300) / aTotal) * 100.0
        val cci = af
        val finesInc = af - bf

        val now = DateTimeUtils.nowDateTimeIso()
        val time = DateTimeUtils.nowTime()
        val dtStr = "${testDate}T${time}"
        val actor = sessionManager.currentUsername()

        val entity = CciTestEntity(
            sampleId = sampleId.trim().ifBlank { nextSampleId(testDate) },
            testDateTime = dtStr,
            testDate = testDate,
            shift = shift,
            crusherLine = crusherLine,
            cokeType = cokeType,
            cokeSource = cokeSource,
            operatorName = operatorName.trim().ifBlank { actor },
            beforeFrac050 = before050,
            beforeFrac100 = before100,
            beforeFrac300 = before300,
            beforeFracPlus3 = beforePlus3,
            beforeTotal = bTotal,
            afterFrac050 = after050,
            afterFrac100 = after100,
            afterFrac300 = after300,
            afterFracPlus3 = afterPlus3,
            afterTotal = aTotal,
            beforeFinesPct = bf,
            afterFinesPct = af,
            crushingIndex = cci,
            finesIncreasePp = finesInc,
            remarks = remarks.trim(),
            createdAt = now,
            createdBy = actor,
            updatedAt = now,
            updatedBy = actor
        )
        val id = testDao.insert(entity)

        val settings = settingsDataStore.settingsFlow.first()
        if (cci < settings.targetMinCci) {
            alertDao.insert(
                AlertEntity(
                    timestamp = now,
                    severity = if (cci < settings.targetMinCci - 5.0) "CRITICAL" else "WARNING",
                    title = "Low CCI on $crusherLine (${String.format(Locale.US, "%.2f", cci)}%)",
                    message = "Sample ${entity.sampleId} (Shift $shift) fell below target ${String.format(Locale.US, "%.1f", settings.targetMinCci)}%. Check roller gap & wear.",
                    crusherLine = crusherLine
                )
            )
        }

        auditLogDao.insert(
            AuditLogEntity(
                timestamp = now,
                username = actor,
                action = "ADD_TEST",
                entityType = "CCI_TEST",
                details = "${entity.sampleId} | $crusherLine S-$shift | CCI ${String.format(Locale.US, "%.2f", cci)}%"
            )
        )
        return Result.success(entity.copy(id = id))
    }

    suspend fun deleteTest(test: CciTestEntity) {
        val now = DateTimeUtils.nowDateTimeIso()
        val actor = sessionManager.currentUsername()
        testDao.softDelete(test.id, now, actor)
        auditLogDao.insert(
            AuditLogEntity(
                timestamp = now,
                username = actor,
                action = "DELETE_TEST",
                entityType = "CCI_TEST",
                details = "Soft-deleted sample ${test.sampleId}"
            )
        )
    }

    suspend fun acknowledgeAlert(alertId: Long) {
        alertDao.acknowledge(alertId)
    }
}

class CrusherRepository(
    private val database: CciDatabase,
    private val sessionManager: SessionManager
) {
    val crusherStatus: Flow<CrusherStatusEntity?> = database.crusherStatusDao().observeStatus()
    val changeovers: Flow<List<ChangeoverEntity>> = database.changeoverDao().observeAll()

    suspend fun executeChangeover(
        newRunningLine: String,
        newStandbyLine: String,
        reason: String,
        manualOverride: Boolean,
        remarks: String
    ): Result<Pair<CrusherStatusEntity, ChangeoverEntity>> {
        val now = DateTimeUtils.nowDateTimeIso()
        val actor = sessionManager.currentUsername()
        val current = database.crusherStatusDao().getStatus()
        val prevLine = current?.runningLine ?: newStandbyLine

        val statusEntity = CrusherStatusEntity(
            id = 1,
            runningLine = newRunningLine,
            standbyLine = newStandbyLine,
            lastChangeoverAt = now,
            updatedAt = now,
            updatedBy = actor
        )
        val changeoverEntity = ChangeoverEntity(
            eventDateTime = now,
            previousLine = prevLine,
            newLine = newRunningLine,
            newStandbyLine = newStandbyLine,
            reason = reason,
            manualOverride = manualOverride,
            authorizedBy = actor,
            remarks = remarks.trim(),
            createdAt = now,
            createdBy = actor,
            updatedAt = now,
            updatedBy = actor
        )
        var insertedChangeoverId = 0L

        database.withTransaction {
            database.crusherStatusDao().upsert(statusEntity)
            insertedChangeoverId = database.changeoverDao().insert(changeoverEntity)
            database.auditLogDao().insert(
                AuditLogEntity(
                    timestamp = now,
                    username = actor,
                    action = "CHANGEOVER",
                    entityType = "CRUSHER_LINE",
                    details = "$prevLine -> $newRunningLine ($reason)"
                )
            )
        }
        return Result.success(statusEntity to changeoverEntity.copy(id = insertedChangeoverId))
    }
}

class MaintenanceRepository(
    private val maintenanceDao: MaintenanceDao,
    private val auditLogDao: AuditLogDao,
    private val sessionManager: SessionManager
) {
    val maintenanceRecords: Flow<List<MaintenanceEntity>> = maintenanceDao.observeAll()

    suspend fun addMaintenance(
        crusherLine: String,
        activityDate: String,
        activityType: String,
        details: String,
        measurements: String,
        rollerCondition: String,
        nextDueDate: String
    ): Result<MaintenanceEntity> {
        if (details.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter maintenance work details"))
        }
        val now = DateTimeUtils.nowDateTimeIso()
        val actor = sessionManager.currentUsername()
        val entity = MaintenanceEntity(
            crusherLine = crusherLine,
            activityDate = activityDate,
            activityType = activityType,
            details = details.trim(),
            measurements = measurements.trim(),
            rollerCondition = rollerCondition,
            completedBy = actor,
            approvedBy = "",
            nextDueDate = nextDueDate.trim(),
            createdAt = now,
            createdBy = actor,
            updatedAt = now,
            updatedBy = actor
        )
        val insertedId = maintenanceDao.insert(entity)
        auditLogDao.insert(
            AuditLogEntity(
                timestamp = now,
                username = actor,
                action = "LOG_MAINTENANCE",
                entityType = "MAINTENANCE",
                details = "$crusherLine — $activityType"
            )
        )
        return Result.success(entity.copy(id = insertedId))
    }

    suspend fun approveMaintenance(record: MaintenanceEntity) {
        val now = DateTimeUtils.nowDateTimeIso()
        val actor = sessionManager.currentUsername()
        maintenanceDao.approve(record.id, actor, now, actor)
        auditLogDao.insert(
            AuditLogEntity(
                timestamp = now,
                username = actor,
                action = "APPROVE_MAINTENANCE",
                entityType = "MAINTENANCE",
                details = "Approved ${record.activityType} on ${record.crusherLine}"
            )
        )
    }
}

class SettingsRepository(
    private val database: CciDatabase,
    private val settingsDataStore: AppSettingsDataStore,
    private val sessionManager: SessionManager
) {
    val settings: Flow<PlantSettings> = settingsDataStore.settingsFlow
    val recentAuditLogs: Flow<List<AuditLogEntity>> = database.auditLogDao().observeRecent()

    suspend fun savePlantConfig(
        companyName: String,
        departmentName: String,
        targetMinCci: Double,
        maxFinesIncreasePp: Double,
        whatsappSignOff: String
    ) {
        settingsDataStore.updatePlantConfig(
            companyName = companyName,
            departmentName = departmentName,
            targetMinCci = targetMinCci,
            maxFinesIncreasePp = maxFinesIncreasePp,
            whatsappSignOff = whatsappSignOff
        )
        database.auditLogDao().insert(
            AuditLogEntity(
                timestamp = DateTimeUtils.nowDateTimeIso(),
                username = sessionManager.currentUsername(),
                action = "UPDATE_SETTINGS",
                entityType = "SETTINGS",
                details = "Target Min CCI=${String.format(Locale.US, "%.1f", targetMinCci)}%"
            )
        )
    }

    suspend fun setThemeMode(mode: String) {
        settingsDataStore.setThemeMode(mode)
    }
}

class DailyReportRepository(
    private val dailyReportDao: DailyReportDao,
    private val auditLogDao: AuditLogDao,
    private val settingsDataStore: AppSettingsDataStore,
    private val sessionManager: SessionManager
) {
    val dailyReports: Flow<List<DailyReport>> = dailyReportDao.observeAll()

    suspend fun saveDailyReport(
        reportDate: String,
        shiftScope: String,
        runningLine: String,
        standbyLine: String,
        rollerGapMm: Double,
        feedRateTph: Double,
        rollerPressureBar: Double,
        cokeMoisturePct: Double,
        tests: List<CciTestEntity>,
        changeoverCount: Int,
        maintenanceCount: Int,
        remarks: String,
        summaryText: String
    ): Result<DailyReport> {
        val settings = settingsDataStore.settingsFlow.first()
        val now = DateTimeUtils.nowDateTimeIso()
        val actor = sessionManager.currentUsername()

        val count = tests.size
        val avgB05 = if (count > 0) tests.map { it.beforeFrac050 }.average() else 0.0
        val avgB10 = if (count > 0) tests.map { it.beforeFrac100 }.average() else 0.0
        val avgB30 = if (count > 0) tests.map { it.beforeFrac300 }.average() else 0.0
        val avgBPlus = if (count > 0) tests.map { it.beforeFracPlus3 }.average() else 0.0
        val avgA05 = if (count > 0) tests.map { it.afterFrac050 }.average() else 0.0
        val avgA10 = if (count > 0) tests.map { it.afterFrac100 }.average() else 0.0
        val avgA30 = if (count > 0) tests.map { it.afterFrac300 }.average() else 0.0
        val avgAPlus = if (count > 0) tests.map { it.afterFracPlus3 }.average() else 0.0
        val avgBf = if (count > 0) tests.map { it.beforeFinesPct }.average() else 0.0
        val avgAf = if (count > 0) tests.map { it.afterFinesPct }.average() else 0.0
        val avgCci = if (count > 0) tests.map { it.crushingIndex }.average() else 0.0
        val minCci = if (count > 0) tests.minOf { it.crushingIndex } else 0.0
        val maxCci = if (count > 0) tests.maxOf { it.crushingIndex } else 0.0
        val avgGain = if (count > 0) tests.map { it.finesIncreasePp }.average() else 0.0
        val compliance = if (count > 0) {
            (tests.count { it.crushingIndex >= settings.targetMinCci }.toDouble() / count) * 100.0
        } else 0.0

        val entity = DailyReport(
            reportDate = reportDate,
            shiftScope = shiftScope,
            runningLine = runningLine,
            standbyLine = standbyLine,
            rollerGapMm = rollerGapMm,
            feedRateTph = feedRateTph,
            rollerPressureBar = rollerPressureBar,
            cokeMoisturePct = cokeMoisturePct,
            sampleCount = count,
            avgBeforeFrac050 = avgB05,
            avgBeforeFrac100 = avgB10,
            avgBeforeFrac300 = avgB30,
            avgBeforeFracPlus3 = avgBPlus,
            avgAfterFrac050 = avgA05,
            avgAfterFrac100 = avgA10,
            avgAfterFrac300 = avgA30,
            avgAfterFracPlus3 = avgAPlus,
            avgBeforeFinesPct = avgBf,
            avgAfterFinesPct = avgAf,
            avgCrushingIndex = avgCci,
            minCrushingIndex = minCci,
            maxCrushingIndex = maxCci,
            avgFinesIncreasePp = avgGain,
            targetCompliancePct = compliance,
            changeoverCount = changeoverCount,
            maintenanceCount = maintenanceCount,
            remarks = remarks.trim(),
            summaryText = summaryText,
            createdAt = now,
            createdBy = actor
        )

        val insertedId = dailyReportDao.insert(entity)
        auditLogDao.insert(
            AuditLogEntity(
                timestamp = now,
                username = actor,
                action = "SAVE_DAILY_REPORT",
                entityType = "DAILY_REPORT",
                details = "Saved daily report for $reportDate ($runningLine | Gap ${String.format(Locale.US, "%.1f", rollerGapMm)}mm | Avg CCI ${String.format(Locale.US, "%.2f", avgCci)}%)"
            )
        )
        return Result.success(entity.copy(id = insertedId))
    }

    suspend fun deleteDailyReport(report: DailyReport) {
        dailyReportDao.deleteById(report.id)
        auditLogDao.insert(
            AuditLogEntity(
                timestamp = DateTimeUtils.nowDateTimeIso(),
                username = sessionManager.currentUsername(),
                action = "DELETE_DAILY_REPORT",
                entityType = "DAILY_REPORT",
                details = "Deleted saved daily report for ${report.reportDate}"
            )
        )
    }
}

