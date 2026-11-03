package com.eymen.gelir.backup

import com.eymen.gelir.data.FinanceCategory
import com.eymen.gelir.data.FinanceEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID

class TransferBackupTest {
    private val zone = ZoneId.of("UTC")
    private val month = YearMonth.of(2026, 11)
    private val day = LocalDate.of(2026, 11, 3).atStartOfDay(zone).toInstant().toEpochMilli()

    @Test fun personAndTransferDetailsSurviveZipRoundTrip() {
        val uuid = UUID.randomUUID().toString()
        val entry = FinanceEntry(
            entryUuid = uuid,
            kind = "EXPENSE",
            category = "Gönderilen Para",
            amountKurus = 145025,
            description = "Kira desteği",
            occurredAt = day,
            recipientName = "Ahmet Yılmaz"
        )
        val snapshot = BackupSnapshot(month,
            listOf(entry),
            listOf(FinanceCategory(name = "Gönderilen Para", kind = "EXPENSE", emoji = "💸"))
        )
        val bytes = ByteArrayOutputStream()
        MonthlyBackupArchive.write(bytes, snapshot, zone)
        val restored = MonthlyBackupArchive.read(ByteArrayInputStream(bytes.toByteArray()))
        assertEquals("Ahmet Yılmaz", restored.entries.single().recipientName)
        assertEquals(145025L, restored.entries.single().amountKurus)
        assertEquals("Kira desteği", restored.entries.single().description)
    }

    @Test fun olderEntriesWithoutRecipientStillWork() {
        val item = FinanceEntry(
            entryUuid = UUID.randomUUID().toString(), kind = "EXPENSE",
            category = "Kira", amountKurus = 650000,
            description = "", occurredAt = day
        )
        val data = ByteArrayOutputStream()
        MonthlyBackupArchive.write(data,
            BackupSnapshot(month, listOf(item),
                listOf(FinanceCategory(name = "Kira", kind = "EXPENSE", emoji = "🏠"))),
            zone)
        val restored = MonthlyBackupArchive.read(ByteArrayInputStream(data.toByteArray()))
        assertNull(restored.entries.single().recipientName)
    }
}
