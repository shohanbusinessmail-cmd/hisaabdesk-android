
package com.hisaabdesk.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "accounts", indices = [Index("name")])
data class FinancialAccount(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String, val type: String, // CASH, BANK, MFS, SAVINGS, OTHER
    val institution: String = "", val identifier: String = "",
    val openingBalancePoisha: Long = 0, val createdAt: Long = System.currentTimeMillis(),
    val notes: String = "", val archived: Boolean = false
)

@Entity(tableName = "ledger_entries",
    foreignKeys = [ForeignKey(entity = FinancialAccount::class, parentColumns = ["id"], childColumns = ["accountId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("accountId"), Index("occurredAt"), Index("type"), Index("groupId"), Index("categoryId")])
data class LedgerEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: String, val accountId: Long,
    val type: String, // INCOME EXPENSE TRANSFER_IN TRANSFER_OUT FEE_INTEREST etc (see FINANCIAL_RULES)
    val amountPoisha: Long, // signed relative to accountId
    val occurredAt: Long, val categoryId: Long? = null, val subcategoryId: Long? = null,
    val counterparty: String = "", val description: String = "", val paymentMethod: String = "",
    val refTable: String = "", val refId: Long? = null, val note: String = ""
)

@Entity(tableName = "categories", indices = [Index("name")])
data class TransactionCategory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String, val kind: String, // INCOME or EXPENSE
    val colorHex: String = "#16834A", val iconName: String = "label", val archived: Boolean = false
)

@Entity(tableName = "subcategories",
    foreignKeys = [ForeignKey(entity = TransactionCategory::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("categoryId")])
data class CategorySubcategory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long, val name: String, val archived: Boolean = false
)

@Entity(tableName = "shops", indices = [Index("name")])
data class Shop(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String, val keeperName: String = "", val phone: String = "", val address: String = "",
    val identifier: String = "", val notes: String = "", val archived: Boolean = false
)

@Entity(tableName = "shop_purchases",
    foreignKeys = [ForeignKey(entity = Shop::class, parentColumns = ["id"], childColumns = ["shopId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("shopId"), Index("purchasedAt")])
data class ShopPurchase(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shopId: Long, val purchasedAt: Long, val totalPoisha: Long, val paidNowPoisha: Long,
    val paymentAccountId: Long? = null, val categoryId: Long? = null, val notes: String = ""
)

@Entity(tableName = "shop_purchase_items",
    foreignKeys = [ForeignKey(entity = ShopPurchase::class, parentColumns = ["id"], childColumns = ["purchaseId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("purchaseId")])
data class ShopPurchaseItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val purchaseId: Long, val productName: String, val quantityText: String,
    val unitPricePoisha: Long, val lineTotalPoisha: Long
)

@Entity(tableName = "shop_payments",
    foreignKeys = [ForeignKey(entity = Shop::class, parentColumns = ["id"], childColumns = ["shopId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("shopId"), Index("paidAt")])
data class ShopPayment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shopId: Long, val paidAt: Long, val amountPoisha: Long, val accountId: Long,
    val method: String = "", val reference: String = "", val notes: String = ""
)

@Entity(tableName = "persons", indices = [Index("name")])
data class Person(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String, val phone: String = "", val address: String = "", val notes: String = "", val archived: Boolean = false
)

@Entity(tableName = "personal_debts",
    foreignKeys = [ForeignKey(entity = Person::class, parentColumns = ["id"], childColumns = ["personId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("personId"), Index("expectedRepayAt")])
data class PersonalDebt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long, val direction: String, // BORROWED (user owes) or LENT (user is owed)
    val principalPoisha: Long, val startAt: Long, val expectedRepayAt: Long? = null,
    val purpose: String = "", val accountId: Long, val notes: String = "", val status: String = "ACTIVE"
)

@Entity(tableName = "personal_debt_payments",
    foreignKeys = [ForeignKey(entity = PersonalDebt::class, parentColumns = ["id"], childColumns = ["debtId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("debtId"), Index("paidAt")])
data class PersonalDebtPayment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val debtId: Long, val paidAt: Long, val amountPoisha: Long, val accountId: Long, val method: String = "", val notes: String = ""
)

@Entity(tableName = "ngo_loans", indices = [Index("ngoName")])
data class NgoLoanAccount(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ngoName: String, val loanIdentifier: String = "", val principalPoisha: Long,
    val disbursedAt: Long, val serviceChargePoisha: Long? = null, val otherChargesPoisha: Long? = null,
    val totalPayablePoisha: Long? = null, // user-supplied contractual total; never invented
    val frequency: String = "WEEKLY", val installmentPoisha: Long? = null, val installmentCount: Int? = null,
    val firstInstallmentAt: Long? = null, val disbursementAccountId: Long, val notes: String = "", val status: String = "ACTIVE"
)

@Entity(tableName = "ngo_installments",
    foreignKeys = [ForeignKey(entity = NgoLoanAccount::class, parentColumns = ["id"], childColumns = ["loanId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("loanId"), Index("dueAt")])
data class NgoLoanInstallment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loanId: Long, val dueAt: Long, val expectedPoisha: Long
)

@Entity(tableName = "ngo_payments",
    foreignKeys = [ForeignKey(entity = NgoLoanAccount::class, parentColumns = ["id"], childColumns = ["loanId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("loanId"), Index("paidAt")])
data class NgoLoanPayment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loanId: Long, val installmentId: Long? = null, val paidAt: Long, val amountPoisha: Long, val accountId: Long, val notes: String = ""
)

@Entity(tableName = "bank_loans", indices = [Index("bankName")])
data class BankLoanAccount(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bankName: String, val nickname: String = "", val maskedIdentifier: String = "",
    val principalPoisha: Long, val disbursedAt: Long, val annualRatePercent: String? = null, // BigDecimal string, user-supplied
    val method: String = "MANUAL", // REDUCING_BALANCE, FLAT, MANUAL
    val tenureMonths: Int? = null, val frequency: String = "MONTHLY", val installmentPoisha: Long? = null,
    val processingFeePoisha: Long = 0, val otherChargesPoisha: Long = 0, val contractualTotalPayablePoisha: Long? = null,
    val disbursementAccountId: Long, val notes: String = "", val status: String = "ACTIVE"
)

@Entity(tableName = "bank_schedule",
    foreignKeys = [ForeignKey(entity = BankLoanAccount::class, parentColumns = ["id"], childColumns = ["loanId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("loanId"), Index("dueAt")])
data class BankLoanSchedule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loanId: Long, val dueAt: Long, val expectedPoisha: Long,
    val interestPoisha: Long? = null, val principalPoisha: Long? = null // null when not determinable (MANUAL)
)

@Entity(tableName = "bank_payments",
    foreignKeys = [ForeignKey(entity = BankLoanAccount::class, parentColumns = ["id"], childColumns = ["loanId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("loanId"), Index("paidAt")])
data class BankLoanPayment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loanId: Long, val scheduleId: Long? = null, val paidAt: Long, val amountPoisha: Long,
    val interestPoisha: Long? = null, val principalPoisha: Long? = null, val accountId: Long, val notes: String = ""
)

@Entity(tableName = "savings_accounts", indices = [Index("name")])
data class SavingsAccount(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String, val institution: String = "", val purpose: String = "",
    val openingBalancePoisha: Long = 0, val createdAt: Long = System.currentTimeMillis(), val notes: String = "", val archived: Boolean = false
)

@Entity(tableName = "savings_movements",
    foreignKeys = [ForeignKey(entity = SavingsAccount::class, parentColumns = ["id"], childColumns = ["savingsId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("savingsId"), Index("occurredAt")])
data class SavingsMovement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val savingsId: Long, val direction: String, // DEPOSIT or WITHDRAW
    val amountPoisha: Long, val occurredAt: Long, val accountId: Long, val purpose: String = "", val method: String = "", val notes: String = ""
)

@Entity(tableName = "budgets", indices = [Index("startAt")])
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String, val startAt: Long, val endAt: Long, val limitPoisha: Long,
    val categoryId: Long? = null, val subcategoryId: Long? = null, val notes: String = ""
)

@Entity(tableName = "recurring_transactions", indices = [Index("nextDueAt")])
data class RecurringTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String, val type: String, val amountPoisha: Long, val accountId: Long, val categoryId: Long? = null,
    val frequency: String, // DAILY WEEKLY MONTHLY YEARLY
    val nextDueAt: Long, val autoPost: Boolean = false, val active: Boolean = true, val notes: String = ""
)

@Entity(tableName = "scheduled_reminders", indices = [Index("dueAt")])
data class ScheduledReminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String, val kind: String, val refId: Long? = null, val dueAt: Long,
    val enabled: Boolean = true, val notes: String = ""
)
