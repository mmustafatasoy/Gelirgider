package com.eymen.gelir.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

private val TR = Locale("tr", "TR")

/** User enters Turkish amounts: 1.250,75 or 1250,75. Stores as integer kuruş. */
fun parseTurkishMoney(input: String): Long? {
    val cleaned = input.trim().replace("₺", "").replace(" ", "")
    if (cleaned.isBlank() || !Regex("""\d{1,3}(\.\d{3})*(,\d{1,2})?|\d+(,\d{1,2})?""").matches(cleaned)) return null
    return runCatching {
        val amount = BigDecimal(cleaned.replace(".", "").replace(',', '.'))
        if (amount <= BigDecimal.ZERO) return null
        amount.multiply(BigDecimal(100)).setScale(0, RoundingMode.UNNECESSARY).longValueExact()
    }.getOrNull()
}

fun formatMoney(kurus: Long): String {
    val fmt = NumberFormat.getCurrencyInstance(TR)
    fmt.maximumFractionDigits = 2
    fmt.minimumFractionDigits = 2
    return fmt.format(BigDecimal.valueOf(kurus, 2))
}
