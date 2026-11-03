package com.eymen.gelir

import android.app.Application
import android.net.Uri
import androidx.room.withTransaction
import com.eymen.gelir.backup.BackupSnapshot
import com.eymen.gelir.backup.MonthlyBackupArchive
import com.eymen.gelir.backup.ReceiptStorage
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eymen.gelir.data.DEFAULT_CATEGORIES
import com.eymen.gelir.data.FinanceCategory
import com.eymen.gelir.data.FinanceDatabase
import com.eymen.gelir.data.FinanceEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.time.YearMonth
import java.time.ZoneId

data class ReleaseStatus(
    val isChecking: Boolean = false,
    val checked: Boolean = false,
    val newVersion: Boolean = false,
    val version: String = "",
    val releaseUrl: String = "",
    val releaseNotes: String = "",
    val message: String = ""
)

data class PendingBackupImport(val snapshot: BackupSnapshot)

class FinanceViewModel(application: Application) : AndroidViewModel(application) {
    private val database = FinanceDatabase.get(application)
    private val dao = database.financeDao()

    val entries: StateFlow<List<FinanceEntry>> = dao.observeEntries()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val categories: StateFlow<List<FinanceCategory>> = dao.observeCategories()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val releaseStatus = MutableStateFlow(ReleaseStatus())
    val operationStatus = MutableStateFlow<String?>(null)
    val backupBusy = MutableStateFlow(false)
    val pendingImport = MutableStateFlow<PendingBackupImport?>(null)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            // Insert any missing default categories while preserving existing user entries.
            DEFAULT_CATEGORIES.forEach { dao.insertCategory(it) }
        }
    }

    fun addOrUpdate(
        existing: FinanceEntry?,
        kind: String,
        category: String,
        amountKurus: Long,
        description: String,
        occurredAt: Long,
        receiptUri: Uri? = null,
        removeReceipt: Boolean = false
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val uuid = existing?.entryUuid ?: UUID.randomUUID().toString()
                val receiptName = when {
                    receiptUri != null -> ReceiptStorage.saveFromUri(
                        getApplication<Application>(), receiptUri, uuid
                    )
                    removeReceipt -> null
                    else -> existing?.receiptFileName
                }
                if (existing == null) {
                    dao.insertEntry(
                        FinanceEntry(
                            entryUuid = uuid,
                            kind = kind,
                            category = category,
                            amountKurus = amountKurus,
                            description = description.trim(),
                            occurredAt = occurredAt,
                            receiptFileName = receiptName
                        )
                    )
                } else {
                    dao.updateEntry(
                        existing.copy(
                            kind = kind,
                            category = category,
                            amountKurus = amountKurus,
                            description = description.trim(),
                            occurredAt = occurredAt,
                            receiptFileName = receiptName
                        )
                    )
                }
                if (removeReceipt && existing?.receiptFileName != null) {
                    ReceiptStorage.delete(getApplication<Application>(), existing.receiptFileName)
                }
                operationStatus.value = "Kayıt kaydedildi."
            } catch (error: Exception) {
                Log.e("EymenGelir", "Saving entry failed", error)
                operationStatus.value = "Kayıt kaydedilemedi."
            }
        }
    }

    fun delete(entry: FinanceEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dao.deleteEntry(entry)
                ReceiptStorage.delete(getApplication<Application>(), entry.receiptFileName)
                operationStatus.value = "Kayıt silindi."
            } catch (error: Exception) {
                operationStatus.value = "Kayıt silinemedi."
            }
        }
    }

    fun addCategory(name: String, kind: String, emoji: String) {
        val cleaned = name.trim().take(48)
        if (cleaned.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val result = dao.insertCategory(
                FinanceCategory(name = cleaned, kind = kind, emoji = emoji, isDefault = false)
            )
            operationStatus.value = if (result == -1L) "Bu kategori zaten var." else "Yeni kategori eklendi."
        }
    }

    fun deleteCategory(category: FinanceCategory) {
        if (category.isDefault) return
        viewModelScope.launch(Dispatchers.IO) {
            if (entries.value.any { it.category == category.name && it.kind == category.kind }) {
                operationStatus.value = "Bu kategoriye bağlı işlemler var; önce onları düzenleyin."
            } else {
                dao.deleteCategory(category)
                operationStatus.value = "Kategori silindi."
            }
        }
    }


    /** Exports only the chosen month into a user-selected document. No broad storage permissions. */
    fun exportMonth(uri: Uri, month: YearMonth) {
        if (backupBusy.value) return
        backupBusy.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val all = dao.allEntries()
                val zone = ZoneId.systemDefault()
                val monthly = all.filter {
                    YearMonth.from(java.time.Instant.ofEpochMilli(it.occurredAt).atZone(zone)) == month
                }
                val receipts = monthly.filter { it.receiptFileName != null }.associate { entry ->
                    entry.entryUuid to ReceiptStorage.read(
                        getApplication<Application>(),
                        checkNotNull(entry.receiptFileName)
                    )
                }
                val snapshot = BackupSnapshot(month, monthly, dao.allCategories(), receipts)
                val resolver = getApplication<Application>().contentResolver
                val stream = resolver.openOutputStream(uri, "wt")
                    ?: error("Seçilen dosya açılamadı.")
                stream.use { MonthlyBackupArchive.write(it, snapshot, zone) }
                operationStatus.value = "${month} yedeği oluşturuldu (${monthly.size} işlem)."
            } catch (error: Exception) {
                Log.e("EymenGelir", "ZIP export failed", error)
                operationStatus.value = "ZIP oluşturulamadı. Dosya ve depolama izinlerini kontrol edin."
            } finally {
                backupBusy.value = false
            }
        }
    }

    /** Checks the full archive before asking the user for explicit confirmation. */
    fun inspectBackup(uri: Uri) {
        if (backupBusy.value) return
        backupBusy.value = true
        pendingImport.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resolver = getApplication<Application>().contentResolver
                val stream = resolver.openInputStream(uri) ?: error("ZIP açılamadı.")
                val parsed = stream.use { MonthlyBackupArchive.read(it) }
                pendingImport.value = PendingBackupImport(parsed)
            } catch (error: Exception) {
                Log.e("EymenGelir", "ZIP verification failed", error)
                operationStatus.value = "ZIP doğrulanamadı. Dosya bozuk ya da desteklenmiyor."
            } finally {
                backupBusy.value = false
            }
        }
    }

    fun cancelPendingImport() { pendingImport.value = null }

    /** Safe merge: existing data is never deleted or overwritten; duplicate UUIDs are ignored. */
    fun restorePendingBackup() {
        if (backupBusy.value) return
        val snapshot = pendingImport.value?.snapshot ?: return
        pendingImport.value = null
        backupBusy.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var inserted = 0
                val written = mutableListOf<String>()
                try {
                    database.withTransaction {
                        snapshot.categories.forEach { dao.insertCategory(it.copy(id = 0)) }
                        snapshot.entries.forEach { entry ->
                            if (dao.insertEntry(entry.copy(id = 0)) != -1L) {
                                inserted++
                                if (entry.receiptFileName != null) {
                                    val bytes = checkNotNull(snapshot.receipts[entry.entryUuid])
                                    val name = ReceiptStorage.write(
                                        getApplication<Application>(), entry.entryUuid, bytes
                                    )
                                    written.add(name)
                                }
                            }
                        }
                    }
                } catch (failed: Exception) {
                    written.forEach { ReceiptStorage.delete(getApplication<Application>(), it) }
                    throw failed
                }
                operationStatus.value = "${snapshot.month}: ${inserted} yeni işlem geri yüklendi. Mevcut kayıtlar korundu."
            } catch (error: Exception) {
                Log.e("EymenGelir", "ZIP import failed", error)
                operationStatus.value = "Geri yükleme başarısız. Mevcut veriler değiştirilmedi."
            } finally {
                backupBusy.value = false
            }
        }
    }

    fun clearOperationStatus() {
        operationStatus.value = null
    }

    fun checkForUpdates() {
        if (releaseStatus.value.isChecking) return
        viewModelScope.launch {
            releaseStatus.value = ReleaseStatus(isChecking = true, message = "Sürüm kontrol ediliyor...")
            releaseStatus.value = withContext(Dispatchers.IO) { fetchLatestRelease() }
        }
    }

    private fun fetchLatestRelease(): ReleaseStatus {
        var connection: HttpURLConnection? = null
        return try {
            connection = URL("https://api.github.com/repos/mmustafatasoy/Gelirgider/releases/latest")
                .openConnection() as HttpURLConnection
            connection.connectTimeout = 9000
            connection.readTimeout = 9000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "EymenGelir-Android")
            when (connection.responseCode) {
                200 -> {
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)
                    val version = json.optString("tag_name").removePrefix("v")
                    val link = json.optString("html_url")
                    val isNew = compareVersions(version, BuildConfig.VERSION_NAME) > 0
                    val safeLink = if (link.startsWith("https://github.com/mmustafatasoy/Gelirgider/releases/")) link else ""
                    ReleaseStatus(
                        checked = true,
                        newVersion = isNew && safeLink.isNotEmpty(),
                        version = version,
                        releaseUrl = safeLink,
                        releaseNotes = json.optString("body").take(1500),
                        message = if (isNew && safeLink.isNotEmpty()) "Yeni sürüm bulundu: v$version" else "Uygulama güncel."
                    )
                }
                404 -> ReleaseStatus(checked = true, message = "Henüz yayımlanmış bir GitHub sürümü yok.")
                else -> ReleaseStatus(checked = true, message = "Güncelleme denetlenemedi (HTTP ${connection.responseCode}).")
            }
        } catch (error: Exception) {
            Log.w("EymenGelir", "Update check failed", error)
            ReleaseStatus(checked = true, message = "İnternet bağlantısı veya GitHub erişimi kontrol edilemedi.")
        } finally {
            connection?.disconnect()
        }
    }

    private fun compareVersions(latest: String, installed: String): Int {
        val a = latest.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
        val b = installed.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
        for (index in 0 until maxOf(a.size, b.size)) {
            val comparison = (a.getOrElse(index) { 0 }).compareTo(b.getOrElse(index) { 0 })
            if (comparison != 0) return comparison
        }
        return 0
    }
}
