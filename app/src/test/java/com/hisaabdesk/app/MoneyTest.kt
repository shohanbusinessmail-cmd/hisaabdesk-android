
package com.hisaabdesk.app

import com.hisaabdesk.app.core.Money
import org.junit.Assert.*
import org.junit.Test

class MoneyTest {
    @Test fun latinParsing() { assertEquals(50000L, Money.parsePoisha("500")); assertEquals(50050L, Money.parsePoisha("500.5")); assertEquals(50055L, Money.parsePoisha("500.55")) }
    @Test fun bengaliParsing() { assertEquals(50000L, Money.parsePoisha("৫০০")); assertEquals(123450L, Money.parsePoisha("১,২৩৪.৫০")) }
    @Test fun takaSymbolAndCommas() { assertEquals(123400L, Money.parsePoisha("৳1,234")) }
    @Test(expected = IllegalArgumentException::class) fun rejectsThreeDecimals() { Money.parsePoisha("10.123") }
    @Test(expected = IllegalArgumentException::class) fun rejectsLetters() { Money.parsePoisha("abc") }
    @Test(expected = IllegalArgumentException::class) fun rejectsEmpty() { Money.parsePoisha("") }
    @Test fun formatting() {
        assertEquals("৳৫০০", Money.formatPoisha(50000))
        assertEquals("৳১,২৩,৪৫৬", Money.formatPoisha(12345600))
        assertTrue(Money.formatPoisha(123450).contains("১,২৩৪"))
        assertEquals("-৳২০০", Money.formatPoisha(-20000))
    }
    @Test fun lakhGrouping() { assertEquals("1,00,000", Money.lakhGrouping("100000")); assertEquals("12,34,567", Money.lakhGrouping("1234567")); assertEquals("999", Money.lakhGrouping("999")) }
    @Test fun roundTrip() { assertEquals(987650L, Money.parsePoisha(Money.formatPoisha(987650).removePrefix("৳"))) }
}
