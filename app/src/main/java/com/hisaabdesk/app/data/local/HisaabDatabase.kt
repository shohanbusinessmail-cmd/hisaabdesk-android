
package com.hisaabdesk.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.hisaabdesk.app.data.local.dao.*
import com.hisaabdesk.app.data.local.entity.*

@Database(
    entities = [FinancialAccount::class, LedgerEntry::class, TransactionCategory::class, CategorySubcategory::class,
        Shop::class, ShopPurchase::class, ShopPurchaseItem::class, ShopPayment::class, Person::class,
        PersonalDebt::class, PersonalDebtPayment::class, NgoLoanAccount::class, NgoLoanInstallment::class, NgoLoanPayment::class,
        BankLoanAccount::class, BankLoanSchedule::class, BankLoanPayment::class, SavingsAccount::class, SavingsMovement::class,
        Budget::class, RecurringTransaction::class, ScheduledReminder::class],
    version = 1, exportSchema = false
)
abstract class HisaabDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun ledgerDao(): LedgerDao
    abstract fun catalogDao(): CatalogDao
    abstract fun shopDao(): ShopDao
    abstract fun debtDao(): DebtDao
    abstract fun loanDao(): LoanDao
    abstract fun savingsBudgetDao(): SavingsBudgetDao

    companion object {
        @Volatile private var INSTANCE: HisaabDatabase? = null
        fun get(context: Context): HisaabDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, HisaabDatabase::class.java, "hisaabdesk.db")
                // No destructive fallback: future schema changes must ship explicit Migrations (see docs/ARCHITECTURE.md).
                .build().also { INSTANCE = it }
        }
    }
}
