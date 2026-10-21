
package com.hisaabdesk.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hisaabdesk.app.HisaabApplication
import com.hisaabdesk.app.core.Dates
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val hisaab = app as HisaabApplication
    val repo = hisaab.repository
    val pinManager = hisaab.pinManager
    val backupManager = hisaab.backupManager

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun clearMessages() { _message.value = null; _error.value = null }

    fun run(label: String = "সংরক্ষণ করা হয়েছে।", block: suspend () -> Unit) {
        viewModelScope.launch {
            try { block(); _message.value = label; _error.value = null }
            catch (e: IllegalArgumentException) { _error.value = e.message ?: "তথ্য সঠিক নয়। আবার চেষ্টা করুন।" }
            catch (e: Exception) { _error.value = "সংরক্ষণ করা যায়নি। আপনার আগের হিসাব অক্ষত রয়েছে। (${e.javaClass.simpleName})" }
        }
    }
    fun runAsync(block: suspend () -> Unit) { viewModelScope.launch { try { block() } catch (e: Exception) { _error.value = e.message ?: "কাজটি সম্পন্ন করা যায়নি।" } } }

    val accounts = repo.observeAccounts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val categories = repo.observeCategories().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val ledgerRecent = repo.observeLedger(100).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val shops = repo.observeShops().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val persons = repo.observePersons().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val debts = repo.observeDebts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val ngoLoans = repo.observeNgoLoans().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val bankLoans = repo.observeBankLoans().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val savings = repo.observeSavings().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val budgets = repo.observeBudgets().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val recurring = repo.observeRecurring().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val reminders = repo.observeReminders().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val totalShopOutstanding = repo.observeTotalShopOutstanding().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    private val todayRange = Dates.today().let { Dates.startOfDayMillis(it) to Dates.endOfDayMillis(it) }
    private val monthRange = Dates.monthRange(Dates.today())
    val todayIncome = repo.observeIncome(todayRange.first, todayRange.second).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    val todayExpense = repo.observeExpense(todayRange.first, todayRange.second).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    val monthIncome = repo.observeIncome(monthRange.first, monthRange.second).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    val monthExpense = repo.observeExpense(monthRange.first, monthRange.second).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    val monthLabel: String = "এই মাস (${Dates.format(Dates.today().withDayOfMonth(1))} থেকে)"

    /** Post due auto-recurring templates once; manual templates are only posted when user taps "পোস্ট করুন". */
    fun postDueAutoRecurring() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            recurring.value.filter { it.active && it.autoPost && it.nextDueAt <= now }.forEach { r ->
                try { repo.postRecurring(r) } catch (_: Exception) { }
            }
        }
    }
}
