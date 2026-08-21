package dev.cipher.pass.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.cipher.pass.crypto.CryptoManager
import dev.cipher.pass.data.EntryCategory
import dev.cipher.pass.data.PasswordEntry
import dev.cipher.pass.data.PasswordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class EntryEditorViewModel @Inject constructor(
    private val repository: PasswordRepository,
    private val crypto: CryptoManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val entryId: String? = savedStateHandle["entryId"]

    private val _entry = MutableStateFlow<PasswordEntry?>(null)
    val entry: StateFlow<PasswordEntry?> = _entry.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    init {
        viewModelScope.launch {
            if (entryId != null && entryId != "new") {
                _entry.value = repository.getEntryById(entryId)
            } else {
                _entry.value = PasswordEntry(
                    id = UUID.randomUUID().toString(),
                    name = "",
                    username = "",
                    password = "",
                    website = "",
                    notes = "",
                    category = EntryCategory.LOGIN
                )
            }
        }
    }

    fun updateName(name: String) {
        _entry.value = _entry.value?.copy(name = name)
    }

    fun updateUsername(username: String) {
        _entry.value = _entry.value?.copy(username = username)
    }

    fun updatePassword(password: String) {
        _entry.value = _entry.value?.copy(password = password)
    }

    fun updateWebsite(website: String) {
        _entry.value = _entry.value?.copy(website = website)
    }

    fun updateNotes(notes: String) {
        _entry.value = _entry.value?.copy(notes = notes)
    }

    fun updateCategory(category: EntryCategory) {
        _entry.value = _entry.value?.copy(category = category)
    }

    fun saveEntry(onSuccess: () -> Unit = {}) {
        val currentEntry = _entry.value ?: return

        val entryToSave = if (currentEntry.name.isBlank()) {
            currentEntry.copy(name = "New Entry")
        } else {
            currentEntry
        }

        _isSaving.value = true
        viewModelScope.launch {
            try {
                repository.saveEntry(entryToSave)
                onSuccess()
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun deleteEntry(onSuccess: () -> Unit = {}) {
        val currentEntry = _entry.value ?: return
        viewModelScope.launch {
            repository.deleteEntry(currentEntry.id)
            onSuccess()
        }
    }

    fun getPasswordStrength(password: String) = crypto.passwordStrength(password)
}