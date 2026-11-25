package com.example.cci.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cci.AppContainer
import com.example.cci.core.constants.AppConstants
import com.example.cci.core.constants.UserRole
import com.example.cci.core.security.ActiveSession
import com.example.cci.core.utils.DateTimeUtils
import com.example.cci.core.utils.ExportFormat
import com.example.cci.core.utils.ReportExporter
import com.example.cci.core.utils.ReportType
import com.example.cci.data.datastore.PlantSettings
import com.example.cci.data.local.entity.AlertEntity
import com.example.cci.data.local.entity.AuditLogEntity
import com.example.cci.data.local.entity.CciTestEntity
import com.example.cci.data.local.entity.ChangeoverEntity
import com.example.cci.data.local.entity.CrusherStatusEntity
import com.example.cci.data.local.entity.DailyReport
import com.example.cci.data.local.entity.MaintenanceEntity
import com.example.cci.data.local.entity.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class MainDestination(val route: String, val label: String) {
    DASHBOARD("dashboard", "Dashboard"),
    TESTS("tests", "CCI Tests"),
    OPERATIONS("operations", "Operations"),
    REPORTS("reports", "Reports"),
    SETTINGS("settings", "Settings")
}

data class CciUiState(
    val tests: List<CciTestEntity> = emptyList(),
    val crusherStatus: CrusherStatusEntity? = null,
    val changeovers: List<ChangeoverEntity> = emptyList(),
    val maintenance: List<MaintenanceEntity> = emptyList(),
    val activeAlerts: List<AlertEntity> = emptyList(),
    val dailyReports: List<DailyReport> = emptyList(),
    val settings: PlantSettings = PlantSettings(),
    val session: ActiveSession = ActiveSession(0, "shiftincharge", "Sinter Shift In-Charge", UserRole.SHIFT_IN_CHARGE),
    val users: List<UserEntity> = emptyList(),
    val auditLogs: List<AuditLogEntity> = emptyList()
) {
    val todayDate: String get() = DateTimeUtils.todayIso()
    val todayTests: List<CciTestEntity> get() = tests.filter { it.testDate == todayDate }
    val todayAvgCci: Double? get() = todayTests.takeIf { it.isNotEmpty() }?.map { it.crushingIndex }?.average()
    val overallAvgCci: Double? get() = tests.takeIf { it.isNotEmpty() }?.map { it.crushingIndex }?.average()
    val todayAvgFinesGain: Double? get() = todayTests.takeIf { it.isNotEmpty() }?.map { it.finesIncreasePp }?.average()
    val runningLine: String get() = crusherStatus?.runningLine ?: AppConstants.LINE_01
    val standbyLine: String get() = crusherStatus?.standbyLine ?: AppConstants.LINE_02
}

class CciViewModel(
    application: Application,
    private val container: AppContainer
) : AndroidViewModel(application) {

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    private val _suggestedSampleId = MutableStateFlow(DateTimeUtils.generateSampleId(DateTimeUtils.todayIso(), 1))
    val suggestedSampleId: StateFlow<String> = _suggestedSampleId

    private val coreDataFlow = combine(
        container.testRepository.activeTests,
        container.crusherRepository.crusherStatus,
        container.crusherRepository.changeovers,
        container.maintenanceRepository.maintenanceRecords,
        container.testRepository.activeAlerts
    ) { tests, status, changeovers, maint, alerts ->
        CoreBundle(tests, status, changeovers, maint, alerts)
    }

    private val metaDataFlow = combine(
        container.settingsRepository.settings,
        container.sessionManager.currentSession,
        container.authRepository.allUsers,
        container.settingsRepository.recentAuditLogs,
        container.dailyReportRepository.dailyReports
    ) { settings, session, users, logs, dailyReports ->
        MetaBundle(settings, session, users, logs, dailyReports)
    }

    val uiState: StateFlow<CciUiState> = combine(coreDataFlow, metaDataFlow) { core, meta ->
        CciUiState(
            tests = core.tests,
            crusherStatus = core.status,
            changeovers = core.changeovers,
            maintenance = core.maintenance,
            activeAlerts = core.alerts,
            dailyReports = meta.dailyReports,
            settings = meta.settings,
            session = meta.session,
            users = meta.users,
            auditLogs = meta.auditLogs
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CciUiState()
    )

    fun refreshNextSampleId(dateIso: String = DateTimeUtils.todayIso()) {
        viewModelScope.launch {
            _suggestedSampleId.value = container.testRepository.nextSampleId(dateIso)
        }
    }

    fun addCciTest(
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
        remarks: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val result = container.testRepository.addTest(
                sampleId = sampleId,
                testDate = testDate,
                shift = shift,
                crusherLine = crusherLine,
                cokeType = cokeType,
                cokeSource = cokeSource,
                operatorName = operatorName,
                before050 = before050,
                before100 = before100,
                before300 = before300,
                beforePlus3 = beforePlus3,
                after050 = after050,
                after100 = after100,
                after300 = after300,
                afterPlus3 = afterPlus3,
                remarks = remarks
            )
            result.onSuccess {
                _snackbarEvents.tryEmit("Saved ${it.sampleId} (CCI ${"%.2f".format(it.crushingIndex)}%)")
                refreshNextSampleId(testDate)
                onSuccess()
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.message ?: "Unable to save test")
            }
        }
    }

    fun deleteTest(test: CciTestEntity) {
        viewModelScope.launch {
            container.testRepository.deleteTest(test)
            _snackbarEvents.tryEmit("Deleted sample ${test.sampleId}")
        }
    }

    fun acknowledgeAlert(alertId: Long) {
        viewModelScope.launch {
            container.testRepository.acknowledgeAlert(alertId)
            _snackbarEvents.tryEmit("Alert acknowledged")
        }
    }

    fun executeChangeover(
        newRunningLine: String,
        newStandbyLine: String,
        reason: String,
        manualOverride: Boolean,
        remarks: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            container.crusherRepository.executeChangeover(
                newRunningLine = newRunningLine,
                newStandbyLine = newStandbyLine,
                reason = reason,
                manualOverride = manualOverride,
                remarks = remarks
            ).onSuccess {
                _snackbarEvents.tryEmit("Switched running line to $newRunningLine")
                onSuccess()
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.message ?: "Changeover failed")
            }
        }
    }

    fun addMaintenance(
        crusherLine: String,
        activityDate: String,
        activityType: String,
        details: String,
        measurements: String,
        rollerCondition: String,
        nextDueDate: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            container.maintenanceRepository.addMaintenance(
                crusherLine = crusherLine,
                activityDate = activityDate,
                activityType = activityType,
                details = details,
                measurements = measurements,
                rollerCondition = rollerCondition,
                nextDueDate = nextDueDate
            ).onSuccess {
                _snackbarEvents.tryEmit("Logged $activityType for $crusherLine")
                onSuccess()
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.message ?: "Unable to log maintenance")
            }
        }
    }

    fun approveMaintenance(record: MaintenanceEntity) {
        viewModelScope.launch {
            container.maintenanceRepository.approveMaintenance(record)
            _snackbarEvents.tryEmit("Approved ${record.activityType} on ${record.crusherLine}")
        }
    }

    fun login(username: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            container.authRepository.login(username, password)
                .onSuccess { session ->
                    _snackbarEvents.tryEmit("Signed in as ${session.fullName} (${session.role.displayName})")
                    onSuccess()
                }
                .onFailure { err ->
                    onError(err.message ?: "Sign-in failed")
                }
        }
    }

    fun createUser(username: String, fullName: String, password: String, role: UserRole, onSuccess: () -> Unit) {
        viewModelScope.launch {
            container.authRepository.createUser(username, fullName, password, role)
                .onSuccess {
                    _snackbarEvents.tryEmit("Created user $username")
                    onSuccess()
                }
                .onFailure { err ->
                    _snackbarEvents.tryEmit(err.message ?: "Could not create user")
                }
        }
    }

    fun toggleUserActive(user: UserEntity) {
        viewModelScope.launch {
            container.authRepository.toggleUserActive(user)
                .onSuccess { _snackbarEvents.tryEmit("Updated status for ${user.username}") }
                .onFailure { err -> _snackbarEvents.tryEmit(err.message ?: "Action denied") }
        }
    }

    fun savePlantConfig(
        companyName: String,
        departmentName: String,
        targetMinCci: Double,
        maxFinesIncreasePp: Double,
        whatsappSignOff: String
    ) {
        viewModelScope.launch {
            container.settingsRepository.savePlantConfig(
                companyName = companyName,
                departmentName = departmentName,
                targetMinCci = targetMinCci,
                maxFinesIncreasePp = maxFinesIncreasePp,
                whatsappSignOff = whatsappSignOff
            )
            _snackbarEvents.tryEmit("Plant settings saved")
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            container.settingsRepository.setThemeMode(mode)
        }
    }

    fun shareTodayQuickWhatsApp(context: Context) {
        val state = uiState.value
        val today = DateTimeUtils.todayIso()
        val todayTests = state.tests.filter { it.testDate == today }
        val todayChangeovers = state.changeovers.filter { it.eventDateTime.startsWith(today) }
        val todayMaintenance = state.maintenance.filter { it.activityDate == today }

        val direct = ReportExporter.shareDailyReportSummaryViaWhatsApp(
            context = context,
            settings = state.settings,
            reportDateLabel = DateTimeUtils.formatDisplayDate(today),
            shiftFilter = "ALL",
            tests = todayTests,
            crusherStatus = state.crusherStatus,
            changeovers = todayChangeovers,
            maintenance = todayMaintenance
        )
        _snackbarEvents.tryEmit(
            if (direct) "Opening WhatsApp with Today's Daily Summary"
            else "WhatsApp not found — opened share sheet"
        )
    }

    fun shareTextReportViaWhatsApp(context: Context, reportText: String) {
        val direct = ReportExporter.shareTextToWhatsApp(context, reportText)
        _snackbarEvents.tryEmit(
            if (direct) "Opening WhatsApp with Daily Report"
            else "WhatsApp not found — opened share sheet"
        )
    }

    fun shareSingleTestViaWhatsApp(context: Context, test: CciTestEntity) {
        val msg = ReportExporter.buildSingleTestWhatsAppMessage(uiState.value.settings, test)
        val direct = ReportExporter.shareTextToWhatsApp(context, msg)
        _snackbarEvents.tryEmit(
            if (direct) "Sharing ${test.sampleId} on WhatsApp"
            else "Opened share sheet for ${test.sampleId}"
        )
    }

    fun copyReportText(context: Context, text: String) {
        ReportExporter.copyToClipboard(context, "Evonith Daily CCI Report", text)
        _snackbarEvents.tryEmit("Daily report copied — ready to paste in WhatsApp")
    }

    fun exportAndShareReport(
        context: Context,
        reportType: ReportType,
        format: ExportFormat,
        startDate: String,
        endDate: String,
        filteredTests: List<CciTestEntity>,
        filteredChangeovers: List<ChangeoverEntity>,
        filteredMaintenance: List<MaintenanceEntity>,
        viaWhatsApp: Boolean,
        whatsappCaption: String? = null
    ) {
        viewModelScope.launch {
            try {
                val state = uiState.value
                val file = withContext(Dispatchers.IO) {
                    ReportExporter.generateReportFile(
                        context = context,
                        settings = state.settings,
                        reportType = reportType,
                        format = format,
                        startDate = startDate,
                        endDate = endDate,
                        tests = filteredTests,
                        crusherStatus = state.crusherStatus,
                        changeovers = filteredChangeovers,
                        maintenance = filteredMaintenance
                    )
                }
                if (viaWhatsApp) {
                    val direct = ReportExporter.shareFileToWhatsApp(
                        context = context,
                        file = file,
                        mimeType = format.mime,
                        caption = whatsappCaption
                    )
                    _snackbarEvents.tryEmit(
                        if (direct) "Sharing ${file.name} on WhatsApp"
                        else "Opened share sheet for ${file.name}"
                    )
                } else {
                    ReportExporter.openOrShareFile(context, file, format.mime)
                    _snackbarEvents.tryEmit("Generated ${file.name}")
                }
            } catch (e: Exception) {
                _snackbarEvents.tryEmit("Export failed: ${e.message ?: "Unknown error"}")
            }
        }
    }

    fun exportJsonBackup(context: Context) {
        viewModelScope.launch {
            try {
                val state = uiState.value
                val file = withContext(Dispatchers.IO) {
                    ReportExporter.exportJsonBackup(
                        context = context,
                        settings = state.settings,
                        tests = state.tests,
                        changeovers = state.changeovers,
                        maintenance = state.maintenance
                    )
                }
                ReportExporter.openOrShareFile(context, file, "application/json")
                _snackbarEvents.tryEmit("Backup created: ${file.name}")
            } catch (e: Exception) {
                _snackbarEvents.tryEmit("Backup failed: ${e.message ?: "Unknown error"}")
            }
        }
    }

    fun saveDailyReport(
        reportDate: String,
        shiftScope: String,
        rollerGapMm: Double,
        feedRateTph: Double,
        rollerPressureBar: Double,
        cokeMoisturePct: Double,
        tests: List<CciTestEntity>,
        changeoverCount: Int,
        maintenanceCount: Int,
        remarks: String,
        summaryText: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val state = uiState.value
            container.dailyReportRepository.saveDailyReport(
                reportDate = reportDate,
                shiftScope = shiftScope,
                runningLine = state.runningLine,
                standbyLine = state.standbyLine,
                rollerGapMm = rollerGapMm,
                feedRateTph = feedRateTph,
                rollerPressureBar = rollerPressureBar,
                cokeMoisturePct = cokeMoisturePct,
                tests = tests,
                changeoverCount = changeoverCount,
                maintenanceCount = maintenanceCount,
                remarks = remarks,
                summaryText = summaryText
            ).onSuccess { saved ->
                _snackbarEvents.tryEmit("Saved Daily Report for ${DateTimeUtils.formatDisplayDate(saved.reportDate)}")
                onSuccess()
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.message ?: "Unable to save daily report")
            }
        }
    }

    fun deleteDailyReport(report: DailyReport) {
        viewModelScope.launch {
            container.dailyReportRepository.deleteDailyReport(report)
            _snackbarEvents.tryEmit("Deleted Daily Report (${DateTimeUtils.formatDisplayDate(report.reportDate)})")
        }
    }

    fun shareSavedDailyReportViaWhatsApp(context: Context, report: DailyReport) {
        val text = ReportExporter.formatSavedDailyReportForWhatsApp(uiState.value.settings, report)
        val direct = ReportExporter.shareTextToWhatsApp(context, text)
        _snackbarEvents.tryEmit(
            if (direct) "Opening WhatsApp with Daily Report (${DateTimeUtils.formatDisplayDate(report.reportDate)})"
            else "WhatsApp not found — opened share sheet"
        )
    }

    private data class CoreBundle(
        val tests: List<CciTestEntity>,
        val status: CrusherStatusEntity?,
        val changeovers: List<ChangeoverEntity>,
        val maintenance: List<MaintenanceEntity>,
        val alerts: List<AlertEntity>
    )

    private data class MetaBundle(
        val settings: PlantSettings,
        val session: ActiveSession,
        val users: List<UserEntity>,
        val auditLogs: List<AuditLogEntity>,
        val dailyReports: List<DailyReport>
    )

    companion object {
        fun factory(application: Application, container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CciViewModel(application, container) as T
                }
            }
    }
}
