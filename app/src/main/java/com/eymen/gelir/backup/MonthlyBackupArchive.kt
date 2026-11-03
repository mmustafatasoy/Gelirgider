package com.eymen.gelir.backup

import com.eymen.gelir.data.FinanceCategory
import com.eymen.gelir.data.FinanceEntry
import com.eymen.gelir.data.BudgetLimit
import com.eymen.gelir.data.PaymentReminder
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class BackupSnapshot(
    val month: YearMonth,
    val entries: List<FinanceEntry>,
    val categories: List<FinanceCategory>,
    val receipts: Map<String, ByteArray> = emptyMap(),
    val budgets: List<BudgetLimit> = emptyList(),
    val reminders: List<PaymentReminder> = emptyList()
)

/**
 * EYMEN GELİR monthly archive versions 1 and 2 (backward compatible).
 * Format: manifest.json (SHA-256 + month + time zone), records.json (entries + categories).
 * Whitelisted entries include optional receipt JPEGs in v2; extraction never writes files.
 */
object MonthlyBackupArchive {
    private const val MAGIC = "eymen-gelir-month"
    private const val FORMAT_VERSION = 4
    private const val MAX_UNCOMPRESSED_BYTES = 20 * 1024 * 1024
    private const val MAX_PHOTOS = 80
    private const val MAX_PLANS = 400
    private const val MAX_ENTRIES = 20000
    private const val MAX_CATEGORIES = 400
    private const val MANIFEST = "manifest.json"
    private const val RECORDS = "records.json"
    private val receiptPattern = Regex("receipts/[0-9a-fA-F-]{36}\\.jpg")

    fun write(output: OutputStream, snapshot: BackupSnapshot, timeZone: ZoneId = ZoneId.systemDefault()) {
        require(snapshot.entries.size <= MAX_ENTRIES && snapshot.categories.size <= MAX_CATEGORIES) {
            "Yedek sınırı aşıldı."
        }
        require(snapshot.entries.all { matchesMonth(it, snapshot.month, timeZone) })
        require(snapshot.receipts.size <= MAX_PHOTOS)
        val photoIds = snapshot.entries.filter { it.receiptFileName != null }.map { it.entryUuid }.toSet()
        require(snapshot.receipts.keys == photoIds) { "Bazı fiş fotoğrafları yedekte yok." }
        require(snapshot.receipts.values.all { it.size in 4..ReceiptStorage.MAX_BYTES && ReceiptStorage.isJpeg(it) }) {
            "Geçersiz fiş JPEG içeriği."
        }
        val items = JSONArray()
        snapshot.entries.forEach { entry ->
            items.put(
                JSONObject()
                    .put("uuid", entry.entryUuid)
                    .put("kind", entry.kind)
                    .put("category", entry.category)
                    .put("amountKurus", entry.amountKurus)
                    .put("description", entry.description)
                    .put("occurredAt", entry.occurredAt)
                    .put("createdAt", entry.createdAt)
                    .put("receiptFileName", entry.receiptFileName ?: JSONObject.NULL)
                    .put("recipientName", entry.recipientName ?: JSONObject.NULL)
            )
        }
        val cats = JSONArray()
        snapshot.categories.forEach { cat ->
            cats.put(
                JSONObject()
                    .put("name", cat.name)
                    .put("kind", cat.kind)
                    .put("emoji", cat.emoji)
                    .put("isDefault", cat.isDefault)
            )
        }
        require(snapshot.budgets.size <= MAX_PLANS && snapshot.reminders.size <= MAX_PLANS)
        require(snapshot.budgets.all {
            it.category.isNotBlank() && it.category.length <= 48 && it.monthlyLimitKurus > 0
        })
        require(snapshot.reminders.all {
            it.category.isNotBlank() && it.category.length <= 48 &&
                it.title.isNotBlank() && it.title.length <= 80 && it.dayOfMonth in 1..31
        })
        val budgetArray = JSONArray()
        snapshot.budgets.forEach {
            budgetArray.put(JSONObject()
                .put("category", it.category)
                .put("monthlyLimitKurus", it.monthlyLimitKurus))
        }
        val reminderArray = JSONArray()
        snapshot.reminders.forEach {
            reminderArray.put(JSONObject()
                .put("category", it.category)
                .put("title", it.title)
                .put("dayOfMonth", it.dayOfMonth)
                .put("enabled", it.enabled))
        }
        val payload = JSONObject().put("entries", items).put("categories", cats)
            .put("budgets", budgetArray).put("reminders", reminderArray)
            .toString().toByteArray(StandardCharsets.UTF_8)
        require(payload.size <= MAX_UNCOMPRESSED_BYTES) { "Yedek çok büyük." }
        val receiptHashes = JSONObject()
        snapshot.receipts.toSortedMap().forEach { (uuid, bytes) ->
            require(snapshot.entries.any { it.entryUuid == uuid && it.receiptFileName == uuid + ".jpg" })
            receiptHashes.put(uuid, sha256(bytes))
        }
        val manifest = JSONObject()
            .put("magic", MAGIC)
            .put("formatVersion", FORMAT_VERSION)
            .put("month", snapshot.month.toString())
            .put("timeZone", timeZone.id)
            .put("entryCount", snapshot.entries.size)
            .put("recordSha256", sha256(payload))
            .put("receiptSha256", receiptHashes)
            .toString().toByteArray(StandardCharsets.UTF_8)
        ZipOutputStream(output).use { zip ->
            writeFile(zip, MANIFEST, manifest)
            writeFile(zip, RECORDS, payload)
            snapshot.receipts.toSortedMap().forEach { (uuid, bytes) ->
                writeFile(zip, "receipts/" + uuid + ".jpg", bytes)
            }
        }
    }

    fun read(input: InputStream): BackupSnapshot {
        val files = LinkedHashMap<String, ByteArray>()
        var totalBytes = 0
        var photoCount = 0
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                require(!entry.isDirectory && (entry.name in setOf(MANIFEST, RECORDS) || receiptPattern.matches(entry.name))) {
                    "Tanımlanmayan ZIP içeriği."
                }
                require(!files.containsKey(entry.name)) { "Tekrarlı ZIP girişi." }
                if (entry.name.startsWith("receipts/")) {
                    photoCount++
                    require(photoCount <= MAX_PHOTOS) { "Çok fazla fiş fotoğrafı." }
                }
                val buffer = ByteArray(8192)
                val dest = ByteArrayOutputStream()
                while (true) {
                    val read = zip.read(buffer)
                    if (read == -1) break
                    totalBytes += read
                    if (entry.name.startsWith("receipts/")) {
                        require(dest.size() + read <= ReceiptStorage.MAX_BYTES) { "Fiş boyutu sınırı aşıldı." }
                    }
                    require(totalBytes <= MAX_UNCOMPRESSED_BYTES) { "ZIP dosyası çok büyük." }
                    dest.write(buffer, 0, read)
                }
                files[entry.name] = dest.toByteArray()
                zip.closeEntry()
            }
        }
        require(files.containsKey(MANIFEST) && files.containsKey(RECORDS)) {
            "Yedek dosyası eksik."
        }
        val manifest = JSONObject(String(files.getValue(MANIFEST), StandardCharsets.UTF_8))
        val archiveVersion = manifest.getInt("formatVersion")
        require(manifest.getString("magic") == MAGIC && archiveVersion in 1..FORMAT_VERSION) {
            "Desteklenmeyen yedek biçimi."
        }
        val month = YearMonth.parse(manifest.getString("month"))
        val zone = ZoneId.of(manifest.getString("timeZone"))
        val recordBytes = files.getValue(RECORDS)
        require(sha256(recordBytes).equals(manifest.getString("recordSha256"), ignoreCase = true)) {
            "ZIP bütünlük doğrulaması başarısız."
        }
        val payload = JSONObject(String(recordBytes, StandardCharsets.UTF_8))
        val entryArray = payload.getJSONArray("entries")
        val categoryArray = payload.getJSONArray("categories")
        require(entryArray.length() <= MAX_ENTRIES && categoryArray.length() <= MAX_CATEGORIES)
        require(entryArray.length() == manifest.getInt("entryCount")) { "Kayıt sayısı uyuşmuyor." }
        val categories = ArrayList<FinanceCategory>(categoryArray.length())
        for (index in 0 until categoryArray.length()) {
            val raw = categoryArray.getJSONObject(index)
            val kind = raw.getString("kind")
            val name = raw.getString("name").trim()
            val emoji = raw.getString("emoji")
            require(kind == "INCOME" || kind == "EXPENSE")
            require(name.isNotEmpty() && name.length <= 48 && emoji.length <= 24)
            categories.add(
                FinanceCategory(
                    name = name,
                    kind = kind,
                    emoji = emoji,
                    isDefault = raw.optBoolean("isDefault", false)
                )
            )
        }
        val entries = ArrayList<FinanceEntry>(entryArray.length())
        val uuids = HashSet<String>()
        for (index in 0 until entryArray.length()) {
            val raw = entryArray.getJSONObject(index)
            val uuid = raw.getString("uuid")
            val kind = raw.getString("kind")
            val category = raw.getString("category").trim()
            val description = raw.getString("description")
            val amountKurus = raw.getLong("amountKurus")
            val occurredAt = raw.getLong("occurredAt")
            val createdAt = raw.getLong("createdAt")
            val receiptName = if (archiveVersion >= 2 && !raw.isNull("receiptFileName")) {
                raw.getString("receiptFileName")
            } else null
            val recipientName = if (archiveVersion >= 4 && !raw.isNull("recipientName")) {
                raw.getString("recipientName").trim()
            } else null
            require(recipientName == null || (recipientName.isNotBlank() && recipientName.length <= 100)) {
                "Para gönderilen kişi adı geçersiz."
            }
            require(receiptName == null || receiptName == uuid + ".jpg") { "Fiş adı uyuşmuyor." }

            require(uuid.matches(Regex("[0-9a-fA-F-]{36}")) && uuids.add(uuid))
            require(kind == "INCOME" || kind == "EXPENSE")
            require(category.isNotEmpty() && category.length <= 48)
            require(description.length <= 500)
            require(amountKurus > 0 && amountKurus <= 100_000_000_000_00L)
            require(occurredAt >= 0 && createdAt >= 0)
            val entry = FinanceEntry(
                entryUuid = uuid,
                kind = kind,
                category = category,
                amountKurus = amountKurus,
                description = description,
                occurredAt = occurredAt,
                createdAt = createdAt,
                receiptFileName = receiptName,
                recipientName = recipientName
            )
            require(matchesMonth(entry, month, zone)) { "Yedekte farklı bir ayın kaydı bulundu." }
            entries.add(entry)
        }
        // A saved entry must have a matching category, even if it is no longer used locally.
        require(entries.all { entry ->
            categories.any { it.kind == entry.kind && it.name == entry.category }
        }) { "Yedekte bazı kategoriler eksik." }
        val expectedReceipts = entries.filter { it.receiptFileName != null }.map { it.entryUuid }.toSet()
        val filesWithImages = files.keys.filter { it.startsWith("receipts/") }.map {
            it.removePrefix("receipts/").removeSuffix(".jpg")
        }.toSet()
        require(expectedReceipts == filesWithImages) { "Fiş fotoğrafları eksik veya fazla." }
        val hashes = if (archiveVersion >= 2) manifest.getJSONObject("receiptSha256") else JSONObject()
        require(hashes.length() == expectedReceipts.size) { "Fiş özetleri uyuşmuyor." }
        val receipts = HashMap<String, ByteArray>()
        expectedReceipts.forEach { uuid ->
            val bytes = files.getValue("receipts/" + uuid + ".jpg")
            require(ReceiptStorage.isJpeg(bytes)) { "Fiş JPEG doğrulaması başarısız." }
            require(sha256(bytes).equals(hashes.getString(uuid), ignoreCase = true)) {
                "Fiş bütünlük doğrulaması başarısız."
            }
            receipts[uuid] = bytes
        }
        val budgets = mutableListOf<BudgetLimit>()
        val reminders = mutableListOf<PaymentReminder>()
        if (archiveVersion >= 3) {
            val budgetArray = payload.getJSONArray("budgets")
            val reminderArray = payload.getJSONArray("reminders")
            require(budgetArray.length() <= MAX_PLANS && reminderArray.length() <= MAX_PLANS)
            val budgetNames = HashSet<String>()
            for (i in 0 until budgetArray.length()) {
                val raw = budgetArray.getJSONObject(i)
                val category = raw.getString("category")
                val amount = raw.getLong("monthlyLimitKurus")
                require(category.isNotBlank() && category.length <= 48 && budgetNames.add(category))
                require(amount in 1..100_000_000_000_00L)
                budgets.add(BudgetLimit(category, amount))
            }
            val reminderKeys = HashSet<String>()
            for (i in 0 until reminderArray.length()) {
                val raw = reminderArray.getJSONObject(i)
                val category = raw.getString("category")
                val title = raw.getString("title")
                val day = raw.getInt("dayOfMonth")
                val key = "$category|$title|$day"
                require(category.isNotBlank() && category.length <= 48)
                require(title.isNotBlank() && title.length <= 80)
                require(day in 1..31 && reminderKeys.add(key))
                reminders.add(PaymentReminder(
                    category = category,
                    title = title,
                    dayOfMonth = day,
                    enabled = raw.getBoolean("enabled")
                ))
            }
        }
        return BackupSnapshot(month, entries, categories, receipts, budgets, reminders)
    }

    private fun matchesMonth(entry: FinanceEntry, month: YearMonth, zone: ZoneId): Boolean =
        YearMonth.from(Instant.ofEpochMilli(entry.occurredAt).atZone(zone)) == month

    private fun writeFile(zip: ZipOutputStream, name: String, payload: ByteArray) {
        zip.putNextEntry(ZipEntry(name).also { it.time = 0L })
        zip.write(payload)
        zip.closeEntry()
    }

    private fun sha256(content: ByteArray): String = MessageDigest
        .getInstance("SHA-256").digest(content)
        .joinToString("") { "%02x".format(it) }
}
