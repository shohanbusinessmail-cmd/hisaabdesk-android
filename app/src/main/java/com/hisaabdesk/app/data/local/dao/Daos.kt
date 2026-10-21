
package com.hisaabdesk.app.data.local.dao

import androidx.room.*
import com.hisaabdesk.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY archived, name") fun observeAll(): Flow<List<FinancialAccount>>
    @Query("SELECT * FROM accounts WHERE id=:id") suspend fun getById(id: Long): FinancialAccount?
    @Insert suspend fun insert(a: FinancialAccount): Long
    @Update suspend fun update(a: FinancialAccount)
    @Query("SELECT COALESCE(SUM(amountPoisha),0) FROM ledger_entries WHERE accountId=:id") fun observeLedgerSum(id: Long): Flow<Long>
    @Query("SELECT COALESCE(SUM(amountPoisha),0) FROM ledger_entries WHERE accountId=:id") suspend fun ledgerSum(id: Long): Long
    @Query("SELECT * FROM accounts") suspend fun allOnce(): List<FinancialAccount>
    @Query("DELETE FROM accounts") suspend fun deleteAll()
}

@Dao
interface LedgerDao {
    @Insert suspend fun insert(e: LedgerEntry): Long
    @Insert suspend fun insertAll(e: List<LedgerEntry>)
    @Query("SELECT * FROM ledger_entries ORDER BY occurredAt DESC, id DESC LIMIT :limit OFFSET :offset") fun observeRecent(limit: Int, offset: Int): Flow<List<LedgerEntry>>
    @Query("SELECT * FROM ledger_entries WHERE occurredAt BETWEEN :from AND :to ORDER BY occurredAt DESC") fun observeRange(from: Long, to: Long): Flow<List<LedgerEntry>>
    @Query("SELECT COALESCE(SUM(amountPoisha),0) FROM ledger_entries WHERE type='INCOME' AND occurredAt BETWEEN :from AND :to") fun observeIncome(from: Long, to: Long): Flow<Long>
    @Query("SELECT COALESCE(SUM(-amountPoisha),0) FROM ledger_entries WHERE type='EXPENSE' AND occurredAt BETWEEN :from AND :to") fun observeExpense(from: Long, to: Long): Flow<Long>
    @Query("SELECT COALESCE(SUM(-amountPoisha),0) FROM ledger_entries WHERE type='EXPENSE' AND categoryId=:cat AND occurredAt BETWEEN :from AND :to") suspend fun expenseForCategory(cat: Long, from: Long, to: Long): Long
    @Query("SELECT COALESCE(SUM(-amountPoisha),0) FROM ledger_entries WHERE type='EXPENSE' AND occurredAt BETWEEN :from AND :to") suspend fun totalExpense(from: Long, to: Long): Long
    @Query("SELECT * FROM ledger_entries WHERE (description LIKE '%' || :q || '%' OR counterparty LIKE '%' || :q || '%' OR note LIKE '%' || :q || '%') ORDER BY occurredAt DESC LIMIT 200") fun search(q: String): Flow<List<LedgerEntry>>
    @Query("SELECT * FROM ledger_entries") suspend fun allOnce(): List<LedgerEntry>
    @Query("DELETE FROM ledger_entries") suspend fun deleteAll()
    @Query("DELETE FROM ledger_entries WHERE id=:id") suspend fun deleteById(id: Long)
    @Query("SELECT * FROM ledger_entries WHERE accountId=:accountId ORDER BY occurredAt DESC LIMIT 200") fun observeForAccount(accountId: Long): Flow<List<LedgerEntry>>
}

@Dao
interface CatalogDao {
    @Query("SELECT * FROM categories ORDER BY archived, kind, name") fun observeCategories(): Flow<List<TransactionCategory>>
    @Insert suspend fun insertCategory(c: TransactionCategory): Long
    @Update suspend fun updateCategory(c: TransactionCategory)
    @Query("SELECT * FROM subcategories WHERE categoryId=:cat ORDER BY name") fun observeSubcategories(cat: Long): Flow<List<CategorySubcategory>>
    @Insert suspend fun insertSubcategory(s: CategorySubcategory): Long
    @Update suspend fun updateSubcategory(s: CategorySubcategory)
    @Query("SELECT COUNT(*) FROM ledger_entries WHERE categoryId=:cat") suspend fun categoryUsage(cat: Long): Int
    @Query("SELECT * FROM categories") suspend fun categoriesOnce(): List<TransactionCategory>
    @Query("SELECT * FROM subcategories") suspend fun subcategoriesOnce(): List<CategorySubcategory>
    @Query("DELETE FROM categories") suspend fun deleteCategories()
    @Query("DELETE FROM subcategories") suspend fun deleteSubcategories()
}

@Dao
interface ShopDao {
    @Query("SELECT * FROM shops ORDER BY archived, name") fun observeShops(): Flow<List<Shop>>
    @Insert suspend fun insertShop(s: Shop): Long
    @Update suspend fun updateShop(s: Shop)
    @Query("SELECT * FROM shop_purchases WHERE shopId=:shop ORDER BY purchasedAt DESC") fun observePurchases(shop: Long): Flow<List<ShopPurchase>>
    @Insert suspend fun insertPurchase(p: ShopPurchase): Long
    @Insert suspend fun insertItems(items: List<ShopPurchaseItem>)
    @Query("SELECT * FROM shop_purchase_items WHERE purchaseId=:pid") suspend fun items(pid: Long): List<ShopPurchaseItem>
    @Query("SELECT * FROM shop_payments WHERE shopId=:shop ORDER BY paidAt DESC") fun observePayments(shop: Long): Flow<List<ShopPayment>>
    @Insert suspend fun insertPayment(p: ShopPayment): Long
    @Query("SELECT COALESCE(SUM(totalPoisha),0) FROM shop_purchases WHERE shopId=:shop") fun observePurchased(shop: Long): Flow<Long>
    @Query("SELECT COALESCE(SUM(amountPoisha),0) FROM (SELECT amountPoisha FROM shop_payments WHERE shopId=:shop UNION ALL SELECT paidNowPoisha AS amountPoisha FROM shop_purchases WHERE shopId=:shop)") fun observePaid(shop: Long): Flow<Long>
    @Query("SELECT COALESCE(SUM(totalPoisha),0) FROM shop_purchases") fun observeAllPurchased(): Flow<Long>
    @Query("SELECT COALESCE(SUM(x),0) FROM (SELECT (SELECT COALESCE(SUM(totalPoisha),0) FROM shop_purchases WHERE shopId=shops.id) - (SELECT COALESCE(SUM(amountPoisha),0) FROM shop_payments WHERE shopId=shops.id) - (SELECT COALESCE(SUM(paidNowPoisha),0) FROM shop_purchases WHERE shopId=shops.id) AS x FROM shops WHERE archived=0)") fun observeTotalOutstanding(): Flow<Long>
    @Query("SELECT * FROM shops") suspend fun shopsOnce(): List<Shop>
    @Query("SELECT * FROM shop_purchases") suspend fun purchasesOnce(): List<ShopPurchase>
    @Query("SELECT * FROM shop_purchase_items") suspend fun itemsOnce(): List<ShopPurchaseItem>
    @Query("SELECT * FROM shop_payments") suspend fun paymentsOnce(): List<ShopPayment>
    @Query("DELETE FROM shops") suspend fun delShops(); @Query("DELETE FROM shop_purchases") suspend fun delPurchases()
    @Query("DELETE FROM shop_purchase_items") suspend fun delItems(); @Query("DELETE FROM shop_payments") suspend fun delPayments()
}

@Dao
interface DebtDao {
    @Query("SELECT * FROM persons ORDER BY archived, name") fun observePersons(): Flow<List<Person>>
    @Insert suspend fun insertPerson(p: Person): Long
    @Update suspend fun updatePerson(p: Person)
    @Query("SELECT * FROM personal_debts ORDER BY startAt DESC") fun observeDebts(): Flow<List<PersonalDebt>>
    @Insert suspend fun insertDebt(d: PersonalDebt): Long
    @Update suspend fun updateDebt(d: PersonalDebt)
    @Insert suspend fun insertPayment(p: PersonalDebtPayment): Long
    @Query("SELECT * FROM personal_debt_payments WHERE debtId=:debt ORDER BY paidAt DESC") fun observePayments(debt: Long): Flow<List<PersonalDebtPayment>>
    @Query("SELECT COALESCE(SUM(amountPoisha),0) FROM personal_debt_payments WHERE debtId=:debt") suspend fun paidSum(debt: Long): Long
    @Query("SELECT COALESCE(SUM(amountPoisha),0) FROM personal_debt_payments WHERE debtId=:debt") fun observePaidSum(debt: Long): Flow<Long>
    @Query("SELECT * FROM persons") suspend fun personsOnce(): List<Person>
    @Query("SELECT * FROM personal_debts") suspend fun debtsOnce(): List<PersonalDebt>
    @Query("SELECT * FROM personal_debt_payments") suspend fun paymentsOnce(): List<PersonalDebtPayment>
    @Query("DELETE FROM persons") suspend fun delPersons(); @Query("DELETE FROM personal_debts") suspend fun delDebts(); @Query("DELETE FROM personal_debt_payments") suspend fun delPayments()
}

@Dao
interface LoanDao {
    @Query("SELECT * FROM ngo_loans ORDER BY disbursedAt DESC") fun observeNgoLoans(): Flow<List<NgoLoanAccount>>
    @Insert suspend fun insertNgo(l: NgoLoanAccount): Long
    @Update suspend fun updateNgo(l: NgoLoanAccount)
    @Insert suspend fun insertNgoInstallments(i: List<NgoLoanInstallment>)
    @Query("SELECT * FROM ngo_installments WHERE loanId=:loan ORDER BY dueAt") fun observeNgoInstallments(loan: Long): Flow<List<NgoLoanInstallment>>
    @Insert suspend fun insertNgoPayment(p: NgoLoanPayment): Long
    @Query("SELECT * FROM ngo_payments WHERE loanId=:loan ORDER BY paidAt DESC") fun observeNgoPayments(loan: Long): Flow<List<NgoLoanPayment>>
    @Query("SELECT COALESCE(SUM(amountPoisha),0) FROM ngo_payments WHERE loanId=:loan") fun observeNgoPaid(loan: Long): Flow<Long>
    @Query("SELECT COALESCE(SUM(amountPoisha),0) FROM ngo_payments WHERE loanId=:loan") suspend fun ngoPaid(loan: Long): Long
    @Query("SELECT * FROM bank_loans ORDER BY disbursedAt DESC") fun observeBankLoans(): Flow<List<BankLoanAccount>>
    @Insert suspend fun insertBank(l: BankLoanAccount): Long
    @Update suspend fun updateBank(l: BankLoanAccount)
    @Insert suspend fun insertBankSchedule(s: List<BankLoanSchedule>)
    @Query("SELECT * FROM bank_schedule WHERE loanId=:loan ORDER BY dueAt") fun observeBankSchedule(loan: Long): Flow<List<BankLoanSchedule>>
    @Insert suspend fun insertBankPayment(p: BankLoanPayment): Long
    @Query("SELECT * FROM bank_payments WHERE loanId=:loan ORDER BY paidAt DESC") fun observeBankPayments(loan: Long): Flow<List<BankLoanPayment>>
    @Query("SELECT COALESCE(SUM(amountPoisha),0) FROM bank_payments WHERE loanId=:loan") fun observeBankPaid(loan: Long): Flow<Long>
    @Query("SELECT COALESCE(SUM(amountPoisha),0) FROM bank_payments WHERE loanId=:loan") suspend fun bankPaid(loan: Long): Long
    @Query("SELECT * FROM ngo_loans") suspend fun ngoOnce(): List<NgoLoanAccount>
    @Query("SELECT * FROM ngo_installments") suspend fun ngoInstOnce(): List<NgoLoanInstallment>
    @Query("SELECT * FROM ngo_payments") suspend fun ngoPayOnce(): List<NgoLoanPayment>
    @Query("SELECT * FROM bank_loans") suspend fun bankOnce(): List<BankLoanAccount>
    @Query("SELECT * FROM bank_schedule") suspend fun bankSchedOnce(): List<BankLoanSchedule>
    @Query("SELECT * FROM bank_payments") suspend fun bankPayOnce(): List<BankLoanPayment>
    @Query("DELETE FROM ngo_loans") suspend fun delNgo(); @Query("DELETE FROM ngo_installments") suspend fun delNgoInst(); @Query("DELETE FROM ngo_payments") suspend fun delNgoPay()
    @Query("DELETE FROM bank_loans") suspend fun delBank(); @Query("DELETE FROM bank_schedule") suspend fun delBankSched(); @Query("DELETE FROM bank_payments") suspend fun delBankPay()
}

@Dao
interface SavingsBudgetDao {
    @Query("SELECT * FROM savings_accounts ORDER BY archived, name") fun observeSavings(): Flow<List<SavingsAccount>>
    @Insert suspend fun insertSavings(s: SavingsAccount): Long
    @Update suspend fun updateSavings(s: SavingsAccount)
    @Insert suspend fun insertMovement(m: SavingsMovement): Long
    @Query("SELECT * FROM savings_movements WHERE savingsId=:id ORDER BY occurredAt DESC") fun observeMovements(id: Long): Flow<List<SavingsMovement>>
    @Query("SELECT COALESCE(SUM(CASE WHEN direction='DEPOSIT' THEN amountPoisha ELSE -amountPoisha END),0) FROM savings_movements WHERE savingsId=:id") fun observeBalance(id: Long): Flow<Long>
    @Query("SELECT * FROM budgets ORDER BY startAt DESC") fun observeBudgets(): Flow<List<Budget>>
    @Insert suspend fun insertBudget(b: Budget): Long
    @Update suspend fun updateBudget(b: Budget)
    @Query("DELETE FROM budgets WHERE id=:id") suspend fun deleteBudget(id: Long)
    @Query("SELECT * FROM recurring_transactions ORDER BY nextDueAt") fun observeRecurring(): Flow<List<RecurringTransaction>>
    @Insert suspend fun insertRecurring(r: RecurringTransaction): Long
    @Update suspend fun updateRecurring(r: RecurringTransaction)
    @Query("DELETE FROM recurring_transactions WHERE id=:id") suspend fun deleteRecurring(id: Long)
    @Query("SELECT * FROM scheduled_reminders ORDER BY dueAt") fun observeReminders(): Flow<List<ScheduledReminder>>
    @Insert suspend fun insertReminder(r: ScheduledReminder): Long
    @Update suspend fun updateReminder(r: ScheduledReminder)
    @Query("DELETE FROM scheduled_reminders WHERE id=:id") suspend fun deleteReminder(id: Long)
    @Query("SELECT * FROM savings_accounts") suspend fun savingsOnce(): List<SavingsAccount>
    @Query("SELECT * FROM savings_movements") suspend fun movementsOnce(): List<SavingsMovement>
    @Query("SELECT * FROM budgets") suspend fun budgetsOnce(): List<Budget>
    @Query("SELECT * FROM recurring_transactions") suspend fun recurringOnce(): List<RecurringTransaction>
    @Query("SELECT * FROM scheduled_reminders") suspend fun remindersOnce(): List<ScheduledReminder>
    @Query("DELETE FROM savings_accounts") suspend fun delSavings(); @Query("DELETE FROM savings_movements") suspend fun delMovements()
    @Query("DELETE FROM budgets") suspend fun delBudgets(); @Query("DELETE FROM recurring_transactions") suspend fun delRecurring(); @Query("DELETE FROM scheduled_reminders") suspend fun delReminders()
}
