
package com.example.kttrs.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.kttrs.ui.ControlMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(context: Context) {

    private val dataStore = context.dataStore

    private object PreferencesKeys {
        val CONTROL_MODE = stringPreferencesKey("control_mode")
    }

    val controlMode: Flow<ControlMode> = dataStore.data
        .map { preferences ->
            val controlModeName = preferences[PreferencesKeys.CONTROL_MODE] ?: ControlMode.Buttons.name
            ControlMode.valueOf(controlModeName)
        }

    suspend fun saveControlMode(controlMode: ControlMode) {
        dataStore.edit {
            it[PreferencesKeys.CONTROL_MODE] = controlMode.name
        }
    }
}
