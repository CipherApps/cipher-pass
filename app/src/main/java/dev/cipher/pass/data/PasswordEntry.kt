package dev.cipher.pass.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class EntryCategory { LOGIN, CARD, NOTE }
enum class PasswordStrength { WEAK, MODERATE, STRONG, VERY_STRONG }

@Entity(tableName = "entries")
data class PasswordEntry(
    @PrimaryKey val id: String,
    val category: EntryCategory = EntryCategory.LOGIN,
    val name: String = "",
    val username: String = "",
    val password: String = "",
    val website: String = "",
    val notes: String = "",
    val tags: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis()
)

data class PasswordEntryWithStrength(
    val entry: PasswordEntry,
    val strength: PasswordStrength,
    val isReused: Boolean = false
)
