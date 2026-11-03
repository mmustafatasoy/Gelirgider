package com.eymen.gelir.domain

import com.eymen.gelir.data.FinanceEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class PlanningRulesTest {
    private val utc = ZoneId.of("UTC")

    @Test fun FebruaryRemindersClampDay31() {
        assertEquals(LocalDate.of(2028, 2, 29), ReminderRules.dueDate(YearMonth.of(2028, 2), 31))
        assertEquals(LocalDate.of(2027, 2, 28), ReminderRules.dueDate(YearMonth.of(2027, 2), 31))
    }

    @Test fun noDuplicateNotificationWithinMonth() {
        val day = LocalDate.of(2026, 11, 20)
        assertTrue(ReminderRules.shouldNotify(day, 20, null))
        assertFalse(ReminderRules.shouldNotify(day, 20, "2026-11"))
        assertFalse(ReminderRules.shouldNotify(day, 25, null))
        assertTrue(ReminderRules.shouldNotify(day, 21, "2026-10"))
    }

    @Test fun monthlySpentFiltersIncomeOtherCategoriesAndMonths() {
        val month = YearMonth.of(2026, 11)
        fun entry(category: String, kind: String, date: LocalDate, kuruş: Long) = FinanceEntry(
            entryUuid = java.util.UUID.randomUUID().toString(),
            category = category, kind = kind,
            occurredAt = date.atStartOfDay(utc).toInstant().toEpochMilli(),
            amountKurus = kuruş, description = ""
        )
        val entries = listOf(
            entry("Market", "EXPENSE", LocalDate.of(2026, 11, 3), 15000),
            entry("Market", "EXPENSE", LocalDate.of(2026, 10, 3), 13000),
            entry("Market", "INCOME", LocalDate.of(2026, 11, 5), 40000),
            entry("Elektrik", "EXPENSE", LocalDate.of(2026, 11, 7), 23000)
        )
        assertEquals(15000L, BudgetRules.spentInMonth(entries, "Market", month, utc))
        assertEquals("Limite yaklaşıldı", BudgetRules.status(8000, 10000))
        assertEquals("Limit aşıldı / doldu", BudgetRules.status(10000, 10000))
    }
}
