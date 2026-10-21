
package com.hisaabdesk.app.data.repository

import androidx.room.withTransaction
import com.hisaabdesk.app.data.local.HisaabDatabase
import com.hisaabdesk.app.data.local.entity.*
import com.hisaabdesk.app.domain.calculator.BankLoanMethod
import com.hisaabdesk.app.domain.calculator.LoanCalculator
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class FinanceRepository(val db: HisaabDatabase) {
    private val accounts = db.accountDao(); private val ledger = db.ledgerDao()
    private val catalog = db.catalogDao(); private val shops = db.shopDao()
    private val debts = db.debtDao(); private val loans = db.loanDao(); private val sb = db.savingsBudgetDao()

    fun observeAccounts() = accounts.observeAll()
    fun observeLedger(limit: Int = 100) = ledger.observeRecent(limit, 0)
    fun observeLedgerRange(from: Long, to: Long) = ledger.observeRange(from, to)
    fun searchLedger(q: String) = ledger.search(q)
    fun observeAccountLedger(id: Long) = ledger.observeForAccount(id)
    fun observeCategories() = catalog.observeCategories()
    fun observeSubcategories(cat: Long) = catalog.observeSubcategories(cat)
    fun observeShops() = shops.observeShops()
    fun observeShopPurchases(id: Long) = shops.observePurchases(id)
    fun observeShopPayments(id: Long) = shops.observePayments(id)
    fun observeShopOutstanding(id: Long): Flow<Long> = shops.observePurchased(id) // combined in VM with paid flow
    fun observeShopPurchased(id: Long) = shops.observePurchased(id)
    fun observeShopPaid(id: Long) = shops.observePaid(id)
    fun observeTotalShopOutstanding() = shops.observeTotalOutstanding()
    fun observePersons() = debts.observePersons()
    fun observeDebts() = debts.observeDebts()
    fun observeDebtPayments(id: Long) = debts.observePayments(id)
    fun observeDebtPaid(id: Long) = debts.observePaidSum(id)
    fun observeNgoLoans() = loans.observeNgoLoans()
    fun observeNgoInstallments(id: Long) = loans.observeNgoInstallments(id)
    fun observeNgoPayments(id: Long) = loans.observeNgoPayments(id)
    fun observeNgoPaid(id: Long) = loans.observeNgoPaid(id)
    fun observeBankLoans() = loans.observeBankLoans()
    fun observeBankSchedule(id: Long) = loans.observeBankSchedule(id)
    fun observeBankPayments(id: Long) = loans.observeBankPayments(id)
    fun observeBankPaid(id: Long) = loans.observeBankPaid(id)
    fun observeSavings() = sb.observeSavings()
    fun observeSavingsMovements(id: Long) = sb.observeMovements(id)
    fun observeSavingsBalance(id: Long) = sb.observeBalance(id)
    fun observeBudgets() = sb.observeBudgets()
    fun observeRecurring() = sb.observeRecurring()
    fun observeReminders() = sb.observeReminders()
    fun observeIncome(from: Long, to: Long) = ledger.observeIncome(from, to)
    fun observeExpense(from: Long, to: Long) = ledger.observeExpense(from, to)
    fun accountBalance(id: Long): Flow<Long> = accounts.observeLedgerSum(id)
    suspend fun accountBalanceOnce(id: Long): Long { val a = accounts.getById(id) ?: return 0; return a.openingBalancePoisha + accounts.ledgerSum(id) }
    suspend fun expenseForCategory(cat: Long?, from: Long, to: Long): Long = if (cat == null) ledger.totalExpense(from, to) else ledger.expenseForCategory(cat, from, to)

    private fun gid() = UUID.randomUUID().toString()
    private suspend fun requireSufficient(accountId: Long, amount: Long, allowOverdraft: Boolean) {
        if (!allowOverdraft) { val bal = accountBalanceOnce(accountId); require(bal >= amount) { "এই অ্যাকাউন্টে পর্যাপ্ত টাকা নেই।" } }
    }

    suspend fun createAccount(a: FinancialAccount): Long = accounts.insert(a)
    suspend fun updateAccount(a: FinancialAccount) = accounts.update(a)

    suspend fun addIncome(accountId: Long, amount: Long, at: Long, categoryId: Long?, source: String, method: String, desc: String) {
        require(amount > 0) { "পরিমাণ শূন্যের বেশি হতে হবে।" }
        ledger.insert(LedgerEntry(groupId = gid(), accountId = accountId, type = "INCOME", amountPoisha = amount, occurredAt = at, categoryId = categoryId, counterparty = source, paymentMethod = method, description = desc))
    }
    suspend fun addExpense(accountId: Long, amount: Long, at: Long, categoryId: Long?, merchant: String, method: String, desc: String, allowOverdraft: Boolean = false) {
        require(amount > 0) { "পরিমাণ শূন্যের বেশি হতে হবে।" }; requireSufficient(accountId, amount, allowOverdraft)
        ledger.insert(LedgerEntry(groupId = gid(), accountId = accountId, type = "EXPENSE", amountPoisha = -amount, occurredAt = at, categoryId = categoryId, counterparty = merchant, paymentMethod = method, description = desc))
    }
    suspend fun transfer(fromId: Long, toId: Long, amount: Long, at: Long, note: String, allowOverdraft: Boolean = false) {
        require(fromId != toId) { "উৎস ও গন্তব্য অ্যাকাউন্ট আলাদা হতে হবে।" }; require(amount > 0) { "পরিমাণ শূন্যের বেশি হতে হবে।" }
        requireSufficient(fromId, amount, allowOverdraft); val g = gid()
        db.withTransaction {
            ledger.insert(LedgerEntry(groupId = g, accountId = fromId, type = "TRANSFER_OUT", amountPoisha = -amount, occurredAt = at, description = "স্থানান্তর", note = note))
            ledger.insert(LedgerEntry(groupId = g, accountId = toId, type = "TRANSFER_IN", amountPoisha = amount, occurredAt = at, description = "স্থানান্তর", note = note))
        }
    }
    /** Reversal: deletes the ledger row and, if it was one side of a transfer group, both sides. History note is kept in description of reversal handling by UI confirmation. */
    suspend fun deleteLedgerEntry(entry: LedgerEntry) {
        db.withTransaction {
            // transfer groups: remove counterpart too to keep both accounts consistent
            val all = ledger.allOnce().filter { it.groupId == entry.groupId && (it.type == "TRANSFER_IN" || it.type == "TRANSFER_OUT") }
            if (all.size > 1) all.forEach { ledger.deleteById(it.id) } else ledger.deleteById(entry.id)
        }
    }

    suspend fun addCategory(c: TransactionCategory) = catalog.insertCategory(c)
    suspend fun updateCategory(c: TransactionCategory) = catalog.updateCategory(c)
    suspend fun addSubcategory(s: CategorySubcategory) = catalog.insertSubcategory(s)
    suspend fun categoryUsage(id: Long) = catalog.categoryUsage(id)

    suspend fun addShop(s: Shop) = shops.insertShop(s)
    suspend fun updateShop(s: Shop) = shops.updateShop(s)
    suspend fun recordShopPurchase(shopId: Long, at: Long, items: List<ShopPurchaseItem>, paidNow: Long, accountId: Long?, categoryId: Long?, notes: String, allowOverdraft: Boolean = false): Long {
        val total = items.sumOf { it.lineTotalPoisha }; require(total > 0) { "পণ্যের মোট মূল্য সঠিক হতে হবে।" }
        require(paidNow in 0..total) { "পরিশোধিত পরিমাণ মোটের বেশি হতে পারে না।" }
        if (paidNow > 0) { requireNotNull(accountId) { "পেমেন্ট অ্যাকাউন্ট নির্বাচন করুন।" }; requireSufficient(accountId, paidNow, allowOverdraft) }
        return db.withTransaction {
            val pid = shops.insertPurchase(ShopPurchase(shopId = shopId, purchasedAt = at, totalPoisha = total, paidNowPoisha = paidNow, paymentAccountId = accountId, categoryId = categoryId, notes = notes))
            shops.insertItems(items.map { it.copy(purchaseId = pid) })
            // Expense recognised ONCE on purchase date (full total). Cash-paid part hits account; credit part does not hit account again later.
            if (paidNow > 0 && accountId != null) {
                ledger.insert(LedgerEntry(groupId = gid(), accountId = accountId, type = "EXPENSE", amountPoisha = -paidNow, occurredAt = at, categoryId = categoryId, description = "দোকান থেকে কেনাকাটা (নগদ অংশ)", refTable = "shop_purchases", refId = pid))
            }
            if (total - paidNow > 0) {
                // credit portion recorded as expense-type reporting marker WITHOUT cash movement is avoided:
                // reports add shop credit purchases separately (see Reports) — here we only store purchase; no ledger row, so no double count.
            }
            pid
        }
    }
    suspend fun shopOutstandingOnce(shopId: Long): Long {
        val purchased = shops.purchasesOnce().filter { it.shopId == shopId }.sumOf { it.totalPoisha }
        val paidNow = shops.purchasesOnce().filter { it.shopId == shopId }.sumOf { it.paidNowPoisha }
        val paid = shops.paymentsOnce().filter { it.shopId == shopId }.sumOf { it.amountPoisha }
        return (purchased - paidNow - paid).coerceAtLeast(0)
    }
    suspend fun payShop(shopId: Long, amount: Long, at: Long, accountId: Long, method: String, ref: String, allowOverdraft: Boolean = false) {
        require(amount > 0) { "পরিমাণ শূন্যের বেশি হতে হবে।" }
        val out = shopOutstandingOnce(shopId); require(amount <= out) { "পেমেন্ট বকেয়ার বেশি হতে পারে না। বকেয়া যাচাই করুন।" }
        requireSufficient(accountId, amount, allowOverdraft)
        db.withTransaction {
            shops.insertPayment(ShopPayment(shopId = shopId, paidAt = at, amountPoisha = amount, accountId = accountId, method = method, reference = ref))
            ledger.insert(LedgerEntry(groupId = gid(), accountId = accountId, type = "SHOP_PAYMENT", amountPoisha = -amount, occurredAt = at, description = "দোকানের বাকি পরিশোধ", refTable = "shops", refId = shopId))
        }
    }

    suspend fun addPerson(p: Person) = debts.insertPerson(p)
    suspend fun updatePerson(p: Person) = debts.updatePerson(p)
    suspend fun createPersonalDebt(personId: Long, direction: String, principal: Long, at: Long, expected: Long?, purpose: String, accountId: Long, notes: String, allowOverdraft: Boolean = false): Long {
        require(principal > 0) { "পরিমাণ শূন্যের বেশি হতে হবে।" }
        if (direction == "LENT") requireSufficient(accountId, principal, allowOverdraft)
        return db.withTransaction {
            val id = debts.insertDebt(PersonalDebt(personId = personId, direction = direction, principalPoisha = principal, startAt = at, expectedRepayAt = expected, purpose = purpose, accountId = accountId, notes = notes))
            val type = if (direction == "BORROWED") "LOAN_RECEIVED" else "LOAN_GIVEN"
            val amt = if (direction == "BORROWED") principal else -principal
            ledger.insert(LedgerEntry(groupId = gid(), accountId = accountId, type = type, amountPoisha = amt, occurredAt = at, description = if (direction == "BORROWED") "ধার গ্রহণ" else "ধার প্রদান", refTable = "personal_debts", refId = id))
            id
        }
    }
    suspend fun repayPersonalDebt(debtId: Long, amount: Long, at: Long, accountId: Long, method: String, allowOverdraft: Boolean = false) {
        val all = debts.debtsOnce(); val debt = all.firstOrNull { it.id == debtId } ?: throw IllegalArgumentException("ধারের রেকর্ড পাওয়া যায়নি।")
        val paid = debts.paidSum(debtId); val outstanding = debt.principalPoisha - paid
        require(amount > 0 && amount <= outstanding) { "পরিশোধের পরিমাণ বকেয়ার বেশি হতে পারে না।" }
        if (debt.direction == "BORROWED") requireSufficient(accountId, amount, allowOverdraft)
        db.withTransaction {
            debts.insertPayment(PersonalDebtPayment(debtId = debtId, paidAt = at, amountPoisha = amount, accountId = accountId, method = method))
            val type = if (debt.direction == "BORROWED") "DEBT_REPAYMENT_OUT" else "DEBT_REPAYMENT_IN"
            val amt = if (debt.direction == "BORROWED") -amount else amount
            ledger.insert(LedgerEntry(groupId = gid(), accountId = accountId, type = type, amountPoisha = amt, occurredAt = at, description = "ধার পরিশোধ", refTable = "personal_debts", refId = debtId))
            if (paid + amount >= debt.principalPoisha) debts.updateDebt(debt.copy(status = "SETTLED"))
        }
    }

    suspend fun createNgoLoan(loan: NgoLoanAccount, generateSchedule: Boolean): Long {
        require(loan.principalPoisha > 0) { "ঋণের মূলধন সঠিক হতে হবে।" }
        return db.withTransaction {
            val id = loans.insertNgo(loan)
            ledger.insert(LedgerEntry(groupId = gid(), accountId = loan.disbursementAccountId, type = "NGO_DISBURSEMENT", amountPoisha = loan.principalPoisha, occurredAt = loan.disbursedAt, description = "এনজিও ঋণ গ্রহণ: ${loan.ngoName}", refTable = "ngo_loans", refId = id))
            if (generateSchedule && loan.installmentCount != null && loan.installmentPoisha != null && loan.firstInstallmentAt != null) {
                val list = (0 until loan.installmentCount).map { i ->
                    NgoLoanInstallment(loanId = id, dueAt = addPeriod(loan.firstInstallmentAt, loan.frequency, i), expectedPoisha = loan.installmentPoisha)
                }; loans.insertNgoInstallments(list)
            }
            id
        }
    }
    suspend fun payNgoInstallment(loanId: Long, installmentId: Long?, amount: Long, at: Long, accountId: Long, allowOverdraft: Boolean = false) {
        require(amount > 0) { "পরিমাণ শূন্যের বেশি হতে হবে।" }; requireSufficient(accountId, amount, allowOverdraft)
        db.withTransaction {
            loans.insertNgoPayment(NgoLoanPayment(loanId = loanId, installmentId = installmentId, paidAt = at, amountPoisha = amount, accountId = accountId))
            ledger.insert(LedgerEntry(groupId = gid(), accountId = accountId, type = "NGO_INSTALLMENT", amountPoisha = -amount, occurredAt = at, description = "এনজিও কিস্তি", refTable = "ngo_loans", refId = loanId))
        }
    }
    suspend fun createBankLoan(loan: BankLoanAccount): Long {
        require(loan.principalPoisha > 0) { "ঋণের মূলধন সঠিক হতে হবে।" }
        return db.withTransaction {
            val id = loans.insertBank(loan)
            ledger.insert(LedgerEntry(groupId = gid(), accountId = loan.disbursementAccountId, type = "BANK_DISBURSEMENT", amountPoisha = loan.principalPoisha, occurredAt = loan.disbursedAt, description = "ব্যাংক ঋণ গ্রহণ: ${loan.bankName}", refTable = "bank_loans", refId = id))
            val months = loan.tenureMonths; val first = loan.disbursedAt
            if (months != null && months > 0) {
                val schedule = when (loan.method) {
                    BankLoanMethod.REDUCING_BALANCE.name -> {
                        val rate = loan.annualRatePercent?.let { BigDecimal(it) } ?: throw IllegalArgumentException("সুদহার আবশ্যক।")
                        val emi = LoanCalculator.reducingEmiPoisha(loan.principalPoisha, rate, months)
                        var opening = loan.principalPoisha
                        (1..months).map { i ->
                            val (interest, principal) = LoanCalculator.reducingSplit(opening, rate, emi)
                            opening = (opening - principal).coerceAtLeast(0)
                            BankLoanSchedule(loanId = id, dueAt = addMonths(first, i), expectedPoisha = emi, interestPoisha = interest, principalPoisha = principal)
                        }
                    }
                    BankLoanMethod.FLAT.name -> {
                        val rate = loan.annualRatePercent?.let { BigDecimal(it) } ?: throw IllegalArgumentException("সুদহার আবশ্যক।")
                        val emi = LoanCalculator.flatEmiPoisha(loan.principalPoisha, rate, months)
                        val totalInterest = LoanCalculator.flatTotalInterestPoisha(loan.principalPoisha, rate, months)
                        val perInterest = totalInterest / months; val perPrincipal = loan.principalPoisha / months
                        (1..months).map { i -> BankLoanSchedule(loanId = id, dueAt = addMonths(first, i), expectedPoisha = emi, interestPoisha = perInterest, principalPoisha = perPrincipal) }
                    }
                    else -> {
                        val emi = loan.installmentPoisha ?: throw IllegalArgumentException("ম্যানুয়াল পদ্ধতিতে কিস্তির পরিমাণ আবশ্যক।")
                        (1..months).map { i -> BankLoanSchedule(loanId = id, dueAt = addMonths(first, i), expectedPoisha = emi) }
                    }
                }
                loans.insertBankSchedule(schedule)
            }
            id
        }
    }
    suspend fun payBankInstallment(loanId: Long, scheduleId: Long?, amount: Long, interest: Long?, principal: Long?, at: Long, accountId: Long, allowOverdraft: Boolean = false) {
        require(amount > 0) { "পরিমাণ শূন্যের বেশি হতে হবে।" }; requireSufficient(accountId, amount, allowOverdraft)
        db.withTransaction {
            loans.insertBankPayment(BankLoanPayment(loanId = loanId, scheduleId = scheduleId, paidAt = at, amountPoisha = amount, interestPoisha = interest, principalPoisha = principal, accountId = accountId))
            ledger.insert(LedgerEntry(groupId = gid(), accountId = accountId, type = "BANK_INSTALLMENT", amountPoisha = -amount, occurredAt = at, description = "ব্যাংক ঋণের কিস্তি", refTable = "bank_loans", refId = loanId))
            if (interest != null && interest > 0) {
                // interest portion is tracked on the payment row; ledger cash already left once above, so no second cash row (no double count)
            }
        }
    }

    suspend fun createSavings(s: SavingsAccount) = sb.insertSavings(s)
    suspend fun updateSavings(s: SavingsAccount) = sb.updateSavings(s)
    suspend fun savingsMovement(savingsId: Long, direction: String, amount: Long, at: Long, accountId: Long, purpose: String, allowOverdraft: Boolean = false) {
        require(amount > 0) { "পরিমাণ শূন্যের বেশি হতে হবে।" }
        if (direction == "DEPOSIT") requireSufficient(accountId, amount, allowOverdraft)
        db.withTransaction {
            sb.insertMovement(SavingsMovement(savingsId = savingsId, direction = direction, amountPoisha = amount, occurredAt = at, accountId = accountId, purpose = purpose))
            val type = if (direction == "DEPOSIT") "SAVINGS_DEPOSIT" else "SAVINGS_WITHDRAW"
            val amt = if (direction == "DEPOSIT") -amount else amount
            ledger.insert(LedgerEntry(groupId = gid(), accountId = accountId, type = type, amountPoisha = amt, occurredAt = at, description = if (direction == "DEPOSIT") "সঞ্চয়ে জমা" else "সঞ্চয় থেকে উত্তোলন", refTable = "savings_accounts", refId = savingsId))
        }
    }
    suspend fun addBudget(b: Budget) = sb.insertBudget(b)
    suspend fun updateBudget(b: Budget) = sb.updateBudget(b)
    suspend fun deleteBudget(id: Long) = sb.deleteBudget(id)
    suspend fun addRecurring(r: RecurringTransaction) = sb.insertRecurring(r)
    suspend fun updateRecurring(r: RecurringTransaction) = sb.updateRecurring(r)
    suspend fun deleteRecurring(id: Long) = sb.deleteRecurring(id)
    suspend fun addReminder(r: ScheduledReminder) = sb.insertReminder(r)
    suspend fun updateReminder(r: ScheduledReminder) = sb.updateReminder(r)
    suspend fun deleteReminder(id: Long) = sb.deleteReminder(id)

    /** Post a due recurring template exactly once per occurrence (stable groupId prevents duplicates). */
    suspend fun postRecurring(r: RecurringTransaction) {
        val group = "rec-${r.id}-${r.nextDueAt}"
        val exists = ledger.allOnce().any { it.groupId == group }
        if (!exists) {
            val amt = if (r.type == "INCOME") r.amountPoisha else -r.amountPoisha
            ledger.insert(LedgerEntry(groupId = group, accountId = r.accountId, type = r.type, amountPoisha = amt, occurredAt = r.nextDueAt, categoryId = r.categoryId, description = r.title))
        }
        val next = when (r.frequency) { "DAILY" -> r.nextDueAt + 86400000L; "WEEKLY" -> r.nextDueAt + 7 * 86400000L; "YEARLY" -> addMonths(r.nextDueAt, 12); else -> addMonths(r.nextDueAt, 1) }
        sb.updateRecurring(r.copy(nextDueAt = next))
    }

    private fun addMonths(epoch: Long, months: Int): Long {
        val z = ZoneId.of("Asia/Dhaka"); val d = java.time.Instant.ofEpochMilli(epoch).atZone(z).toLocalDate().plusMonths(months.toLong())
        return d.atStartOfDay(z).toInstant().toEpochMilli()
    }
    private fun addPeriod(first: Long, frequency: String, index: Int): Long = when (frequency) {
        "WEEKLY" -> first + index * 7L * 86400000L
        "MONTHLY" -> addMonths(first, index)
        else -> first + index * 7L * 86400000L
    }
}
