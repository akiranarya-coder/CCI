package com.example.cci.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["username"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val username: String,
    val fullName: String,
    val passwordHash: String,
    val role: String,
    val isActive: Boolean = true,
    val createdAt: String,
    val createdBy: String,
    val updatedAt: String,
    val updatedBy: String
)

@Entity(
    tableName = "cci_tests",
    indices = [Index(value = ["testDate"]), Index(value = ["crusherLine"])]
)
data class CciTestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sampleId: String,
    val testDateTime: String,
    val testDate: String,
    val shift: String,
    val crusherLine: String,
    val cokeType: String,
    val cokeSource: String,
    val operatorName: String,
    val beforeFrac050: Double,
    val beforeFrac100: Double,
    val beforeFrac300: Double,
    val beforeFracPlus3: Double,
    val beforeTotal: Double,
    val afterFrac050: Double,
    val afterFrac100: Double,
    val afterFrac300: Double,
    val afterFracPlus3: Double,
    val afterTotal: Double,
    val beforeFinesPct: Double,
    val afterFinesPct: Double,
    val crushingIndex: Double,
    val finesIncreasePp: Double,
    val remarks: String = "",
    val isDeleted: Boolean = false,
    val createdAt: String,
    val createdBy: String,
    val updatedAt: String,
    val updatedBy: String
)

@Entity(tableName = "crusher_status")
data class CrusherStatusEntity(
    @PrimaryKey val id: Int = 1,
    val runningLine: String,
    val standbyLine: String,
    val lastChangeoverAt: String,
    val updatedAt: String,
    val updatedBy: String
)

@Entity(tableName = "changeovers")
data class ChangeoverEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val eventDateTime: String,
    val previousLine: String,
    val newLine: String,
    val newStandbyLine: String,
    val reason: String,
    val manualOverride: Boolean = false,
    val authorizedBy: String,
    val remarks: String = "",
    val createdAt: String,
    val createdBy: String,
    val updatedAt: String,
    val updatedBy: String
)

@Entity(tableName = "maintenance")
data class MaintenanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val crusherLine: String,
    val activityDate: String,
    val activityType: String,
    val details: String,
    val measurements: String = "",
    val rollerCondition: String = "",
    val completedBy: String,
    val approvedBy: String = "",
    val nextDueDate: String = "",
    val createdAt: String,
    val createdBy: String,
    val updatedAt: String,
    val updatedBy: String
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val timestamp: String,
    val username: String,
    val action: String,
    val entityType: String,
    val details: String
)

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val timestamp: String,
    val severity: String, // "CRITICAL", "WARNING", "INFO"
    val title: String,
    val message: String,
    val crusherLine: String,
    val isAcknowledged: Boolean = false
)

@Entity(
    tableName = "daily_reports",
    indices = [Index(value = ["reportDate"])]
)
data class DailyReport(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val reportDate: String,
    val shiftScope: String,
    // Crusher operating parameters
    val runningLine: String,
    val standbyLine: String,
    val rollerGapMm: Double,
    val feedRateTph: Double,
    val rollerPressureBar: Double,
    val cokeMoisturePct: Double,
    // Aggregated sieve analysis results
    val sampleCount: Int,
    val avgBeforeFrac050: Double,
    val avgBeforeFrac100: Double,
    val avgBeforeFrac300: Double,
    val avgBeforeFracPlus3: Double,
    val avgAfterFrac050: Double,
    val avgAfterFrac100: Double,
    val avgAfterFrac300: Double,
    val avgAfterFracPlus3: Double,
    val avgBeforeFinesPct: Double,
    val avgAfterFinesPct: Double,
    val avgCrushingIndex: Double,
    val minCrushingIndex: Double,
    val maxCrushingIndex: Double,
    val avgFinesIncreasePp: Double,
    val targetCompliancePct: Double,
    // Plant events & persisted summary
    val changeoverCount: Int,
    val maintenanceCount: Int,
    val remarks: String = "",
    val summaryText: String = "",
    val createdAt: String,
    val createdBy: String
)

