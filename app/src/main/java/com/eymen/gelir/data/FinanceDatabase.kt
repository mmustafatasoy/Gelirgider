package com.eymen.gelir.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "entries", indices = [Index(value = ["entryUuid"], unique = true), Index("occurredAt")])
data class FinanceEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entryUuid: String,
    val kind: String,
    val category: String,
    val amountKurus: Long,
    val description: String,
    val occurredAt: Long,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "categories", indices = [Index(value = ["name", "kind"], unique = true)])
data class FinanceCategory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kind: String,
    val emoji: String,
    val isDefault: Boolean = false
)

@Dao
interface FinanceDao {
    @Query("SELECT * FROM entries ORDER BY occurredAt DESC, id DESC")
    fun observeEntries(): Flow<List<FinanceEntry>>

    @Query("SELECT * FROM categories ORDER BY isDefault DESC, name COLLATE NOCASE")
    fun observeCategories(): Flow<List<FinanceCategory>>

    @Query("SELECT * FROM entries ORDER BY occurredAt DESC, id DESC")
    suspend fun allEntries(): List<FinanceEntry>

    @Query("SELECT * FROM categories ORDER BY name COLLATE NOCASE")
    suspend fun allCategories(): List<FinanceCategory>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun countCategories(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: FinanceCategory): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEntry(entry: FinanceEntry): Long

    @Update
    suspend fun updateEntry(entry: FinanceEntry)

    @Delete
    suspend fun deleteEntry(entry: FinanceEntry)

    @Delete
    suspend fun deleteCategory(category: FinanceCategory)
}

@Database(entities = [FinanceEntry::class, FinanceCategory::class], version = 1, exportSchema = false)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao

    companion object {
        @Volatile private var instance: FinanceDatabase? = null

        fun get(context: Context): FinanceDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FinanceDatabase::class.java,
                    "eymen_gelir.db"
                ).build().also { instance = it }
            }
    }
}

val DEFAULT_CATEGORIES = listOf(
    FinanceCategory(name = "Maaş", kind = "INCOME", emoji = "💰", isDefault = true),
    FinanceCategory(name = "Ek Gelir", kind = "INCOME", emoji = "🪙", isDefault = true),
    FinanceCategory(name = "Diğer Gelir", kind = "INCOME", emoji = "🎁", isDefault = true),
    FinanceCategory(name = "Elektrik", kind = "EXPENSE", emoji = "⚡", isDefault = true),
    FinanceCategory(name = "Su", kind = "EXPENSE", emoji = "💧", isDefault = true),
    FinanceCategory(name = "Doğalgaz", kind = "EXPENSE", emoji = "🔥", isDefault = true),
    FinanceCategory(name = "Bina Aidatı", kind = "EXPENSE", emoji = "🏢", isDefault = true),
    FinanceCategory(name = "Market", kind = "EXPENSE", emoji = "🛒", isDefault = true),
    FinanceCategory(name = "İnternet", kind = "EXPENSE", emoji = "📶", isDefault = true),
    FinanceCategory(name = "Ulaşım", kind = "EXPENSE", emoji = "🚘", isDefault = true),
    FinanceCategory(name = "Sağlık", kind = "EXPENSE", emoji = "❤️", isDefault = true),
    FinanceCategory(name = "Eğitim", kind = "EXPENSE", emoji = "📚", isDefault = true),
    FinanceCategory(name = "Diğer", kind = "EXPENSE", emoji = "🟣", isDefault = true)
)
