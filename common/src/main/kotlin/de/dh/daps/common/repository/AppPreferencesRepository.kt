package de.dh.daps.common.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import de.dh.daps.common.DEFAULT_CATHETER_PATCH_WARNING_TIME
import de.dh.daps.common.domain.model.therapy.CarbsUnit
import de.dh.daps.common.domain.model.therapy.GlucoseUnit
import java.time.LocalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

val Preferences?.userDeclinedPermissions: Boolean
    get() = this?.get(USER_DECLINED_PERMISSIONS_KEY) ?: false

suspend fun AppPreferencesRepository.setUserDeclinedPermissions(value: Boolean) {
    editPreferences { mutablePreferences ->
        mutablePreferences[USER_DECLINED_PERMISSIONS_KEY] = value
    }
}

val Preferences?.glucoseUnit: GlucoseUnit
    get() = this?.get(GLUCOSE_UNIT_KEY)?.let { GlucoseUnit.valueOf(it) } ?: GlucoseUnit.MG_DL

suspend fun AppPreferencesRepository.setGlucoseUnit(value: GlucoseUnit) {
    editPreferences { mutablePreferences ->
        mutablePreferences[GLUCOSE_UNIT_KEY] = value.name
    }
}

val Preferences?.carbsUnit: CarbsUnit
    get() = this?.get(CARBS_UNIT_KEY)?.let { CarbsUnit.valueOf(it) } ?: CarbsUnit.GRAMS

suspend fun AppPreferencesRepository.setCarbsUnit(value: CarbsUnit) {
    editPreferences { mutablePreferences ->
        mutablePreferences[CARBS_UNIT_KEY] = value.name
    }
}

val Preferences?.catheterPatchWarningTime: LocalTime
    get() = this?.get(CATHETER_PATCH_WARNING_TIME_KEY)?.let {
        try {
            LocalTime.parse(it)
        } catch (_: Exception) {
            DEFAULT_CATHETER_PATCH_WARNING_TIME
        }
    } ?: DEFAULT_CATHETER_PATCH_WARNING_TIME

suspend fun AppPreferencesRepository.setCatheterPatchWarningTime(value: LocalTime) {
    editPreferences { mutablePreferences ->
        mutablePreferences[CATHETER_PATCH_WARNING_TIME_KEY] = value.toString()
    }
}

val Preferences?.backupDirectoryUri: String?
    get() = this?.get(BACKUP_DIRECTORY_URI_KEY)

suspend fun AppPreferencesRepository.setBackupDirectoryUri(uriString: String?) {
    editPreferences { mutablePreferences ->
        if (uriString != null) {
            mutablePreferences[BACKUP_DIRECTORY_URI_KEY] = uriString
        } else {
            mutablePreferences.remove(BACKUP_DIRECTORY_URI_KEY)
        }
    }
}

val USER_DECLINED_PERMISSIONS_KEY = booleanPreferencesKey("user_declined_permissions")
val GLUCOSE_UNIT_KEY = stringPreferencesKey("glucose_unit")
val CARBS_UNIT_KEY = stringPreferencesKey("carbs_unit")
val CATHETER_PATCH_WARNING_TIME_KEY = stringPreferencesKey("catheter_patch_warning_time")
val BACKUP_DIRECTORY_URI_KEY = stringPreferencesKey("backup_directory_uri")

class AppPreferencesRepository(private val context: Context, private val scope: CoroutineScope) {
    /**
     * Gets the eagerly loaded state of the preferences as StateFlow, i.e. it can be queried
     * without the use of a suspend function, but the value will initially be {@ null} until
     * the preferences are loaded.
     */
    val cachedPreferences: StateFlow<Preferences?> = context.dataStore.data
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    val preferences = context.dataStore.data

    val glucoseUnit: StateFlow<GlucoseUnit> = cachedPreferences
        .map { it.glucoseUnit }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = GlucoseUnit.MG_DL
        )

    val carbsUnit: StateFlow<CarbsUnit> = cachedPreferences
        .map { it.carbsUnit }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = CarbsUnit.GRAMS
        )

    val catheterPatchWarningTime: StateFlow<LocalTime> = cachedPreferences
        .map { it.catheterPatchWarningTime }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = DEFAULT_CATHETER_PATCH_WARNING_TIME
        )

    val backupDirectoryUri: StateFlow<String?> = cachedPreferences
        .map { it.backupDirectoryUri }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    /**
     * Gets the current preferences as a suspend function.
     */
    suspend fun getPreferences(): Preferences {
        return context.dataStore.data.first()
    }

    suspend fun editPreferences(block: (MutablePreferences) -> Unit) {
        context.dataStore.edit { mutablePreferences ->
            block(mutablePreferences)
        }
    }
}