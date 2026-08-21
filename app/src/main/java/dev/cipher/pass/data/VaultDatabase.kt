package dev.cipher.pass.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [PasswordEntry::class], version = 1, exportSchema = false)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
}
