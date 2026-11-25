package com.example.cci.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.cci.data.local.entity.AlertEntity
import com.example.cci.data.local.entity.AuditLogEntity
import com.example.cci.data.local.entity.CciTestEntity
import com.example.cci.data.local.entity.ChangeoverEntity
import com.example.cci.data.local.entity.CrusherStatusEntity
import com.example.cci.data.local.entity.DailyReport
import com.example.cci.data.local.entity.MaintenanceEntity
import com.example.cci.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY id ASC")
    fun observeAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun findByUsername(username: String): UserEntity?

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity): Long

    @Update
    suspend fun update(user: UserEntity)
}

@Dao
interface TestDao {
    @Query("SELECT * FROM cci_tests WHERE isDeleted = 0 ORDER BY testDateTime DESC, id DESC")
    fun observeActiveTests(): Flow<List<CciTestEntity>>

    @Query("SELECT * FROM cci_tests WHERE isDeleted = 0 AND testDate BETWEEN :startDate AND :endDate ORDER BY testDateTime DESC, id DESC")
    suspend fun getTestsInDateRange(startDate: String, endDate: String): List<CciTestEntity>

    @Query("SELECT COUNT(*) FROM cci_tests WHERE isDeleted = 0")
    suspend fun getActiveCount(): Int

    @Query("SELECT COUNT(*) FROM cci_tests WHERE isDeleted = 0 AND testDate = :date")
    suspend fun getCountForDate(date: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(test: CciTestEntity): Long

    @Query("UPDATE cci_tests SET isDeleted = 1, updatedAt = :updatedAt, updatedBy = :updatedBy WHERE id = :id")
    suspend fun softDelete(id: Long, updatedAt: String, updatedBy: String)
}

@Dao
interface CrusherStatusDao {
    @Query("SELECT * FROM crusher_status WHERE id = 1 LIMIT 1")
    fun observeStatus(): Flow<CrusherStatusEntity?>

    @Query("SELECT * FROM crusher_status WHERE id = 1 LIMIT 1")
    suspend fun getStatus(): CrusherStatusEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(status: CrusherStatusEntity)
}

@Dao
interface ChangeoverDao {
    @Query("SELECT * FROM changeovers ORDER BY eventDateTime DESC, id DESC")
    fun observeAll(): Flow<List<ChangeoverEntity>>

    @Query("SELECT * FROM changeovers ORDER BY eventDateTime DESC, id DESC")
    suspend fun getAll(): List<ChangeoverEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(changeover: ChangeoverEntity): Long
}

@Dao
interface MaintenanceDao {
    @Query("SELECT * FROM maintenance ORDER BY activityDate DESC, id DESC")
    fun observeAll(): Flow<List<MaintenanceEntity>>

    @Query("SELECT * FROM maintenance ORDER BY activityDate DESC, id DESC")
    suspend fun getAll(): List<MaintenanceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(maintenance: MaintenanceEntity): Long

    @Query("UPDATE maintenance SET approvedBy = :approvedBy, updatedAt = :updatedAt, updatedBy = :updatedBy WHERE id = :id")
    suspend fun approve(id: Long, approvedBy: String, updatedAt: String, updatedBy: String)
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY id DESC LIMIT 100")
    fun observeRecent(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: AuditLogEntity): Long
}

@Dao
interface AlertDao {
    @Query("SELECT * FROM alerts WHERE isAcknowledged = 0 ORDER BY id DESC")
    fun observeActiveAlerts(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts ORDER BY id DESC LIMIT 50")
    fun observeAllAlerts(): Flow<List<AlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alert: AlertEntity): Long

    @Query("UPDATE alerts SET isAcknowledged = 1 WHERE id = :id")
    suspend fun acknowledge(id: Long)
}

@Dao
interface DailyReportDao {
    @Query("SELECT * FROM daily_reports ORDER BY reportDate DESC, id DESC")
    fun observeAll(): Flow<List<DailyReport>>

    @Query("SELECT * FROM daily_reports WHERE reportDate = :date ORDER BY id DESC")
    suspend fun getByDate(date: String): List<DailyReport>

    @Query("SELECT COUNT(*) FROM daily_reports")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(report: DailyReport): Long

    @Query("DELETE FROM daily_reports WHERE id = :id")
    suspend fun deleteById(id: Long)
}

