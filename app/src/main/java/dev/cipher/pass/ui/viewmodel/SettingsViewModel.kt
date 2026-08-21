package dev.cipher.pass.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.cipher.pass.data.PasswordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: PasswordRepository,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    companion object {
        private val DYNAMIC_COLORS_KEY = booleanPreferencesKey("use_dynamic_colors")
        private val APP_LOCK_KEY = booleanPreferencesKey("app_lock_enabled")
        private val APP_PIN_KEY = stringPreferencesKey("app_pin")
        private val BIOMETRIC_ENABLED_KEY = booleanPreferencesKey("biometric_enabled")
    }

    private val _backupState = MutableStateFlow<String?>(null)
    val backupState: StateFlow<String?> = _backupState.asStateFlow()

    val useDynamicColors: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[DYNAMIC_COLORS_KEY] ?: true
        }

    fun setDynamicColors(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[DYNAMIC_COLORS_KEY] = enabled
            }
        }
    }

    val isAppLockEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[APP_LOCK_KEY] ?: false
        }

    fun setAppLock(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[APP_LOCK_KEY] = enabled
            }
        }
    }

    val isBiometricEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[BIOMETRIC_ENABLED_KEY] ?: true
        }

    fun setBiometric(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[BIOMETRIC_ENABLED_KEY] = enabled
            }
        }
    }

    val appPin: Flow<String?> = dataStore.data
        .map { preferences ->
            preferences[APP_PIN_KEY]
        }

    fun setAppPin(pin: String?) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                if (pin == null) {
                    preferences.remove(APP_PIN_KEY)
                } else {
                    preferences[APP_PIN_KEY] = pin
                }
            }
        }
    }

    fun exportToCsv(context: Context, uri: Uri) {
        viewModelScope.launch {
            runCatching {
                repo.exportToCsv(context, uri)
            }.onSuccess {
                _backupState.value = "CSV backup exported successfully!"
            }.onFailure { error ->
                _backupState.value = "Export failed: ${error.localizedMessage}"
            }
        }
    }

    fun importFromCsv(context: Context, uri: Uri) {
        viewModelScope.launch {
            runCatching {
                repo.importFromCsv(context, uri)
            }.onSuccess {
                _backupState.value = "Vault restored from CSV successfully!"
            }.onFailure { error ->
                _backupState.value = "Import failed: ${error.localizedMessage}"
            }
        }
    }

    fun exportToCipher(context: Context, uri: Uri, password: String) {
        viewModelScope.launch {
            runCatching {
                repo.exportToCipher(context, uri, password)
            }.onSuccess {
                _backupState.value = "Encrypted backup created successfully!"
            }.onFailure { error ->
                _backupState.value = "Export failed: ${error.localizedMessage}"
            }
        }
    }

    fun importFromCipher(context: Context, uri: Uri, password: String) {
        viewModelScope.launch {
            runCatching {
                repo.importFromCipher(context, uri, password)
            }.onSuccess {
                _backupState.value = "Vault restored successfully!"
            }.onFailure { error ->
                _backupState.value = "Import failed: Wrong password or corrupt file."
            }
        }
    }

    fun clearState() {
        _backupState.value = null
    }

    fun nuclearWipe() {
        viewModelScope.launch {
            repo.deleteAllEntries()
            dataStore.edit { preferences ->
                preferences.clear()
            }
        }
    }
}