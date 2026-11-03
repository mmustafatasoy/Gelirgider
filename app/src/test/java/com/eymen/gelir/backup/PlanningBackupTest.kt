package com.eymen.gelir.backup

import com.eymen.gelir.data.BudgetLimit
import com.eymen.gelir.data.PaymentReminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.YearMonth
import java.time.ZoneId
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import org.json.JSONObject

class PlanningBackupTest {
    private val month = YearMonth.of(2026, 11)
    private val zone = ZoneId.of("UTC")
    private val snapshot = BackupSnapshot(
        month = month,
        entries = emptyList(),
        categories = emptyList(),
        budgets = listOf(BudgetLimit(category = "Market", monthlyLimitKurus = 500000)),
        reminders = listOf(PaymentReminder(category = "Elektrik", title = "Ev faturası", dayOfMonth = 20))
    )

    @Test fun roundTripPreservesLimitsAndReminderDates() {
        val out = ByteArrayOutputStream()
        MonthlyBackupArchive.write(out, snapshot, zone)
        val restored = MonthlyBackupArchive.read(ByteArrayInputStream(out.toByteArray()))
        assertEquals(500000L, restored.budgets.single().monthlyLimitKurus)
        assertEquals("Elektrik", restored.reminders.single().category)
        assertEquals(20, restored.reminders.single().dayOfMonth)
    }

    @Test fun olderVersionTwoArchiveWithoutPlansLoadsAsEmpty() {
        val out = ByteArrayOutputStream()
        MonthlyBackupArchive.write(out,
            BackupSnapshot(month, emptyList(), emptyList()), zone)
        val parts = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(out.toByteArray())).use { zip ->
            while (true) {
                val item = zip.nextEntry ?: break
                parts[item.name] = zip.readBytes()
                zip.closeEntry()
            }
        }
        val manifest = JSONObject(parts.getValue("manifest.json").toString(Charsets.UTF_8))
        manifest.put("formatVersion", 2)
        parts["manifest.json"] = manifest.toString().toByteArray()
        val legacy = ByteArrayOutputStream()
        ZipOutputStream(legacy).use { zip ->
            parts.forEach { (name, data) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(data)
                zip.closeEntry()
            }
        }
        val restored = MonthlyBackupArchive.read(ByteArrayInputStream(legacy.toByteArray()))
        assertTrue(restored.budgets.isEmpty())
        assertTrue(restored.reminders.isEmpty())
    }
}
