package com.evonith.cci

import android.app.Application
import com.evonith.cci.core.security.SessionManager
import com.evonith.cci.data.datastore.AppSettingsDataStore
import com.evonith.cci.data.local.CciDatabase
import com.evonith.cci.data.remote.FirestoreSyncService
import com.evonith.cci.data.repository.AuthRepository
import com.evonith.cci.data.repository.CrusherRepository
import com.evonith.cci.data.repository.MaintenanceRepository
import com.evonith.cci.data.repository.SettingsRepository
import com.evonith.cci.data.repository.TestRepository

class AppContainer(application: Application) {
    val database: CciDatabase = CciDatabase.getInstance(application)
    val settingsDataStore: AppSettingsDataStore = AppSettingsDataStore(application)
    val sessionManager: SessionManager = SessionManager()
    val firestoreSyncService: FirestoreSyncService = FirestoreSyncService(
        context = application,
        testDao = database.testDao(),
        changeoverDao = database.changeoverDao(),
        maintenanceDao = database.maintenanceDao()
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
        settingsDataStore = settingsDataStore,
        sessionManager = sessionManager
    )
    val settingsRepository: SettingsRepository = SettingsRepository(
        context = application,
        database = database,
        settingsDataStore = settingsDataStore,
        sessionManager = sessionManager,
        firestoreSyncService = firestoreSyncService
    )
}

class CciApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
