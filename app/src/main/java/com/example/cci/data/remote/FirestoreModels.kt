package com.example.cci.data.remote

import androidx.annotation.Keep
import com.example.cci.data.local.entity.AlertEntity
import com.example.cci.data.local.entity.CciTestEntity
import com.example.cci.data.local.entity.ChangeoverEntity
import com.example.cci.data.local.entity.CrusherStatusEntity
import com.example.cci.data.local.entity.DailyReport
import com.example.cci.data.local.entity.MaintenanceEntity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfileDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val plantDepartment: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "displayName" to displayName.take(120),
        "email" to email.take(254),
        "plantDepartment" to plantDepartment.take(120),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "displayName" to displayName.take(120),
        "email" to email.take(254),
        "plantDepartment" to plantDepartment.take(120),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class CciTestDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val sampleId: String = "",
    val testDateTime: String = "",
    val testDate: String = "",
    val shift: String = "A",
    val crusherLine: String = "",
    val cokeType: String = "",
    val cokeSource: String = "",
    val operatorName: String = "",
    val beforeFrac050: Double = 0.0,
    val beforeFrac100: Double = 0.0,
    val beforeFrac300: Double = 0.0,
    val beforeFracPlus3: Double = 0.0,
    val beforeTotal: Double = 0.0,
    val afterFrac050: Double = 0.0,
    val afterFrac100: Double = 0.0,
    val afterFrac300: Double = 0.0,
    val afterFracPlus3: Double = 0.0,
    val afterTotal: Double = 0.0,
    val beforeFinesPct: Double = 0.0,
    val afterFinesPct: Double = 0.0,
    val crushingIndex: Double = 0.0,
    val finesIncreasePp: Double = 0.0,
    val remarks: String = "",
    val deleted: Boolean = false,
    val createdBy: String = "",
    val updatedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "sampleId" to sampleId.take(64).ifBlank { "SAMPLE" },
        "testDateTime" to testDateTime.take(40).ifBlank { testDate },
        "testDate" to testDate.take(20).ifBlank { "1970-01-01" },
        "shift" to if (shift in listOf("A", "B", "C")) shift else "A",
        "crusherLine" to crusherLine.take(32).ifBlank { "Line-01" },
        "cokeType" to cokeType.take(64).ifBlank { "Met Coke" },
        "cokeSource" to cokeSource.take(120),
        "operatorName" to operatorName.take(100).ifBlank { "Operator" },
        "beforeFrac050" to beforeFrac050.coerceIn(0.0, 100000.0),
        "beforeFrac100" to beforeFrac100.coerceIn(0.0, 100000.0),
        "beforeFrac300" to beforeFrac300.coerceIn(0.0, 100000.0),
        "beforeFracPlus3" to beforeFracPlus3.coerceIn(0.0, 100000.0),
        "beforeTotal" to beforeTotal.coerceIn(0.0, 400000.0),
        "afterFrac050" to afterFrac050.coerceIn(0.0, 100000.0),
        "afterFrac100" to afterFrac100.coerceIn(0.0, 100000.0),
        "afterFrac300" to afterFrac300.coerceIn(0.0, 100000.0),
        "afterFracPlus3" to afterFracPlus3.coerceIn(0.0, 100000.0),
        "afterTotal" to afterTotal.coerceIn(0.0, 400000.0),
        "beforeFinesPct" to beforeFinesPct.coerceIn(0.0, 100.0),
        "afterFinesPct" to afterFinesPct.coerceIn(0.0, 100.0),
        "crushingIndex" to crushingIndex.coerceIn(0.0, 100.0),
        "finesIncreasePp" to finesIncreasePp.coerceIn(-100.0, 100.0),
        "remarks" to remarks.take(500),
        "deleted" to deleted,
        "createdBy" to createdBy.take(100).ifBlank { "operator" },
        "updatedBy" to updatedBy.take(100).ifBlank { "operator" },
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    companion object {
        fun fromEntity(userId: String, entity: CciTestEntity): CciTestDoc = CciTestDoc(
            userId = userId,
            sampleId = entity.sampleId,
            testDateTime = entity.testDateTime,
            testDate = entity.testDate,
            shift = entity.shift,
            crusherLine = entity.crusherLine,
            cokeType = entity.cokeType,
            cokeSource = entity.cokeSource,
            operatorName = entity.operatorName,
            beforeFrac050 = entity.beforeFrac050,
            beforeFrac100 = entity.beforeFrac100,
            beforeFrac300 = entity.beforeFrac300,
            beforeFracPlus3 = entity.beforeFracPlus3,
            beforeTotal = entity.beforeTotal,
            afterFrac050 = entity.afterFrac050,
            afterFrac100 = entity.afterFrac100,
            afterFrac300 = entity.afterFrac300,
            afterFracPlus3 = entity.afterFracPlus3,
            afterTotal = entity.afterTotal,
            beforeFinesPct = entity.beforeFinesPct,
            afterFinesPct = entity.afterFinesPct,
            crushingIndex = entity.crushingIndex,
            finesIncreasePp = entity.finesIncreasePp,
            remarks = entity.remarks,
            deleted = entity.isDeleted,
            createdBy = entity.createdBy,
            updatedBy = entity.updatedBy
        )
    }
}

@Keep
@IgnoreExtraProperties
data class DailyReportDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val reportDate: String = "",
    val shiftScope: String = "ALL",
    val runningLine: String = "",
    val standbyLine: String = "",
    val rollerGapMm: Double = 0.0,
    val feedRateTph: Double = 0.0,
    val rollerPressureBar: Double = 0.0,
    val cokeMoisturePct: Double = 0.0,
    val sampleCount: Int = 0,
    val avgBeforeFrac050: Double = 0.0,
    val avgBeforeFrac100: Double = 0.0,
    val avgBeforeFrac300: Double = 0.0,
    val avgBeforeFracPlus3: Double = 0.0,
    val avgAfterFrac050: Double = 0.0,
    val avgAfterFrac100: Double = 0.0,
    val avgAfterFrac300: Double = 0.0,
    val avgAfterFracPlus3: Double = 0.0,
    val avgBeforeFinesPct: Double = 0.0,
    val avgAfterFinesPct: Double = 0.0,
    val avgCrushingIndex: Double = 0.0,
    val minCrushingIndex: Double = 0.0,
    val maxCrushingIndex: Double = 0.0,
    val avgFinesIncreasePp: Double = 0.0,
    val targetCompliancePct: Double = 0.0,
    val changeoverCount: Int = 0,
    val maintenanceCount: Int = 0,
    val remarks: String = "",
    val summaryText: String = "",
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "reportDate" to reportDate.take(20).ifBlank { "1970-01-01" },
        "shiftScope" to if (shiftScope in listOf("ALL", "A", "B", "C")) shiftScope else "ALL",
        "runningLine" to runningLine.take(32).ifBlank { "Line-01" },
        "standbyLine" to standbyLine.take(32).ifBlank { "Line-02" },
        "rollerGapMm" to rollerGapMm.coerceIn(0.0, 500.0),
        "feedRateTph" to feedRateTph.coerceIn(0.0, 10000.0),
        "rollerPressureBar" to rollerPressureBar.coerceIn(0.0, 1000.0),
        "cokeMoisturePct" to cokeMoisturePct.coerceIn(0.0, 100.0),
        "sampleCount" to sampleCount.coerceIn(0, 10000),
        "avgBeforeFrac050" to avgBeforeFrac050.coerceIn(0.0, 100000.0),
        "avgBeforeFrac100" to avgBeforeFrac100.coerceIn(0.0, 100000.0),
        "avgBeforeFrac300" to avgBeforeFrac300.coerceIn(0.0, 100000.0),
        "avgBeforeFracPlus3" to avgBeforeFracPlus3.coerceIn(0.0, 100000.0),
        "avgAfterFrac050" to avgAfterFrac050.coerceIn(0.0, 100000.0),
        "avgAfterFrac100" to avgAfterFrac100.coerceIn(0.0, 100000.0),
        "avgAfterFrac300" to avgAfterFrac300.coerceIn(0.0, 100000.0),
        "avgAfterFracPlus3" to avgAfterFracPlus3.coerceIn(0.0, 100000.0),
        "avgBeforeFinesPct" to avgBeforeFinesPct.coerceIn(0.0, 100.0),
        "avgAfterFinesPct" to avgAfterFinesPct.coerceIn(0.0, 100.0),
        "avgCrushingIndex" to avgCrushingIndex.coerceIn(0.0, 100.0),
        "minCrushingIndex" to minCrushingIndex.coerceIn(0.0, 100.0),
        "maxCrushingIndex" to maxCrushingIndex.coerceIn(0.0, 100.0),
        "avgFinesIncreasePp" to avgFinesIncreasePp.coerceIn(-100.0, 100.0),
        "targetCompliancePct" to targetCompliancePct.coerceIn(0.0, 100.0),
        "changeoverCount" to changeoverCount.coerceIn(0, 1000),
        "maintenanceCount" to maintenanceCount.coerceIn(0, 1000),
        "remarks" to remarks.take(1000),
        "summaryText" to summaryText.take(8000),
        "createdBy" to createdBy.take(100).ifBlank { "operator" },
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    companion object {
        fun fromEntity(userId: String, entity: DailyReport): DailyReportDoc = DailyReportDoc(
            userId = userId,
            reportDate = entity.reportDate,
            shiftScope = entity.shiftScope,
            runningLine = entity.runningLine,
            standbyLine = entity.standbyLine,
            rollerGapMm = entity.rollerGapMm,
            feedRateTph = entity.feedRateTph,
            rollerPressureBar = entity.rollerPressureBar,
            cokeMoisturePct = entity.cokeMoisturePct,
            sampleCount = entity.sampleCount,
            avgBeforeFrac050 = entity.avgBeforeFrac050,
            avgBeforeFrac100 = entity.avgBeforeFrac100,
            avgBeforeFrac300 = entity.avgBeforeFrac300,
            avgBeforeFracPlus3 = entity.avgBeforeFracPlus3,
            avgAfterFrac050 = entity.avgAfterFrac050,
            avgAfterFrac100 = entity.avgAfterFrac100,
            avgAfterFrac300 = entity.avgAfterFrac300,
            avgAfterFracPlus3 = entity.avgAfterFracPlus3,
            avgBeforeFinesPct = entity.avgBeforeFinesPct,
            avgAfterFinesPct = entity.avgAfterFinesPct,
            avgCrushingIndex = entity.avgCrushingIndex,
            minCrushingIndex = entity.minCrushingIndex,
            maxCrushingIndex = entity.maxCrushingIndex,
            avgFinesIncreasePp = entity.avgFinesIncreasePp,
            targetCompliancePct = entity.targetCompliancePct,
            changeoverCount = entity.changeoverCount,
            maintenanceCount = entity.maintenanceCount,
            remarks = entity.remarks,
            summaryText = entity.summaryText,
            createdBy = entity.createdBy
        )
    }
}

@Keep
@IgnoreExtraProperties
data class CrusherStatusDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val runningLine: String = "",
    val standbyLine: String = "",
    val lastChangeoverAt: String = "",
    val updatedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "runningLine" to runningLine.take(32).ifBlank { "Line-01" },
        "standbyLine" to standbyLine.take(32).ifBlank { "Line-02" },
        "lastChangeoverAt" to lastChangeoverAt.take(40).ifBlank { "1970-01-01T00:00:00" },
        "updatedBy" to updatedBy.take(100).ifBlank { "operator" },
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "runningLine" to runningLine.take(32).ifBlank { "Line-01" },
        "standbyLine" to standbyLine.take(32).ifBlank { "Line-02" },
        "lastChangeoverAt" to lastChangeoverAt.take(40).ifBlank { "1970-01-01T00:00:00" },
        "updatedBy" to updatedBy.take(100).ifBlank { "operator" },
        "updatedAt" to FieldValue.serverTimestamp()
    )

    companion object {
        fun fromEntity(userId: String, entity: CrusherStatusEntity): CrusherStatusDoc = CrusherStatusDoc(
            userId = userId,
            runningLine = entity.runningLine,
            standbyLine = entity.standbyLine,
            lastChangeoverAt = entity.lastChangeoverAt,
            updatedBy = entity.updatedBy
        )
    }
}

@Keep
@IgnoreExtraProperties
data class ChangeoverDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val eventDateTime: String = "",
    val previousLine: String = "",
    val newLine: String = "",
    val newStandbyLine: String = "",
    val reason: String = "",
    val manualOverride: Boolean = false,
    val authorizedBy: String = "",
    val remarks: String = "",
    val createdBy: String = "",
    val updatedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "eventDateTime" to eventDateTime.take(40).ifBlank { "1970-01-01T00:00:00" },
        "previousLine" to previousLine.take(32).ifBlank { "Line-01" },
        "newLine" to newLine.take(32).ifBlank { "Line-02" },
        "newStandbyLine" to newStandbyLine.take(32).ifBlank { "Line-01" },
        "reason" to reason.take(200).ifBlank { "Scheduled Changeover" },
        "manualOverride" to manualOverride,
        "authorizedBy" to authorizedBy.take(100).ifBlank { "operator" },
        "remarks" to remarks.take(500),
        "createdBy" to createdBy.take(100).ifBlank { "operator" },
        "updatedBy" to updatedBy.take(100).ifBlank { "operator" },
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    companion object {
        fun fromEntity(userId: String, entity: ChangeoverEntity): ChangeoverDoc = ChangeoverDoc(
            userId = userId,
            eventDateTime = entity.eventDateTime,
            previousLine = entity.previousLine,
            newLine = entity.newLine,
            newStandbyLine = entity.newStandbyLine,
            reason = entity.reason,
            manualOverride = entity.manualOverride,
            authorizedBy = entity.authorizedBy,
            remarks = entity.remarks,
            createdBy = entity.createdBy,
            updatedBy = entity.updatedBy
        )
    }
}

@Keep
@IgnoreExtraProperties
data class MaintenanceDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val crusherLine: String = "",
    val activityDate: String = "",
    val activityType: String = "",
    val details: String = "",
    val measurements: String = "",
    val rollerCondition: String = "",
    val completedBy: String = "",
    val approvedBy: String = "",
    val nextDueDate: String = "",
    val createdBy: String = "",
    val updatedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "crusherLine" to crusherLine.take(32).ifBlank { "Line-01" },
        "activityDate" to activityDate.take(20).ifBlank { "1970-01-01" },
        "activityType" to activityType.take(100).ifBlank { "Inspection" },
        "details" to details.take(1000).ifBlank { "Maintenance work" },
        "measurements" to measurements.take(300),
        "rollerCondition" to rollerCondition.take(100),
        "completedBy" to completedBy.take(100).ifBlank { "operator" },
        "approvedBy" to approvedBy.take(100),
        "nextDueDate" to nextDueDate.take(20),
        "createdBy" to createdBy.take(100).ifBlank { "operator" },
        "updatedBy" to updatedBy.take(100).ifBlank { "operator" },
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    companion object {
        fun fromEntity(userId: String, entity: MaintenanceEntity): MaintenanceDoc = MaintenanceDoc(
            userId = userId,
            crusherLine = entity.crusherLine,
            activityDate = entity.activityDate,
            activityType = entity.activityType,
            details = entity.details,
            measurements = entity.measurements,
            rollerCondition = entity.rollerCondition,
            completedBy = entity.completedBy,
            approvedBy = entity.approvedBy,
            nextDueDate = entity.nextDueDate,
            createdBy = entity.createdBy,
            updatedBy = entity.updatedBy
        )
    }
}

@Keep
@IgnoreExtraProperties
data class AlertDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val timestamp: String = "",
    val severity: String = "WARNING",
    val title: String = "",
    val message: String = "",
    val crusherLine: String = "",
    val acknowledged: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "timestamp" to timestamp.take(40).ifBlank { "1970-01-01T00:00:00" },
        "severity" to if (severity in listOf("CRITICAL", "WARNING", "INFO")) severity else "WARNING",
        "title" to title.take(200).ifBlank { "Plant Alert" },
        "message" to message.take(1000).ifBlank { "Threshold alert triggered" },
        "crusherLine" to crusherLine.take(32).ifBlank { "Line-01" },
        "acknowledged" to acknowledged,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    companion object {
        fun fromEntity(userId: String, entity: AlertEntity): AlertDoc = AlertDoc(
            userId = userId,
            timestamp = entity.timestamp,
            severity = entity.severity,
            title = entity.title,
            message = entity.message,
            crusherLine = entity.crusherLine,
            acknowledged = entity.isAcknowledged
        )
    }
}
