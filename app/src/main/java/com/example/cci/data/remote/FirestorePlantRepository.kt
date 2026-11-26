package com.example.cci.data.remote

import com.example.cci.data.local.entity.AlertEntity
import com.example.cci.data.local.entity.CciTestEntity
import com.example.cci.data.local.entity.ChangeoverEntity
import com.example.cci.data.local.entity.CrusherStatusEntity
import com.example.cci.data.local.entity.DailyReport
import com.example.cci.data.local.entity.MaintenanceEntity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

data class CloudSyncSummary(
    val isConnected: Boolean = false,
    val isSyncing: Boolean = false,
    val syncedTestCount: Int = 0,
    val syncedReportCount: Int = 0,
    val syncedChangeoverCount: Int = 0,
    val syncedMaintenanceCount: Int = 0,
    val lastError: String? = null
)

class FirestorePlantRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing Firestore.")

    private fun sanitizeDocId(raw: String, prefix: String): String {
        val cleaned = raw.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(100)
        return "${prefix}_${cleaned.ifBlank { "doc" }}".take(128)
    }

    fun testDocId(entity: CciTestEntity): String =
        sanitizeDocId("${entity.id}_${entity.sampleId}", "test")

    fun reportDocId(entity: DailyReport): String =
        sanitizeDocId("${entity.id}_${entity.reportDate}", "report")

    fun changeoverDocId(entity: ChangeoverEntity): String =
        sanitizeDocId("${entity.id}_${entity.eventDateTime}", "chg")

    fun maintenanceDocId(entity: MaintenanceEntity): String =
        sanitizeDocId("${entity.id}_${entity.activityDate}", "mnt")

    fun alertDocId(alertId: Long): String =
        sanitizeDocId(alertId.toString(), "alert")

    // -------------------------------------------------------------------------
    // Real-time Snapshot Listeners (ESTIMATE server timestamps + 2-tier catch)
    // -------------------------------------------------------------------------

    fun observeTests(userId: String): Flow<List<CciTestDoc>> {
        val path = "users/$userId/cci_tests"
        return firestore.collection("users").document(userId).collection("cci_tests")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(CciTestDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.filter { !it.deleted }
            }
            .catch { cause ->
                val root = if (cause is CancellationException) cause.cause else cause
                if (root is FirebaseFirestoreException) {
                    throw handleFirestoreError(root, OperationType.LIST, path)
                }
                if (cause is CancellationException) throw cause
                throw handleFirestoreError(cause, OperationType.LIST, path)
            }
    }

    fun observeDailyReports(userId: String): Flow<List<DailyReportDoc>> {
        val path = "users/$userId/daily_reports"
        return firestore.collection("users").document(userId).collection("daily_reports")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(DailyReportDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
            }
            .catch { cause ->
                val root = if (cause is CancellationException) cause.cause else cause
                if (root is FirebaseFirestoreException) {
                    throw handleFirestoreError(root, OperationType.LIST, path)
                }
                if (cause is CancellationException) throw cause
                throw handleFirestoreError(cause, OperationType.LIST, path)
            }
    }

    fun observeChangeovers(userId: String): Flow<List<ChangeoverDoc>> {
        val path = "users/$userId/changeovers"
        return firestore.collection("users").document(userId).collection("changeovers")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ChangeoverDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
            }
            .catch { cause ->
                val root = if (cause is CancellationException) cause.cause else cause
                if (root is FirebaseFirestoreException) {
                    throw handleFirestoreError(root, OperationType.LIST, path)
                }
                if (cause is CancellationException) throw cause
                throw handleFirestoreError(cause, OperationType.LIST, path)
            }
    }

    fun observeMaintenance(userId: String): Flow<List<MaintenanceDoc>> {
        val path = "users/$userId/maintenance"
        return firestore.collection("users").document(userId).collection("maintenance")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(MaintenanceDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
            }
            .catch { cause ->
                val root = if (cause is CancellationException) cause.cause else cause
                if (root is FirebaseFirestoreException) {
                    throw handleFirestoreError(root, OperationType.LIST, path)
                }
                if (cause is CancellationException) throw cause
                throw handleFirestoreError(cause, OperationType.LIST, path)
            }
    }

    // -------------------------------------------------------------------------
    // Authenticated Cloud Mutations (Result<T>, explicit create vs update)
    // -------------------------------------------------------------------------

    suspend fun ensureUserProfile(
        displayName: String,
        email: String,
        plantDepartment: String
    ): Result<Unit> {
        val userId = requireUserId()
        val path = "users/$userId"
        return try {
            val docRef = firestore.collection("users").document(userId)
            val profile = UserProfileDoc(
                userId = userId,
                displayName = displayName,
                email = email,
                plantDepartment = plantDepartment
            )
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(profile.toUpdateMap()).await()
            } else {
                docRef.set(profile.toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(handleFirestoreError(e, OperationType.WRITE, path))
        }
    }

    suspend fun saveTest(entity: CciTestEntity): Result<Unit> {
        val userId = requireUserId()
        val docId = testDocId(entity)
        val path = "users/$userId/cci_tests/$docId"
        return try {
            val docRef = firestore.collection("users").document(userId)
                .collection("cci_tests").document(docId)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val doc = CciTestDoc.fromEntity(userId, entity)
                docRef.set(doc.toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(handleFirestoreError(e, OperationType.CREATE, path))
        }
    }

    suspend fun softDeleteTest(entity: CciTestEntity, updatedBy: String): Result<Unit> {
        val userId = requireUserId()
        val docId = testDocId(entity)
        val path = "users/$userId/cci_tests/$docId"
        return try {
            val docRef = firestore.collection("users").document(userId)
                .collection("cci_tests").document(docId)
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "deleted" to true,
                        "updatedBy" to updatedBy.take(100).ifBlank { "operator" },
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(handleFirestoreError(e, OperationType.UPDATE, path))
        }
    }

    suspend fun saveDailyReport(entity: DailyReport): Result<Unit> {
        val userId = requireUserId()
        val docId = reportDocId(entity)
        val path = "users/$userId/daily_reports/$docId"
        return try {
            val docRef = firestore.collection("users").document(userId)
                .collection("daily_reports").document(docId)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val doc = DailyReportDoc.fromEntity(userId, entity)
                docRef.set(doc.toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(handleFirestoreError(e, OperationType.CREATE, path))
        }
    }

    suspend fun deleteDailyReport(entity: DailyReport): Result<Unit> {
        val userId = requireUserId()
        val docId = reportDocId(entity)
        val path = "users/$userId/daily_reports/$docId"
        return try {
            firestore.collection("users").document(userId)
                .collection("daily_reports").document(docId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(handleFirestoreError(e, OperationType.DELETE, path))
        }
    }

    suspend fun upsertCrusherStatus(entity: CrusherStatusEntity): Result<Unit> {
        val userId = requireUserId()
        val path = "users/$userId/crusher_status/current"
        return try {
            val docRef = firestore.collection("users").document(userId)
                .collection("crusher_status").document("current")
            val doc = CrusherStatusDoc.fromEntity(userId, entity)
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(doc.toUpdateMap()).await()
            } else {
                docRef.set(doc.toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(handleFirestoreError(e, OperationType.WRITE, path))
        }
    }

    suspend fun saveChangeover(entity: ChangeoverEntity): Result<Unit> {
        val userId = requireUserId()
        val docId = changeoverDocId(entity)
        val path = "users/$userId/changeovers/$docId"
        return try {
            val docRef = firestore.collection("users").document(userId)
                .collection("changeovers").document(docId)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val doc = ChangeoverDoc.fromEntity(userId, entity)
                docRef.set(doc.toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(handleFirestoreError(e, OperationType.CREATE, path))
        }
    }

    suspend fun saveMaintenance(entity: MaintenanceEntity): Result<Unit> {
        val userId = requireUserId()
        val docId = maintenanceDocId(entity)
        val path = "users/$userId/maintenance/$docId"
        return try {
            val docRef = firestore.collection("users").document(userId)
                .collection("maintenance").document(docId)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val doc = MaintenanceDoc.fromEntity(userId, entity)
                docRef.set(doc.toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(handleFirestoreError(e, OperationType.CREATE, path))
        }
    }

    suspend fun approveMaintenance(entity: MaintenanceEntity, approvedBy: String): Result<Unit> {
        val userId = requireUserId()
        val docId = maintenanceDocId(entity)
        val path = "users/$userId/maintenance/$docId"
        return try {
            val docRef = firestore.collection("users").document(userId)
                .collection("maintenance").document(docId)
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "approvedBy" to approvedBy.take(100),
                        "updatedBy" to approvedBy.take(100).ifBlank { "operator" },
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val doc = MaintenanceDoc.fromEntity(
                    userId,
                    entity.copy(approvedBy = approvedBy, updatedBy = approvedBy)
                )
                docRef.set(doc.toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(handleFirestoreError(e, OperationType.UPDATE, path))
        }
    }

    suspend fun saveAlert(entity: AlertEntity): Result<Unit> {
        val userId = requireUserId()
        val docId = alertDocId(entity.id)
        val path = "users/$userId/alerts/$docId"
        return try {
            val docRef = firestore.collection("users").document(userId)
                .collection("alerts").document(docId)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val doc = AlertDoc.fromEntity(userId, entity)
                docRef.set(doc.toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(handleFirestoreError(e, OperationType.CREATE, path))
        }
    }

    suspend fun acknowledgeAlert(alertId: Long): Result<Unit> {
        val userId = requireUserId()
        val docId = alertDocId(alertId)
        val path = "users/$userId/alerts/$docId"
        return try {
            val docRef = firestore.collection("users").document(userId)
                .collection("alerts").document(docId)
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "acknowledged" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(handleFirestoreError(e, OperationType.UPDATE, path))
        }
    }

    suspend fun syncLocalSnapshotToCloud(
        crusherStatus: CrusherStatusEntity?,
        tests: List<CciTestEntity>,
        dailyReports: List<DailyReport>,
        changeovers: List<ChangeoverEntity>,
        maintenance: List<MaintenanceEntity>,
        alerts: List<AlertEntity>
    ): Result<Int> {
        var syncedCount = 0
        crusherStatus?.let {
            upsertCrusherStatus(it).onFailure { err -> return Result.failure(err) }
            syncedCount++
        }
        for (test in tests) {
            saveTest(test).onFailure { err -> return Result.failure(err) }
            syncedCount++
        }
        for (report in dailyReports) {
            saveDailyReport(report).onFailure { err -> return Result.failure(err) }
            syncedCount++
        }
        for (chg in changeovers) {
            saveChangeover(chg).onFailure { err -> return Result.failure(err) }
            syncedCount++
        }
        for (mnt in maintenance) {
            saveMaintenance(mnt).onFailure { err -> return Result.failure(err) }
            syncedCount++
        }
        for (alert in alerts) {
            saveAlert(alert).onFailure { err -> return Result.failure(err) }
            syncedCount++
        }
        return Result.success(syncedCount)
    }
}
