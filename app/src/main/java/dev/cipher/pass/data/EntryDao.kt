package dev.cipher.pass.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {

    @Query("SELECT * FROM entries ORDER BY modifiedAt DESC")
    fun getAllEntries(): Flow<List<PasswordEntry>>

    @Query("SELECT * FROM entries ORDER BY modifiedAt DESC")
    suspend fun getAllEntriesList(): List<PasswordEntry>

    @Query("SELECT * FROM entries WHERE id = :id")
    suspend fun getEntryById(id: String): PasswordEntry?

    @Query("""
        SELECT * FROM entries WHERE
        (name LIKE '%' || :query || '%' OR username LIKE '%' || :query || '%')
        ORDER BY modifiedAt DESC
    """)
    fun searchEntries(query: String): Flow<List<PasswordEntry>>

    @Query("SELECT * FROM entries WHERE category = :category ORDER BY modifiedAt DESC")
    fun getEntriesByCategory(category: EntryCategory): Flow<List<PasswordEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: PasswordEntry)

    @Update
    suspend fun updateEntry(entry: PasswordEntry)

    @Delete
    suspend fun deleteEntry(entry: PasswordEntry)

    @Query("DELETE FROM entries WHERE id = :id")
    suspend fun deleteEntryById(id: String)

    @Query("SELECT COUNT(*) FROM entries")
    fun getEntryCount(): Flow<Int>

    @Query("DELETE FROM entries")
    suspend fun deleteAllEntries()
}