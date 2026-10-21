
package com.hisaabdesk.app.domain.calculator

import java.math.BigDecimal
import java.math.RoundingMode

enum class BankLoanMethod { REDUCING_BALANCE, FLAT, MANUAL }

object LoanCalculator {
    /** Reducing-balance EMI in poisha. annualRatePercent e.g. "12" means 12%/year. Monthly conversion r=annual/12 (documented). */
    fun reducingEmiPoisha(principalPoisha: Long, annualRatePercent: BigDecimal, months: Int): Long {
        require(principalPoisha > 0 && months > 0) { "মূলধন ও মেয়াদ সঠিক হতে হবে।" }
        if (annualRatePercent.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal(principalPoisha).divide(BigDecimal(months), 0, RoundingMode.HALF_UP).toLong()
        }
        val r = annualRatePercent.divide(BigDecimal(1200), 12, RoundingMode.HALF_UP)
        val factor = BigDecimal.ONE.add(r).pow(months)
        val emi = BigDecimal(principalPoisha).multiply(r).multiply(factor)
            .divide(factor.subtract(BigDecimal.ONE), 6, RoundingMode.HALF_UP)
        return emi.setScale(0, RoundingMode.HALF_UP).toLong()
    }
    /** Flat interest total in poisha: P * rate% * years. years may be fractional via months/12. */
    fun flatTotalInterestPoisha(principalPoisha: Long, annualRatePercent: BigDecimal, months: Int): Long {
        require(principalPoisha > 0 && months > 0) { "মূলধন ও মেয়াদ সঠিক হতে হবে।" }
        val years = BigDecimal(months).divide(BigDecimal(12), 10, RoundingMode.HALF_UP)
        return BigDecimal(principalPoisha).multiply(annualRatePercent).divide(BigDecimal(100), 10, RoundingMode.HALF_UP)
            .multiply(years).setScale(0, RoundingMode.HALF_UP).toLong()
    }
    fun flatEmiPoisha(principalPoisha: Long, annualRatePercent: BigDecimal, months: Int): Long {
        val total = principalPoisha + flatTotalInterestPoisha(principalPoisha, annualRatePercent, months)
        return BigDecimal(total).divide(BigDecimal(months), 0, RoundingMode.HALF_UP).toLong()
    }
    /** Splits one reducing EMI into (interest, principal) for a given opening principal. */
    fun reducingSplit(openingPrincipalPoisha: Long, annualRatePercent: BigDecimal, emiPoisha: Long): Pair<Long, Long> {
        val r = annualRatePercent.divide(BigDecimal(1200), 12, RoundingMode.HALF_UP)
        val interest = BigDecimal(openingPrincipalPoisha).multiply(r).setScale(0, RoundingMode.HALF_UP).toLong()
        val principal = (emiPoisha - interest).coerceAtLeast(0)
        return interest to principal
    }
}

object FinanceCalculator {
    /** NGO remaining: contractual (principal+charges) minus paid; principal-remaining computed separately with principal-first allocation. */
    fun ngoRemainingObligation(principal: Long, serviceCharge: Long, otherCharges: Long, totalPaid: Long): Long =
        (principal + serviceCharge + otherCharges - totalPaid).coerceAtLeast(0)
    fun remainingAfterPayments(total: Long, paid: Long): Long = (total - paid).coerceAtLeast(0)
    fun shopOutstanding(totalPurchased: Long, totalPaid: Long): Long = (totalPurchased - totalPaid).coerceAtLeast(0)
    fun budgetUsedPercent(limit: Long, used: Long): Int = if (limit <= 0) 0 else ((used * 100) / limit).toInt()
    fun netWorth(assets: Long, liabilities: Long): Long = assets - liabilities
    fun installmentStatus(scheduled: Long, paid: Long, dueMillis: Long, nowMillis: Long): String = when {
        paid >= scheduled && scheduled > 0 -> "PAID"
        paid > 0 -> if (dueMillis < nowMillis) "OVERDUE_PARTIAL" else "PARTIAL"
        dueMillis < nowMillis -> "OVERDUE"
        else -> "UNPAID"
    }
}
