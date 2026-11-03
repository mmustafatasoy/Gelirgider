package com.eymen.gelir

import android.app.Application
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

data class ReleaseStatus(
    val isChecking: Boolean = false,
    val checked: Boolean = false,
    val newVersion: Boolean = false,
    val version: String = "",
    val releaseUrl: String = "",
    val releaseNotes: String = "",
    val message: String = ""
)

class FinanceViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = FinanceDatabase.get(application).financeDao()

    val entries: StateFlow<List<FinanceEntry>> = dao.observeEntries()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val categories: StateFlow<List<FinanceCategory>> = dao.observeCategories()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val releaseStatus = MutableStateFlow(ReleaseStatus())
    val operationStatus = MutableStateFlow<String?>(null)

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
        occurredAt: Long
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (existing == null) {
                    dao.insertEntry(
                        FinanceEntry(
                            entryUuid = UUID.randomUUID().toString(),
                            kind = kind,
                            category = category,
                            amountKurus = amountKurus,
                            description = description.trim(),
                            occurredAt = occurredAt
                        )
                    )
                } else {
                    dao.updateEntry(
                        existing.copy(
                            kind = kind,
                            category = category,
                            amountKurus = amountKurus,
                            description = description.trim(),
                            occurredAt = occurredAt
                        )
                    )
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
