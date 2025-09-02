
package vsh.kttrs.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import vsh.kttrs.ui.ControlMode

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(context: Context) {

    private val dataStore = context.dataStore

    private object PreferencesKeys {
        val CONTROL_MODE = stringPreferencesKey("control_mode")
        val SHOW_GHOST_PIECE = booleanPreferencesKey("show_ghost_piece")
        val HIGH_SCORE = intPreferencesKey("high_score")
    }

    val controlMode: Flow<ControlMode> = dataStore.data
        .map { preferences ->
            val controlModeName = preferences[PreferencesKeys.CONTROL_MODE] ?: ControlMode.Buttons.name
            ControlMode.valueOf(controlModeName)
        }

    val showGhostPiece: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SHOW_GHOST_PIECE] ?: true
        }

    val highScore: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.HIGH_SCORE] ?: 0
        }

    suspend fun saveControlMode(controlMode: ControlMode) {
        dataStore.edit {
            it[PreferencesKeys.CONTROL_MODE] = controlMode.name
        }
    }

    suspend fun saveShowGhostPiece(show: Boolean) {
        dataStore.edit {
            it[PreferencesKeys.SHOW_GHOST_PIECE] = show
        }
    }

    suspend fun saveHighScore(score: Int) {
        dataStore.edit {
            it[PreferencesKeys.HIGH_SCORE] = score
        }
    }
}
