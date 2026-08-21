package dev.cipher.pass.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PasswordRepository @Inject constructor(
    private val dao: EntryDao
) {

    fun getAllEntries(): Flow<List<PasswordEntry>> = dao.getAllEntries()
    fun searchEntries(query: String): Flow<List<PasswordEntry>> = dao.searchEntries(query)
    fun getEntriesByCategory(category: EntryCategory): Flow<List<PasswordEntry>> = dao.getEntriesByCategory(category)
    fun getEntryCount(): Flow<Int> = dao.getEntryCount()

    suspend fun getEntryById(id: String): PasswordEntry? = dao.getEntryById(id)

    suspend fun createEntry(category: EntryCategory): PasswordEntry {
        val entry = PasswordEntry(
            id = UUID.randomUUID().toString(),
            category = category,
            createdAt = System.currentTimeMillis(),
            modifiedAt = System.currentTimeMillis()
        )
        dao.insertEntry(entry)
        return entry
    }

    suspend fun saveEntry(entry: PasswordEntry) {
        dao.insertEntry(entry.copy(modifiedAt = System.currentTimeMillis()))
    }

    suspend fun deleteEntry(id: String) = dao.deleteEntryById(id)

    suspend fun deleteAllEntries() = dao.deleteAllEntries()

    suspend fun importEntries(entries: List<PasswordEntry>) {
        entries.forEach { entry ->
            saveEntry(entry)
        }
    }

    fun detectWeakPasswords(entries: List<PasswordEntry>): List<String> {
        return entries
            .filter { isWeakPassword(it.password) }
            .map { it.id }
    }

    fun getWeakPasswordEntries(entries: List<PasswordEntry>): List<PasswordEntry> {
        return entries.filter { isWeakPassword(it.password) }
    }

    fun detectReusedPasswords(entries: List<PasswordEntry>): Map<String, List<String>> {
        val passwordMap = mutableMapOf<String, MutableList<String>>()
        entries.forEach { entry ->
            if (entry.password.isNotBlank()) {
                passwordMap.getOrPut(entry.password) { mutableListOf() }.add(entry.id)
            }
        }
        return passwordMap.filter { it.value.size > 1 }
    }

    private fun isWeakPassword(password: String): Boolean {
        if (password.length < 8) return true
        if (password.all { it.isLowerCase() || it.isDigit() }) return true
        if (password.all { it.isUpperCase() || it.isDigit() }) return true
        if (isCommonPassword(password)) return true
        return false
    }

    private fun isCommonPassword(password: String): Boolean {
        val common = setOf(
            "password", "123456", "password123", "12345678", "qwerty",
            "abc123", "monkey", "letmein", "trustno1", "dragon"
        )
        return common.contains(password.lowercase())
    }

    suspend fun exportToCsv(context: Context, uri: Uri) = withContext(Dispatchers.IO) {
        val entries = dao.getAllEntriesList()
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            OutputStreamWriter(outputStream).use { writer ->
                writer.write("Name,Username,Password,Website,Category,Notes\n")
                entries.forEach { entry ->
                    val line = listOf(
                        escapeCsv(entry.name),
                        escapeCsv(entry.username),
                        escapeCsv(entry.password),
                        escapeCsv(entry.website),
                        escapeCsv(entry.category.name),
                        escapeCsv(entry.notes)
                    ).joinToString(",")
                    writer.write("$line\n")
                }
            }
        }
    }

    suspend fun importFromCsv(context: Context, uri: Uri) = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                val lines = reader.readLines()
                if (lines.isEmpty()) return@use

                val startIdx = if (lines[0].contains("Name", ignoreCase = true) || lines[0].contains("Title", ignoreCase = true)) 1 else 0

                val importedEntries = mutableListOf<PasswordEntry>()
                for (i in startIdx until lines.size) {
                    val line = lines[i].trim()
                    if (line.isEmpty()) continue

                    val tokens = parseCsvLine(line)
                    if (tokens.isNotEmpty()) {
                        val name = tokens.getOrNull(0) ?: "Imported Entry"
                        val username = tokens.getOrNull(1) ?: ""
                        val password = tokens.getOrNull(2) ?: ""
                        val website = tokens.getOrNull(3) ?: ""
                        val categoryStr = tokens.getOrNull(4) ?: EntryCategory.LOGIN.name
                        val notes = tokens.getOrNull(5) ?: ""

                        val category = runCatching { EntryCategory.valueOf(categoryStr) }.getOrDefault(EntryCategory.LOGIN)

                        importedEntries.add(
                            PasswordEntry(
                                id = UUID.randomUUID().toString(),
                                name = name,
                                username = username,
                                password = password,
                                website = website,
                                category = category,
                                notes = notes,
                                createdAt = System.currentTimeMillis(),
                                modifiedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }
                importEntries(importedEntries)
            }
        }
    }

    suspend fun exportToCipher(context: Context, uri: Uri, password: String) = withContext(Dispatchers.IO) {
        val entries = dao.getAllEntriesList()

        val csvBuilder = StringBuilder()
        entries.forEach { entry ->
            val line = listOf(
                escapeCsv(entry.name),
                escapeCsv(entry.username),
                escapeCsv(entry.password),
                escapeCsv(entry.website),
                escapeCsv(entry.category.name),
                escapeCsv(entry.notes)
            ).joinToString(",")
            csvBuilder.append(line).append("\n")
        }

        val plainData = csvBuilder.toString().toByteArray(Charsets.UTF_8)

        val random = SecureRandom()
        val salt = ByteArray(16).also { random.nextBytes(it) }
        val iv = ByteArray(12).also { random.nextBytes(it) }

        val secretKey = deriveKey(password, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val parameterSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec)

        val encryptedData = cipher.doFinal(plainData)

        context.contentResolver.openOutputStream(uri)?.use { output ->
            output.write(salt)
            output.write(iv)
            output.write(encryptedData)
        }
    }

    suspend fun importFromCipher(context: Context, uri: Uri, password: String) = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val bytes = input.readBytes()
            if (bytes.size < 28) throw IllegalArgumentException("File corrupted or invalid format")

            val salt = bytes.copyOfRange(0, 16)
            val iv = bytes.copyOfRange(16, 28)
            val encryptedData = bytes.copyOfRange(28, bytes.size)

            val secretKey = deriveKey(password, salt)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val parameterSpec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec)

            val decryptedBytes = cipher.doFinal(encryptedData)
            val csvText = String(decryptedBytes, Charsets.UTF_8)

            val importedEntries = mutableListOf<PasswordEntry>()
            csvText.lineSequence().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isNotEmpty()) {
                    val tokens = parseCsvLine(trimmed)
                    if (tokens.isNotEmpty()) {
                        val category = runCatching {
                            EntryCategory.valueOf(tokens.getOrNull(4) ?: "")
                        }.getOrDefault(EntryCategory.LOGIN)

                        importedEntries.add(
                            PasswordEntry(
                                id = UUID.randomUUID().toString(),
                                name = tokens.getOrNull(0) ?: "Imported Entry",
                                username = tokens.getOrNull(1) ?: "",
                                password = tokens.getOrNull(2) ?: "",
                                website = tokens.getOrNull(3) ?: "",
                                category = category,
                                notes = tokens.getOrNull(5) ?: "",
                                createdAt = System.currentTimeMillis(),
                                modifiedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }
            importEntries(importedEntries)
        }
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, 10_000, 256)
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false

        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    sb.append('"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                result.add(sb.toString())
                sb.clear()
            } else {
                sb.append(c)
            }
            i++
        }
        result.add(sb.toString())
        return result
    }
}