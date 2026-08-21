package dev.cipher.pass.data

import android.content.Context
import dev.cipher.pass.crypto.CryptoManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton
import java.util.UUID

@Singleton
class BackupManager @Inject constructor(
    private val cryptoManager: CryptoManager
) {
    fun exportToCsv(entries: List<PasswordEntry>, outputStream: OutputStream) {
        val csvHeader = "title,username,password,website,notes,category\n"
        outputStream.bufferedWriter().use { writer ->
            writer.write(csvHeader)
            entries.forEach { entry ->
                val line = listOf(
                    escapeCsv(entry.name),
                    escapeCsv(entry.username),
                    escapeCsv(entry.password),
                    escapeCsv(entry.website),
                    escapeCsv(entry.notes),
                    entry.category.name
                ).joinToString(",")
                writer.write(line + "\n")
            }
        }
    }

    fun importFromCsv(inputStream: InputStream): List<PasswordEntry> {
        val entries = mutableListOf<PasswordEntry>()
        val lines = inputStream.bufferedReader().readLines()

        if (lines.isEmpty()) return emptyList()

        lines.drop(1).forEach { line ->
            if (line.isNotBlank()) {
                val tokens = parseCsvLine(line)
                if (tokens.size >= 3) {
                    entries.add(
                        PasswordEntry(
                            id = UUID.randomUUID().toString(),
                            name = tokens.getOrElse(0) { "Imported Entry" },
                            username = tokens.getOrElse(1) { "" },
                            password = tokens.getOrElse(2) { "" },
                            website = tokens.getOrElse(3) { "" },
                            notes = tokens.getOrElse(4) { "" },
                            category = runCatching { EntryCategory.valueOf(tokens.getOrElse(5) { "LOGIN" }) }.getOrDefault(EntryCategory.LOGIN)
                        )
                    )
                }
            }
        }
        return entries
    }

    fun exportToCipher(entries: List<PasswordEntry>, password: String, outputStream: OutputStream) {
        val jsonArray = JSONArray()
        entries.forEach { entry ->
            val obj = JSONObject().apply {
                put("name", entry.name)
                put("username", entry.username)
                put("password", entry.password)
                put("website", entry.website)
                put("notes", entry.notes)
                put("category", entry.category.name)
            }
            jsonArray.put(obj)
        }

        val encryptedBytes = cryptoManager.encryptWithPassword(jsonArray.toString(), password)
        outputStream.use { it.write(encryptedBytes) }
    }

    fun importFromCipher(inputStream: InputStream, password: String): List<PasswordEntry> {
        val bytes = inputStream.readBytes()
        val jsonString = cryptoManager.decryptWithPassword(bytes, password)
        val jsonArray = JSONArray(jsonString)
        val entries = mutableListOf<PasswordEntry>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            entries.add(
                PasswordEntry(
                    id = UUID.randomUUID().toString(),
                    name = obj.optString("name", "Imported Entry"),
                    username = obj.optString("username", ""),
                    password = obj.optString("password", ""),
                    website = obj.optString("website", ""),
                    notes = obj.optString("notes", ""),
                    category = runCatching { EntryCategory.valueOf(obj.optString("category", "LOGIN")) }.getOrDefault(EntryCategory.LOGIN)
                )
            )
        }
        return entries
    }

    private fun escapeCsv(text: String): String {
        var escaped = text.replace("\"", "\"\"")
        if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\"")) {
            escaped = "\"$escaped\""
        }
        return escaped
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false

        for (ch in line) {
            when {
                ch == '\"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    result.add(sb.toString().trim())
                    sb.clear()
                }
                else -> sb.append(ch)
            }
        }
        result.add(sb.toString().trim())
        return result
    }
}