package dev.cipher.pass.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.cipher.pass.crypto.CryptoManager
import dev.cipher.pass.data.PasswordEntry
import dev.cipher.pass.data.PasswordRepository
import dev.cipher.pass.data.EntryCategory
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val repository: PasswordRepository,
    private val crypto: CryptoManager
) : ViewModel() {

    private val _masterPassword = MutableStateFlow("")
    val masterPassword: StateFlow<String> = _masterPassword.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<EntryCategory?>(null)
    val selectedCategory: StateFlow<EntryCategory?> = _selectedCategory.asStateFlow()

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag.asStateFlow()

    val allEntries = repository.getAllEntries().stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        emptyList()
    )

    val allTags: StateFlow<List<String>> = repository.getAllEntries()
        .map { entries ->
            val regex = Regex("#(\\w+)")
            entries.flatMap { entry ->
                regex.findAll(entry.notes).map { it.groupValues[1] }
            }.distinct().sorted()
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val filteredEntries: StateFlow<List<PasswordEntry>> = combine(
        _searchQuery,
        _selectedCategory,
        _selectedTag,
        allEntries
    ) { query, category, tag, entries ->
        entries.filter { entry ->
            val matchesQuery = if (query.isBlank()) true else {
                entry.name.contains(query, ignoreCase = true) ||
                        entry.username.contains(query, ignoreCase = true)
            }
            val matchesCategory = category == null || entry.category == category
            val matchesTag = tag == null || entry.notes.contains("#$tag", ignoreCase = true)

            matchesQuery && matchesCategory && matchesTag
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val entryCount = repository.getEntryCount()
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val weakPasswords: StateFlow<List<PasswordEntry>> = repository.getAllEntries()
        .map { repository.getWeakPasswordEntries(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val reusedPasswords = allEntries.map { entries ->
        repository.detectReusedPasswords(entries)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    fun selectTag(tag: String?) {
        _selectedTag.value = if (_selectedTag.value == tag) null else tag
    }

    fun setMasterPassword(password: String) {
        _masterPassword.value = password
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: EntryCategory?) {
        _selectedCategory.value = category
    }

    fun getPasswordStrength(password: String) = crypto.passwordStrength(password)
}