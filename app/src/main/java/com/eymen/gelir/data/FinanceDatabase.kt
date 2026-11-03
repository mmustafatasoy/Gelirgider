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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    val createdAt: Long = System.currentTimeMillis(),
    val receiptFileName: String? = null
)

@Entity(tableName = "categories", indices = [Index(value = ["name", "kind"], unique = true)])
data class FinanceCategory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kind: String,
    val emoji: String,
    val isDefault: Boolean = false
)

@Entity(tableName = "budget_limits")
data class BudgetLimit(
    @PrimaryKey val category: String,
    val monthlyLimitKurus: Long
)

/**
 * Recurrent monthly reminder. dayOfMonth is clamped to the last day for shorter months.
 * Composite uniqueness prevents duplicating the same reminder after ZIP imports.
 */
@Entity(
    tableName = "payment_reminders",
    indices = [Index(value = ["category", "title", "dayOfMonth"], unique = true)]
)
data class PaymentReminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val title: String,
    val dayOfMonth: Int,
    val enabled: Boolean = true,
    val lastNotifiedMonth: String? = null
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

    @Query("SELECT * FROM budget_limits ORDER BY category COLLATE NOCASE")
    fun observeBudgetLimits(): Flow<List<BudgetLimit>>

    @Query("SELECT * FROM budget_limits ORDER BY category COLLATE NOCASE")
    suspend fun allBudgetLimits(): List<BudgetLimit>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveBudgetLimit(budget: BudgetLimit)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun importBudgetLimit(budget: BudgetLimit): Long

    @Query("DELETE FROM budget_limits WHERE category = :category")
    suspend fun deleteBudgetLimit(category: String)

    @Query("SELECT * FROM payment_reminders ORDER BY dayOfMonth, category COLLATE NOCASE")
    fun observePaymentReminders(): Flow<List<PaymentReminder>>

    @Query("SELECT * FROM payment_reminders ORDER BY dayOfMonth, category COLLATE NOCASE")
    suspend fun allPaymentReminders(): List<PaymentReminder>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPaymentReminder(reminder: PaymentReminder): Long

    @Update
    suspend fun updatePaymentReminder(reminder: PaymentReminder)

    @Delete
    suspend fun deletePaymentReminder(reminder: PaymentReminder)

    @Query("UPDATE payment_reminders SET lastNotifiedMonth = :month WHERE id = :id AND (lastNotifiedMonth IS NULL OR lastNotifiedMonth != :month)")
    suspend fun markReminderNotified(id: Long, month: String): Int

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

@Database(
    entities = [FinanceEntry::class, FinanceCategory::class, BudgetLimit::class, PaymentReminder::class],
    version = 3,
    exportSchema = false
)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao

    companion object {
        @Volatile private var instance: FinanceDatabase? = null
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE entries ADD COLUMN receiptFileName TEXT")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS budget_limits (" +
                        "category TEXT NOT NULL PRIMARY KEY, " +
                        "monthlyLimitKurus INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS payment_reminders (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "category TEXT NOT NULL, " +
                        "title TEXT NOT NULL, " +
                        "dayOfMonth INTEGER NOT NULL, " +
                        "enabled INTEGER NOT NULL, " +
                        "lastNotifiedMonth TEXT)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_payment_reminders_category_title_dayOfMonth " +
                        "ON payment_reminders (category, title, dayOfMonth)"
                )
            }
        }

        fun get(context: Context): FinanceDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FinanceDatabase::class.java,
                    "eymen_gelir.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
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
