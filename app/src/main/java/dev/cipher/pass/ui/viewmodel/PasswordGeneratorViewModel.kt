package dev.cipher.pass.ui.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.SecureRandom
import javax.inject.Inject

@HiltViewModel
class PasswordGeneratorViewModel @Inject constructor() : ViewModel() {

    private val _generatedPassword = MutableStateFlow("")
    val generatedPassword: StateFlow<String> = _generatedPassword.asStateFlow()

    private val _length = MutableStateFlow(16)
    val length: StateFlow<Int> = _length.asStateFlow()

    private val _useUppercase = MutableStateFlow(true)
    val useUppercase: StateFlow<Boolean> = _useUppercase.asStateFlow()

    private val _useLowercase = MutableStateFlow(true)
    val useLowercase: StateFlow<Boolean> = _useLowercase.asStateFlow()

    private val _useNumbers = MutableStateFlow(true)
    val useNumbers: StateFlow<Boolean> = _useNumbers.asStateFlow()

    private val _useSymbols = MutableStateFlow(true)
    val useSymbols: StateFlow<Boolean> = _useSymbols.asStateFlow()

    private val _excludeAmbiguous = MutableStateFlow(false)
    val excludeAmbiguous: StateFlow<Boolean> = _excludeAmbiguous.asStateFlow()

    init {
        regenerate()
    }

    fun setLength(len: Int) {
        _length.value = len.coerceIn(8, 64)
        regenerate()
    }

    fun toggleUppercase() {
        _useUppercase.value = !_useUppercase.value
        regenerate()
    }

    fun toggleLowercase() {
        _useLowercase.value = !_useLowercase.value
        regenerate()
    }

    fun toggleNumbers() {
        _useNumbers.value = !_useNumbers.value
        regenerate()
    }

    fun toggleSymbols() {
        _useSymbols.value = !_useSymbols.value
        regenerate()
    }

    fun toggleExcludeAmbiguous() {
        _excludeAmbiguous.value = !_excludeAmbiguous.value
        regenerate()
    }

    fun regenerate() {
        var uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        var lowercase = "abcdefghijklmnopqrstuvwxyz"
        var numbers = "0123456789"
        var symbols = "!@#$%^&*-_+=?~"

        if (_excludeAmbiguous.value) {
            val ambiguousChars = listOf('I', 'O', 'l', 'o', '0', '1', '|')
            uppercase = uppercase.filterNot { it in ambiguousChars }
            lowercase = lowercase.filterNot { it in ambiguousChars }
            numbers = numbers.filterNot { it in ambiguousChars }
            symbols = symbols.filterNot { it in ambiguousChars }
        }

        val charset = StringBuilder()
        if (_useUppercase.value) charset.append(uppercase)
        if (_useLowercase.value) charset.append(lowercase)
        if (_useNumbers.value) charset.append(numbers)
        if (_useSymbols.value) charset.append(symbols)

        if (charset.isEmpty()) {
            _generatedPassword.value = ""
            return
        }

        val charArray = charset.toString().toCharArray()
        val random = SecureRandom()
        val password = CharArray(_length.value) { charArray[random.nextInt(charArray.size)] }
        _generatedPassword.value = String(password)
    }
}