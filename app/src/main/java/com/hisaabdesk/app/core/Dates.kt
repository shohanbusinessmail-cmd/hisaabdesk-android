
package com.hisaabdesk.app.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Dates {
    val DHAKA: ZoneId = ZoneId.of("Asia/Dhaka")
    fun today(): LocalDate = LocalDate.now(DHAKA)
    fun startOfDayMillis(date: LocalDate): Long = date.atStartOfDay(DHAKA).toInstant().toEpochMilli()
    fun endOfDayMillis(date: LocalDate): Long = date.plusDays(1).atStartOfDay(DHAKA).toInstant().toEpochMilli() - 1
    fun monthRange(date: LocalDate): Pair<Long, Long> {
        val first = date.withDayOfMonth(1); val next = first.plusMonths(1)
        return startOfDayMillis(first) to (startOfDayMillis(next) - 1)
    }
    fun toLocalDate(epochMillis: Long): LocalDate = Instant.ofEpochMilli(epochMillis).atZone(DHAKA).toLocalDate()
    private val fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH)
    fun format(epochMillis: Long): String = Money.toBengaliDigits(fmt.format(toLocalDate(epochMillis)))
    fun format(date: LocalDate): String = Money.toBengaliDigits(fmt.format(date))
    fun parseDdMmYyyy(s: String): LocalDate = LocalDate.parse(Money.toLatinDigits(s.trim()), fmt)
}
