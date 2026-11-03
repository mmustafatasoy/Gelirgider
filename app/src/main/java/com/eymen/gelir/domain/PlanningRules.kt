package com.eymen.gelir.domain

import com.eymen.gelir.data.FinanceEntry
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** UI and reminder workers share these calendar and monthly-budget calculations. */
object BudgetRules {
    fun spentInMonth(
        entries: List<FinanceEntry>,
        category: String,
        month: YearMonth,
        zone: ZoneId = ZoneId.systemDefault()
    ): Long = entries.asSequence()
        .filter { it.kind == "EXPENSE" && it.category == category }
        .filter { YearMonth.from(Instant.ofEpochMilli(it.occurredAt).atZone(zone)) == month }
        .sumOf { it.amountKurus }

    fun ratio(spentKurus: Long, limitKurus: Long): Float =
        if (limitKurus <= 0) 0f else
            (spentKurus.toDouble() / limitKurus.toDouble()).toFloat()

    fun status(spentKurus: Long, limitKurus: Long): String {
        if (limitKurus <= 0) return "Bütçe belirlenmedi"
        val rate = spentKurus.toDouble() / limitKurus.toDouble()
        return when {
            rate >= 1.0 -> "Limit aşıldı / doldu"
            rate >= 0.8 -> "Limite yaklaşıldı"
            else -> "Bütçe uygun"
        }
    }
}

object ReminderRules {
    fun dueDate(month: YearMonth, configuredDay: Int): LocalDate {
        require(configuredDay in 1..31)
        return month.atDay(configuredDay.coerceAtMost(month.lengthOfMonth()))
    }

    /** 1 day before to 2 days after the due date; WorkManager may run later. */
    fun shouldNotify(today: LocalDate, configuredDay: Int, lastNotifiedMonth: String?): Boolean {
        val month = YearMonth.from(today)
        val due = dueDate(month, configuredDay)
        if (lastNotifiedMonth == month.toString()) return false
        return !today.isBefore(due.minusDays(1)) && !today.isAfter(due.plusDays(2))
    }

    fun daysUntil(today: LocalDate, configuredDay: Int): Long =
        java.time.temporal.ChronoUnit.DAYS.between(
            today, dueDate(YearMonth.from(today), configuredDay)
        )
}
