package com.eymen.gelir.backup

import com.eymen.gelir.data.FinanceCategory
import com.eymen.gelir.data.FinanceEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class MonthlyBackupArchiveTest {
    private val zone = ZoneId.of("UTC")
    private val month = YearMonth.of(2026, 11)
    private val instant = LocalDate.of(2026, 11, 3).atStartOfDay(zone).toInstant().toEpochMilli()
    private val snapshot = BackupSnapshot(month,
        listOf(FinanceEntry(
            entryUuid = "ddedb13e-9135-4fb2-9550-9e55ea333333",
            kind = "EXPENSE", category = "Elektrik",
            amountKurus = 125075, description = "Kasım ayı faturası", occurredAt = instant
        )),
        listOf(FinanceCategory(name = "Elektrik", kind = "EXPENSE", emoji = "⚡"))
    )

    @Test fun roundTripKeepsAmountsDatesAndTurkishText() {
        val bytes = ByteArrayOutputStream()
        MonthlyBackupArchive.write(bytes, snapshot, zone)
        val restored = MonthlyBackupArchive.read(ByteArrayInputStream(bytes.toByteArray()))
        assertEquals(month, restored.month)
        assertEquals(1, restored.entries.size)
        assertEquals("Kasım ayı faturası", restored.entries[0].description)
        assertEquals(125075L, restored.entries[0].amountKurus)
        assertEquals("⚡", restored.categories[0].emoji)
        assertEquals(snapshot.entries[0].entryUuid, restored.entries[0].entryUuid)
    }

    @Test fun emptyMonthIsValid() {
        val bytes = ByteArrayOutputStream()
        MonthlyBackupArchive.write(bytes, BackupSnapshot(month, emptyList(), emptyList()), zone)
        assertTrue(MonthlyBackupArchive.read(ByteArrayInputStream(bytes.toByteArray())).entries.isEmpty())
    }

    @Test fun detectsManifestAndDataHashMismatch() {
        val bytes = ByteArrayOutputStream()
        MonthlyBackupArchive.write(bytes, snapshot, zone)
        val content = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes.toByteArray())).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                content[entry.name] = zip.readBytes()
                zip.closeEntry()
            }
        }
        val originalText = content.getValue("records.json").toString(Charsets.UTF_8)
        assertTrue(originalText.contains("125075"))
        content["records.json"] = originalText.replace("125075", "900000").toByteArray()
        val changed = ByteArrayOutputStream()
        ZipOutputStream(changed).use { zip ->
            content.forEach { (name, data) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(data)
                zip.closeEntry()
            }
        }
        var rejected = false
        try { MonthlyBackupArchive.read(ByteArrayInputStream(changed.toByteArray())) }
        catch (_: IllegalArgumentException) { rejected = true }
        assertTrue(rejected)
    }

    @Test fun rejectsZipTraversalEntry() {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            zip.putNextEntry(ZipEntry("../records.json"))
            zip.write("invalid".toByteArray())
            zip.closeEntry()
        }
        var rejected = false
        try { MonthlyBackupArchive.read(ByteArrayInputStream(bytes.toByteArray())) }
        catch (_: IllegalArgumentException) { rejected = true }
        assertTrue(rejected)
    }

    @Test fun rejectsEntriesFromAnotherMonth() {
        var rejected = false
        try {
            MonthlyBackupArchive.write(ByteArrayOutputStream(), snapshot.copy(month = YearMonth.of(2026, 10)), zone)
        } catch (_: IllegalArgumentException) {
            rejected = true
        }
        assertTrue(rejected)
    }
}
