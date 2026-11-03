package com.eymen.gelir.backup

import com.eymen.gelir.data.FinanceEntry
import com.eymen.gelir.data.FinanceCategory
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ReceiptBackupTest {
    private val month = YearMonth.of(2026, 11)
    private val zone = ZoneId.of("UTC")
    private val uuid = "ddedb13e-9135-4fb2-9550-9e55ea333333"
    private val photo = byteArrayOf(-1, -40, 1, 2, -1, -39)
    private val date = LocalDate.of(2026, 11, 3).atStartOfDay(zone).toInstant().toEpochMilli()

    private fun snapshot() = BackupSnapshot(
        month,
        listOf(FinanceEntry(
            entryUuid = uuid, kind = "EXPENSE", category = "Market",
            amountKurus = 12345, description = "Market alışverişi", occurredAt = date,
            receiptFileName = uuid + ".jpg"
        )),
        listOf(FinanceCategory(name = "Market", kind = "EXPENSE", emoji = "🛒")),
        mapOf(uuid to photo)
    )

    @Test fun imagesAreRestoredAlongsideTransaction() {
        val bytes = ByteArrayOutputStream()
        MonthlyBackupArchive.write(bytes, snapshot(), zone)
        val result = MonthlyBackupArchive.read(ByteArrayInputStream(bytes.toByteArray()))
        assertEquals(uuid + ".jpg", result.entries.single().receiptFileName)
        assertArrayEquals(photo, result.receipts.getValue(uuid))
    }

    @Test fun missingImageCannotBeExported() {
        var rejected = false
        try {
            MonthlyBackupArchive.write(ByteArrayOutputStream(), snapshot().copy(receipts = emptyMap()), zone)
        } catch (_: IllegalArgumentException) { rejected = true }
        assertTrue(rejected)
    }

    @Test fun oldVersionOneBackupCanStillBeRead() {
        val output = ByteArrayOutputStream()
        MonthlyBackupArchive.write(
            output,
            snapshot().copy(
                entries = snapshot().entries.map { it.copy(receiptFileName = null) },
                receipts = emptyMap()
            ),
            zone
        )
        val files = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(output.toByteArray())).use { zip ->
            while (true) {
                val e = zip.nextEntry ?: break
                files[e.name] = zip.readBytes()
                zip.closeEntry()
            }
        }
        val manifest = JSONObject(files.getValue("manifest.json").toString(Charsets.UTF_8))
        manifest.put("formatVersion", 1)
        manifest.remove("receiptSha256")
        files["manifest.json"] = manifest.toString().toByteArray()
        val legacy = ByteArrayOutputStream()
        ZipOutputStream(legacy).use { zip ->
            files.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        val result = MonthlyBackupArchive.read(ByteArrayInputStream(legacy.toByteArray()))
        assertEquals(1, result.entries.size)
        assertTrue(result.receipts.isEmpty())
    }
}
