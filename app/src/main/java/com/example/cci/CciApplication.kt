package com.example.cci

import android.app.Application
import com.example.R
import com.example.cci.core.security.SessionManager
import com.example.cci.data.datastore.AppSettingsDataStore
import com.example.cci.data.local.CciDatabase
import com.example.cci.data.remote.FirestorePlantRepository
import com.example.cci.data.repository.AuthRepository
import com.example.cci.data.repository.CrusherRepository
import com.example.cci.data.repository.DailyReportRepository
import com.example.cci.data.repository.MaintenanceRepository
import com.example.cci.data.repository.SettingsRepository
import com.example.cci.data.repository.TestRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(application: Application) {
    val database: CciDatabase = CciDatabase.getInstance(application)
    val settingsDataStore: AppSettingsDataStore = AppSettingsDataStore(application)
    val sessionManager: SessionManager = SessionManager()

    private val firestoreDatabaseId: String =
        application.getString(R.string.firestore_database_id)
    val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(firestoreDatabaseId)
    val firestorePlantRepository: FirestorePlantRepository = FirestorePlantRepository(
        firestore = firestore,
        auth = Firebase.auth
    )

    val authRepository: AuthRepository = AuthRepository(
        userDao = database.userDao(),
        auditLogDao = database.auditLogDao(),
        settingsDataStore = settingsDataStore,
        sessionManager = sessionManager
    )
    val testRepository: TestRepository = TestRepository(
        testDao = database.testDao(),
        crusherStatusDao = database.crusherStatusDao(),
        alertDao = database.alertDao(),
        auditLogDao = database.auditLogDao(),
        settingsDataStore = settingsDataStore,
        sessionManager = sessionManager
    )
    val crusherRepository: CrusherRepository = CrusherRepository(
        database = database,
        sessionManager = sessionManager
    )
    val maintenanceRepository: MaintenanceRepository = MaintenanceRepository(
        maintenanceDao = database.maintenanceDao(),
        auditLogDao = database.auditLogDao(),
        sessionManager = sessionManager
    )
    val settingsRepository: SettingsRepository = SettingsRepository(
        database = database,
        settingsDataStore = settingsDataStore,
        sessionManager = sessionManager
    )
    val dailyReportRepository: DailyReportRepository = DailyReportRepository(
        dailyReportDao = database.dailyReportDao(),
        auditLogDao = database.auditLogDao(),
        settingsDataStore = settingsDataStore,
        sessionManager = sessionManager
    )
}

class CciApplication : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        appScope.launch {
            CciDatabase.ensureSeeded(container.database)
            container.authRepository.restoreSession()
        }
    }
}
