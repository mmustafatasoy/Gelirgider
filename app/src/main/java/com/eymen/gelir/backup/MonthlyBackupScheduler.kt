package com.eymen.gelir.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.eymen.gelir.data.FinanceDatabase
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/** Opt-in SAF folder permission; no unrestricted file-system or internet access. */
object MonthlyBackupScheduler {
    private const val PREFS = "eymen_gelir_monthly_backup"
    private const val FOLDER = "folder_uri"
    private const val LAST = "last_month"
    private const val STATUS = "last_status"
    private const val PERIODIC_NAME = "eymen-gelir-monthly-backup-check"
    private const val IMMEDIATE_NAME = "eymen-gelir-monthly-backup-initial"

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun folder(context: Context): Uri? =
        preferences(context).getString(FOLDER, null)?.let(Uri::parse)

    fun lastCompletedMonth(context: Context): String =
        preferences(context).getString(LAST, "Henüz yok") ?: "Henüz yok"

    fun status(context: Context): String =
        preferences(context).getString(STATUS, "Yedek bekleniyor.") ?: "Yedek bekleniyor."

    fun enable(context: Context, treeUri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            treeUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        preferences(context).edit()
            .putString(FOLDER, treeUri.toString())
            .remove(LAST)
            .putString(STATUS, "Klasör seçildi. Tamamlanan aylar için otomatik yedek açık.")
            .apply()
        val work = WorkManager.getInstance(context)
        work.enqueueUniquePeriodicWork(
            PERIODIC_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<MonthlyBackupWorker>(24, TimeUnit.HOURS).build()
        )
        work.enqueueUniqueWork(
            IMMEDIATE_NAME,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<MonthlyBackupWorker>().build()
        )
    }

    fun disable(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_NAME)
        WorkManager.getInstance(context).cancelUniqueWork(IMMEDIATE_NAME)
        preferences(context).edit().remove(FOLDER).putString(STATUS, "Otomatik yedek kapalı.").apply()
    }

    internal fun recordSuccess(context: Context, month: YearMonth, status: String) {
        preferences(context).edit().putString(LAST, month.toString())
            .putString(STATUS, status).apply()
    }

    internal fun recordFailure(context: Context, status: String) {
        preferences(context).edit().putString(STATUS, status).apply()
    }
}

/** WorkManager checks daily; when the calendar month changes it saves the previous month. */
class MonthlyBackupWorker(
    private val appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val folderUri = MonthlyBackupScheduler.folder(appContext) ?: return Result.success()
        val zone = ZoneId.systemDefault()
        val month = YearMonth.now(zone).minusMonths(1)
        if (MonthlyBackupScheduler.lastCompletedMonth(appContext) == month.toString()) return Result.success()
        return try {
            val hasGrant = appContext.contentResolver.persistedUriPermissions.any {
                it.uri == folderUri && it.isWritePermission
            }
            require(hasGrant) { "Klasör yazma izni bulunamadı." }
            val targetFolder = DocumentFile.fromTreeUri(appContext, folderUri)
            require(targetFolder != null && targetFolder.isDirectory && targetFolder.canWrite()) {
                "Klasöre yazılamıyor."
            }
            val filename = "EymenGelir_" + month.year + "_" +
                month.monthValue.toString().padStart(2, '0') + "_otomatik.zip"
            if (targetFolder.findFile(filename) != null) {
                MonthlyBackupScheduler.recordSuccess(appContext, month, "Dosya zaten mevcut: " + filename)
                return Result.success()
            }
            val dao = FinanceDatabase.get(appContext).financeDao()
            val entries = dao.allEntries().filter {
                YearMonth.from(Instant.ofEpochMilli(it.occurredAt).atZone(zone)) == month
            }
            val receipts = entries.filter { it.receiptFileName != null }.associate {
                it.entryUuid to ReceiptStorage.read(appContext, checkNotNull(it.receiptFileName))
            }
            val snapshot = BackupSnapshot(month, entries, dao.allCategories(), receipts)
            val archive = ByteArrayOutputStream()
            MonthlyBackupArchive.write(archive, snapshot, zone)
            val target = targetFolder.createFile("application/zip", filename)
                ?: error("Yedek dosyası oluşturulamadı.")
            try {
                val out = appContext.contentResolver.openOutputStream(target.uri, "wt")
                    ?: error("ZIP dosyası açılamadı.")
                out.use { it.write(archive.toByteArray()) }
            } catch (error: Exception) {
                target.delete()
                throw error
            }
            MonthlyBackupScheduler.recordSuccess(
                appContext, month, filename + " · " + entries.size + " işlem kaydedildi."
            )
            Result.success()
        } catch (error: Exception) {
            MonthlyBackupScheduler.recordFailure(
                appContext,
                "Otomatik ZIP oluşturulamadı: " + (error.message ?: "Bilinmeyen hata")
            )
            if (runAttemptCount < 2) Result.retry() else Result.failure()
        }
    }
}
