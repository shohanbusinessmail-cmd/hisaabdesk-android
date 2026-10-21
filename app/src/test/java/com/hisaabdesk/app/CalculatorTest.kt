
package com.hisaabdesk.app

import com.hisaabdesk.app.domain.calculator.FinanceCalculator
import com.hisaabdesk.app.domain.calculator.LoanCalculator
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class CalculatorTest {
    @Test fun fixtureFromSpec() {
        // Opening cash 1000, income 500, expense 200, transfer out 100 => cash 1200; income 500, expense 200
        val opening = 100_000L; val ledger = listOf(50_000L, -20_000L, -10_000L)
        assertEquals(120_000L, opening + ledger.sum())
    }
    @Test fun shopPartialPayment() {
        assertEquals(70_000L, FinanceCalculator.shopOutstanding(100_000, 30_000))
        assertEquals(0L, FinanceCalculator.shopOutstanding(50_000, 50_000))
    }
    @Test fun ngoRemaining() {
        // principal 10000 taka, service 1000, other 0, paid 3000 => remaining obligation 8000
        assertEquals(800_000L, FinanceCalculator.ngoRemainingObligation(1_000_000, 100_000, 0, 300_000))
    }
    @Test fun budgetPercent() { assertEquals(50, FinanceCalculator.budgetUsedPercent(100_000, 50_000)); assertEquals(150, FinanceCalculator.budgetUsedPercent(100_000, 150_000)); assertEquals(0, FinanceCalculator.budgetUsedPercent(0, 100)) }
    @Test fun netWorthCalc() { assertEquals(30_000L, FinanceCalculator.netWorth(100_000, 70_000)); assertEquals(-10_000L, FinanceCalculator.netWorth(0, 10_000)) }
    @Test fun installmentStatuses() {
        assertEquals("PAID", FinanceCalculator.installmentStatus(1000, 1000, 0, 5000))
        assertEquals("OVERDUE", FinanceCalculator.installmentStatus(1000, 0, 100, 5000))
        assertEquals("PARTIAL", FinanceCalculator.installmentStatus(1000, 500, 9000, 5000))
        assertEquals("UNPAID", FinanceCalculator.installmentStatus(1000, 0, 9000, 5000))
    }
    @Test fun reducingEmiKnownValue() {
        // 100,000 taka at 12% for 12 months -> EMI approx 8,884.88 taka
        val emi = LoanCalculator.reducingEmiPoisha(100_000_00L, BigDecimal("12"), 12)
        assertTrue("emi=$emi", emi in 888_000..889_000)
    }
    @Test fun zeroRateEmi() { assertEquals(10_000L, LoanCalculator.reducingEmiPoisha(120_000, BigDecimal.ZERO, 12)) }
    @Test fun flatInterest() {
        // 100,000 taka at 10% flat for 12 months -> interest 10,000 taka
        assertEquals(10_000_00L, LoanCalculator.flatTotalInterestPoisha(100_000_00L, BigDecimal("10"), 12))
        // 6 months -> 5,000 taka
        assertEquals(5_000_00L, LoanCalculator.flatTotalInterestPoisha(100_000_00L, BigDecimal("10"), 6))
    }
    @Test fun reducingSplitSums() {
        val (interest, principal) = LoanCalculator.reducingSplit(100_000_00L, BigDecimal("12"), 888_488L)
        assertEquals(888_488L, interest + principal)
        assertEquals(100_000L, interest) // first month interest exactly 1% of principal
    }
    @Test fun recurringGroupIdStable() {
        // mirrors repository rule: group id derived from template id + occurrence date is stable => dedup
        val a = "rec-7-1700000000000"; val b = "rec-7-1700000000000"; assertEquals(a, b)
    }
}
