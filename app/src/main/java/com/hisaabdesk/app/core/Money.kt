
package com.hisaabdesk.app.core

/** Money is ALWAYS Long poisha (1 taka = 100 poisha). No Float/Double. */
object Money {
    private val bnDigits = charArrayOf('০','১','২','৩','৪','৫','৬','৭','৮','৯')
    fun toBengaliDigits(input: String): String = buildString {
        for (ch in input) append(if (ch in '0'..'9') bnDigits[ch - '0'] else ch)
    }
    fun toLatinDigits(input: String): String = buildString {
        for (ch in input) {
            val i = bnDigits.indexOf(ch)
            append(if (i >= 0) ('0' + i) else ch)
        }
    }
    /** Parses "১,২৩৪.৫০", "1234.5", "৳500" -> poisha. Throws IllegalArgumentException on invalid/overflow. */
    fun parsePoisha(raw: String): Long {
        var s = toLatinDigits(raw.trim()).replace("৳", "").replace(",", "").replace(" ", "").trim()
        require(s.isNotEmpty()) { "পরিমাণটি সঠিকভাবে লিখুন।" }
        val negative = s.startsWith("-"); if (negative) s = s.substring(1)
        require(s.count { it == '.' } <= 1) { "পরিমাণটি সঠিকভাবে লিখুন।" }
        val parts = s.split(".")
        val takaPart = parts[0].ifEmpty { "0" }
        require(takaPart.all { it.isDigit() }) { "পরিমাণটি সঠিকভাবে লিখুন।" }
        val frac = if (parts.size > 1) parts[1] else ""
        require(frac.all { it.isDigit() } && frac.length <= 2) { "সর্বোচ্চ দুই দশমিক ঘর দেওয়া যাবে।" }
        val taka = takaPart.toLongOrNull() ?: throw IllegalArgumentException("পরিমাণটি অনেক বড়।")
        val poishaPart = frac.padEnd(2, '0').toLong()
        val total = Math.addExact(Math.multiplyExact(taka, 100L), poishaPart)
        return if (negative) -total else total
    }
    fun formatPoisha(poisha: Long, alwaysShowPoisha: Boolean = false): String {
        val neg = poisha < 0; val abs = if (neg) -poisha else poisha
        val taka = abs / 100; val rem = (abs % 100).toInt()
        val grouped = lakhGrouping(taka.toString())
        val base = "৳" + toBengaliDigits(grouped)
        val withSign = if (neg) "-$base" else base
        return if (alwaysShowPoisha || rem != 0) withSign + "." + toBengaliDigits(rem.toString().padStart(2, '0')) else withSign
    }
    /** Lakh/crore grouping: 1234567 -> 12,34,567 */
    fun lakhGrouping(digits: String): String {
        if (digits.length <= 3) return digits
        val head = digits.dropLast(3); val tail = digits.takeLast(3)
        val sb = StringBuilder()
        var i = head.length % 2; if (i > 0) { sb.append(head.substring(0, i)); if (head.length > i) sb.append(',') }
        while (i < head.length) { sb.append(head.substring(i, i + 2)); i += 2; if (i < head.length) sb.append(',') }
        if (sb.isNotEmpty() && sb.last() != ',') sb.append(',')
        return sb.toString() + tail
    }
    fun percent(part: Long, total: Long): Int {
        if (total <= 0L) return 0
        return ((part.coerceAtLeast(0) * 100) / total).toInt().coerceIn(0, 10000)
    }
}
