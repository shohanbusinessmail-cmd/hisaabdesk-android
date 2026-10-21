
package com.hisaabdesk.app

import com.hisaabdesk.app.core.Dates
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class DatesTest {
    @Test fun monthRangeBoundaries() {
        val (from, to) = Dates.monthRange(LocalDate.of(2024, 2, 15))
        assertEquals(LocalDate.of(2024, 2, 1), Dates.toLocalDate(from))
        assertEquals(LocalDate.of(2024, 2, 29), Dates.toLocalDate(to)) // leap year, Dhaka boundaries
    }
    @Test fun dayBoundariesInDhaka() {
        val d = LocalDate.of(2026, 1, 1)
        assertEquals(d, Dates.toLocalDate(Dates.startOfDayMillis(d)))
        assertEquals(d, Dates.toLocalDate(Dates.endOfDayMillis(d)))
        assertEquals(d.plusDays(1), Dates.toLocalDate(Dates.endOfDayMillis(d) + 1))
    }
    @Test fun parseAndFormatRoundTrip() {
        val d = Dates.parseDdMmYyyy("05/03/2026"); assertEquals(LocalDate.of(2026, 3, 5), d)
        assertEquals(d, Dates.parseDdMmYyyy(Dates.format(d)))
    }
}
