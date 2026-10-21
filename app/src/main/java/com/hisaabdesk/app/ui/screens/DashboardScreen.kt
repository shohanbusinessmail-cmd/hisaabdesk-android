
package com.hisaabdesk.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hisaabdesk.app.core.Dates
import com.hisaabdesk.app.core.Money
import com.hisaabdesk.app.ui.MainViewModel
import com.hisaabdesk.app.ui.components.EmptyState
import com.hisaabdesk.app.ui.components.MoneyText
import com.hisaabdesk.app.ui.components.SectionCard
import com.hisaabdesk.app.ui.components.typeLabelBn
import kotlinx.coroutines.flow.combine

@Composable
fun DashboardScreen(vm: MainViewModel, nav: NavController) {
    val accounts by vm.accounts.collectAsState()
    val todayIncome by vm.todayIncome.collectAsState()
    val todayExpense by vm.todayExpense.collectAsState()
    val monthIncome by vm.monthIncome.collectAsState()
    val monthExpense by vm.monthExpense.collectAsState()
    val recent by vm.ledgerRecent.collectAsState()
    val debts by vm.debts.collectAsState()
    val shopOut by vm.totalShopOutstanding.collectAsState()
    val savingsList by vm.savings.collectAsState()
    val ngoLoans by vm.ngoLoans.collectAsState()
    val bankLoans by vm.bankLoans.collectAsState()

    // Liquid balance = opening + ledger sum for every active account (collected per account below via key)
    var liquidTotal by remember { mutableStateOf(0L) }
    LaunchedEffect(accounts) {
        var t = 0L; accounts.filter { !it.archived }.forEach { t += vm.repo.accountBalanceOnce(it.id) }
        liquidTotal = t
    }
    var savingsTotal by remember { mutableStateOf(0L) }
    LaunchedEffect(savingsList) {
        var t = 0L
        // openings + movements computed through repository flows snapshot via balance queries is per-account; approximate with openings + repo call loop using savings dao through repo helper below
        savingsList.filter { !it.archived }.forEach { s ->
            // balance = opening + deposits - withdrawals from movements list fetched once
            t += s.openingBalancePoisha
        }
        // movements added via dedicated collection in Savings section; here add movements through repo:
        // (kept simple: savings detail screen shows exact per-account balance; dashboard adds movements via flow below)
        savingsTotal = t
    }
    var savingsMovementsTotal by remember { mutableStateOf(0L) }
    LaunchedEffect(savingsList) {
        // sum all movements across savings accounts using one-shot queries through ledger-independent path:
        // we reuse observeSavingsBalance per account via first() snapshot
        var t = 0L
        savingsList.filter { !it.archived }.forEach { s ->
            t += kotlinx.coroutines.flow.firstOrNull(vm.repo.observeSavingsBalance(s.id)) ?: 0L
        }
        savingsMovementsTotal = t
    }
    val savingsExact = savingsList.filter { !it.archived }.sumOf { it.openingBalancePoisha } + savingsMovementsTotal

    var receivables by remember { mutableStateOf(0L) }
    var payables by remember { mutableStateOf(0L) }
    LaunchedEffect(debts) {
        var rec = 0L; var pay = 0L
        debts.filter { it.status == "ACTIVE" }.forEach { d ->
            val paid = kotlinx.coroutines.flow.firstOrNull(vm.repo.observeDebtPaid(d.id)) ?: 0L
            val out = (d.principalPoisha - paid).coerceAtLeast(0)
            if (d.direction == "LENT") rec += out else pay += out
        }
        receivables = rec; payables = pay
    }
    var ngoLiability by remember { mutableStateOf(0L) }
    var bankLiability by remember { mutableStateOf(0L) }
    LaunchedEffect(ngoLoans) {
        var t = 0L
        ngoLoans.filter { it.status == "ACTIVE" }.forEach { l ->
            val paid = kotlinx.coroutines.flow.firstOrNull(vm.repo.observeNgoPaid(l.id)) ?: 0L
            val total = l.totalPayablePoisha ?: ((l.principalPoisha) + (l.serviceChargePoisha ?: 0) + (l.otherChargesPoisha ?: 0))
            t += (total - paid).coerceAtLeast(0)
        }
        ngoLiability = t
    }
    LaunchedEffect(bankLoans) {
        var t = 0L
        bankLoans.filter { it.status == "ACTIVE" }.forEach { l ->
            val paid = kotlinx.coroutines.flow.firstOrNull(vm.repo.observeBankPaid(l.id)) ?: 0L
            val total = l.contractualTotalPayablePoisha ?: l.principalPoisha
            t += (total - paid).coerceAtLeast(0)
        }
        bankLiability = t
    }
    val totalAssets = liquidTotal + savingsExact + receivables
    val totalLiabilities = shopOut + payables + ngoLiability + bankLiability
    val netWorth = totalAssets - totalLiabilities

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("হিসাবডেস্ক", style = MaterialTheme.typography.headlineSmall); Text("আজ: ${Dates.format(Dates.today())}", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (accounts.isEmpty()) {
            item { SectionCard("শুরু করুন") { EmptyState("এখনও কোনো অ্যাকাউন্ট নেই। কোনো নমুনা তথ্য যোগ করা হয়নি — আপনার আসল হিসাব দিয়ে শুরু করুন।", "অ্যাকাউন্ট তৈরি করুন") { nav.navigate("accounts") } } }
        } else {
            item {
                SectionCard("মোট সম্পদ ও দায়") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("তরল ব্যালেন্স (সব অ্যাকাউন্ট)"); MoneyText(liquidTotal) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("সঞ্চয়"); MoneyText(savingsExact) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("পাওনা (অন্যের কাছে)"); MoneyText(receivables) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("মোট সম্পদ"); MoneyText(totalAssets) }
                    HorizontalDivider(Modifier.padding(vertical = 6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("দোকানের বাকি"); MoneyText(shopOut) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("ব্যক্তিগত দেনা"); MoneyText(payables) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("এনজিও ঋণ বকেয়া"); MoneyText(ngoLiability) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("ব্যাংক ঋণ বকেয়া"); MoneyText(bankLiability) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("মোট দায়"); MoneyText(totalLiabilities) }
                    HorizontalDivider(Modifier.padding(vertical = 6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("নিট সম্পদ (সম্পদ − দায়)", style = MaterialTheme.typography.titleMedium); MoneyText(netWorth) }
                }
            }
            item {
                SectionCard("আজ ও এই মাস") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("আজকের আয়"); MoneyText(todayIncome) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("আজকের ব্যয়"); MoneyText(todayExpense) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("এই মাসের আয়"); MoneyText(monthIncome) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("এই মাসের ব্যয়"); MoneyText(monthExpense) }
                }
            }
            item {
                SectionCard("দ্রুত কাজ") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { nav.navigate("transactions?add=INCOME") }) { Text("আয় যোগ করুন") }
                        Button(onClick = { nav.navigate("transactions?add=EXPENSE") }) { Text("ব্যয় যোগ করুন") }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { nav.navigate("accounts?transfer=1") }) { Text("টাকা স্থানান্তর") }
                        OutlinedButton(onClick = { nav.navigate("savings") }) { Text("সঞ্চয় যোগ করুন") }
                        OutlinedButton(onClick = { nav.navigate("debts") }) { Text("ধার-দেনা") }
                    }
                }
            }
            item { Text("সাম্প্রতিক লেনদেন", style = MaterialTheme.typography.titleMedium) }
            if (recent.isEmpty()) {
                item { EmptyState("এখনও কোনো লেনদেন নেই।", "নতুন লেনদেন") { nav.navigate("transactions?add=INCOME") } }
            } else {
                items(recent.take(10), key = { it.id }) { e ->
                    ListItem(headlineContent = { Text("${typeLabelBn(e.type)} — ${e.description.ifBlank { e.counterparty }}") },
                        supportingContent = { Text(Dates.format(e.occurredAt)) },
                        trailingContent = { MoneyText(e.amountPoisha) })
                }
            }
        }
    }
}
