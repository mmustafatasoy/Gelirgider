package com.eymen.gelir.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {
    @Test fun parsesTurkishAmounts() {
        assertEquals(125075L, parseTurkishMoney("1.250,75"))
        assertEquals(50000L, parseTurkishMoney("500"))
        assertEquals(100L, parseTurkishMoney("1,00"))
    }
    @Test fun rejectsInvalidAmounts() {
        assertNull(parseTurkishMoney("0"))
        assertNull(parseTurkishMoney("-10"))
        assertNull(parseTurkishMoney("1.5"))
        assertNull(parseTurkishMoney("123,999"))
        assertNull(parseTurkishMoney("abc"))
    }
}
