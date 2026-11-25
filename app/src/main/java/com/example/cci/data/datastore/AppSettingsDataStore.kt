package com.example.cci.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.cci.core.constants.AppConstants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "evonith_cci_prefs")

data class PlantSettings(
    val companyName: String = AppConstants.COMPANY_NAME,
    val departmentName: String = AppConstants.DEPARTMENT_NAME,
    val targetMinCci: Double = 40.0,
    val maxFinesIncreasePp: Double = 22.0,
    val themeMode: String = "SYSTEM",
    val loggedInUsername: String = "shiftincharge",
    val whatsappSignOff: String = "Shift In-Charge — Sinter Plant"
)

class AppSettingsDataStore(private val context: Context) {

    private object Keys {
        val COMPANY_NAME = stringPreferencesKey("company_name")
        val DEPARTMENT_NAME = stringPreferencesKey("department_name")
        val TARGET_MIN_CCI = doublePreferencesKey("target_min_cci")
        val MAX_FINES_INCREASE = doublePreferencesKey("max_fines_increase")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LOGGED_IN_USER = stringPreferencesKey("logged_in_user")
        val WHATSAPP_SIGN_OFF = stringPreferencesKey("whatsapp_sign_off")
    }

    val settingsFlow: Flow<PlantSettings> = context.dataStore.data.map { prefs ->
        PlantSettings(
            companyName = prefs[Keys.COMPANY_NAME] ?: AppConstants.COMPANY_NAME,
            departmentName = prefs[Keys.DEPARTMENT_NAME] ?: AppConstants.DEPARTMENT_NAME,
            targetMinCci = prefs[Keys.TARGET_MIN_CCI] ?: 40.0,
            maxFinesIncreasePp = prefs[Keys.MAX_FINES_INCREASE] ?: 22.0,
            themeMode = prefs[Keys.THEME_MODE] ?: "SYSTEM",
            loggedInUsername = prefs[Keys.LOGGED_IN_USER] ?: "shiftincharge",
            whatsappSignOff = prefs[Keys.WHATSAPP_SIGN_OFF] ?: "Shift In-Charge — Sinter Plant"
        )
    }

    suspend fun updatePlantConfig(
        companyName: String,
        departmentName: String,
        targetMinCci: Double,
        maxFinesIncreasePp: Double,
        whatsappSignOff: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.COMPANY_NAME] = companyName.trim().ifBlank { AppConstants.COMPANY_NAME }
            prefs[Keys.DEPARTMENT_NAME] = departmentName.trim().ifBlank { AppConstants.DEPARTMENT_NAME }
            prefs[Keys.TARGET_MIN_CCI] = targetMinCci.coerceIn(10.0, 99.0)
            prefs[Keys.MAX_FINES_INCREASE] = maxFinesIncreasePp.coerceIn(1.0, 80.0)
            prefs[Keys.WHATSAPP_SIGN_OFF] = whatsappSignOff.trim().ifBlank { "Shift In-Charge — Sinter Plant" }
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    suspend fun setLoggedInUsername(username: String) {
        context.dataStore.edit { it[Keys.LOGGED_IN_USER] = username }
    }
}
