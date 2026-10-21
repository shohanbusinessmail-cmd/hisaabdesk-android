
package com.hisaabdesk.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hisaabdesk.app.core.Dates
import com.hisaabdesk.app.core.Money
import com.hisaabdesk.app.data.local.entity.*
import com.hisaabdesk.app.ui.MainViewModel
import com.hisaabdesk.app.ui.components.*

@Composable
fun LoansScreen(vm: MainViewModel) {
    var tab by remember { mutableStateOf(0) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("ঋণ ব্যবস্থাপনা", style = MaterialTheme.typography.headlineSmall)
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }) { Text("এনজিও ঋণ", Modifier.padding(12.dp)) }
            Tab(selected = tab == 1, onClick = { tab = 1 }) { Text("ব্যাংক ঋণ", Modifier.padding(12.dp)) }
        }
        if (tab == 0) NgoSection(vm) else BankSection(vm)
    }
}

@Composable
private fun NgoSection(vm: MainViewModel) {
    val loans by vm.ngoLoans.collectAsState()
    val accounts by vm.accounts.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var payFor by remember { mutableStateOf<NgoLoanAccount?>(null) }
    Column {
        Spacer(Modifier.height(8.dp))
        Button(onClick = { showAdd = true }) { Text("নতুন এনজিও ঋণ") }
        Text("সার্ভিস চার্জ বা কিস্তির পরিমাণ অ্যাপ নিজে থেকে অনুমান করে না — চুক্তিতে যা আছে শুধু তা-ই লিখুন। তথ্য না দিলে মোট পরিশোধযোগ্য ‘জানা নেই’ দেখানো হবে।", style = MaterialTheme.typography.bodySmall)
        if (loans.isEmpty()) EmptyState("কোনো এনজিও ঋণ নেই।", "এনজিও ঋণ যোগ করুন") { showAdd = true }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(loans, key = { it.id }) { l ->
                val paidFlow = remember(l.id) { vm.repo.observeNgoPaid(l.id) }
                val paid by paidFlow.collectAsState(initial = 0L)
                val totalKnown = l.totalPayablePoisha ?: if (l.serviceChargePoisha != null) l.principalPoisha + l.serviceChargePoisha + (l.otherChargesPoisha ?: 0) else null
                val remaining = totalKnown?.let { (it - paid).coerceAtLeast(0) }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${l.ngoName}${if (l.loanIdentifier.isNotBlank()) " (${l.loanIdentifier})" else ""}", style = MaterialTheme.typography.titleMedium)
                        Text("মূলধন ${Money.formatPoisha(l.principalPoisha)} • পরিশোধ ${Money.formatPoisha(paid)}")
                        Text(if (remaining != null) "বকেয়া (চুক্তিভিত্তিক): ${Money.formatPoisha(remaining)}" else "মোট পরিশোধযোগ্য: জানা নেই (সার্ভিস চার্জ দেওয়া হয়নি)")
                        l.installmentPoisha?.let { Text("কিস্তি: ${Money.formatPoisha(it)} • ${l.frequency}") }
                        TextButton(onClick = { payFor = l }) { Text("কিস্তি পরিশোধ করুন") }
                    }
                }
            }
        }
    }
    if (showAdd) {
        var name by remember { mutableStateOf("") }; var principal by remember { mutableStateOf("") }
        var service by remember { mutableStateOf("") }; var other by remember { mutableStateOf("") }
        var totalPayable by remember { mutableStateOf("") }
        var instAmount by remember { mutableStateOf("") }; var instCount by remember { mutableStateOf("") }
        var freq by remember { mutableStateOf("WEEKLY") }; var accountId by remember { mutableStateOf<Long?>(accounts.firstOrNull()?.id) }; var err by remember { mutableStateOf<String?>(null) }
        AlertDialog(onDismissRequest = { showAdd = false }, title = { Text("নতুন এনজিও ঋণ") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField("এনজিওর নাম *", name, { name = it })
                LabeledField("ঋণের মূলধন (টাকা) *", principal, { principal = it }, keyboard = KeyboardType.Decimal)
                LabeledField("সার্ভিস চার্জ (টাকা, চুক্তি অনুযায়ী)", service, { service = it }, keyboard = KeyboardType.Decimal)
                LabeledField("অন্যান্য চার্জ (টাকা)", other, { other = it }, keyboard = KeyboardType.Decimal)
                LabeledField("মোট পরিশোধযোগ্য (টাকা, চুক্তিতে থাকলে)", totalPayable, { totalPayable = it }, keyboard = KeyboardType.Decimal)
                SimplePicker("কিস্তির ধরন", listOf("WEEKLY" to "সাপ্তাহিক", "MONTHLY" to "মাসিক"), freq) { freq = it }
                LabeledField("প্রতি কিস্তির পরিমাণ (টাকা)", instAmount, { instAmount = it }, keyboard = KeyboardType.Decimal)
                LabeledField("কিস্তির সংখ্যা", instCount, { instCount = it }, keyboard = KeyboardType.Number)
                AccountPicker("ঋণের টাকা যে অ্যাকাউন্টে এসেছে", accounts, accountId) { accountId = it }
                err?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            } },
            confirmButton = { Button(onClick = {
                try {
                    val p = Money.parsePoisha(principal); val acc = accountId ?: throw IllegalArgumentException("অ্যাকাউন্ট নির্বাচন করুন।")
                    if (name.isBlank()) throw IllegalArgumentException("এনজিওর নাম আবশ্যক।")
                    val loan = NgoLoanAccount(ngoName = name.trim(), principalPoisha = p, disbursedAt = System.currentTimeMillis(),
                        serviceChargePoisha = service.takeIf { it.isNotBlank() }?.let { Money.parsePoisha(it) },
                        otherChargesPoisha = other.takeIf { it.isNotBlank() }?.let { Money.parsePoisha(it) },
                        totalPayablePoisha = totalPayable.takeIf { it.isNotBlank() }?.let { Money.parsePoisha(it) },
                        frequency = freq, installmentPoisha = instAmount.takeIf { it.isNotBlank() }?.let { Money.parsePoisha(it) },
                        installmentCount = instCount.toIntOrNull(), firstInstallmentAt = System.currentTimeMillis(), disbursementAccountId = acc)
                    vm.run("এনজিও ঋণ সংরক্ষণ হয়েছে।") { vm.repo.createNgoLoan(loan, generateSchedule = loan.installmentCount != null && loan.installmentPoisha != null) }; showAdd = false
                } catch (e: Exception) { err = e.message }
            }) { Text("সংরক্ষণ") } },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("বাতিল") } })
    }
    payFor?.let { l ->
        var amount by remember(l.id) { mutableStateOf("") }; var accountId by remember(l.id) { mutableStateOf<Long?>(accounts.firstOrNull()?.id) }; var err by remember(l.id) { mutableStateOf<String?>(null) }
        AlertDialog(onDismissRequest = { payFor = null }, title = { Text("এনজিও কিস্তি পরিশোধ") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField("পরিমাণ (টাকা) *", amount, { amount = it }, keyboard = KeyboardType.Decimal)
                AccountPicker("যে অ্যাকাউন্ট থেকে", accounts, accountId) { accountId = it }
                Text("আংশিক কিস্তি পরিশোধও রেকর্ড করা যাবে। মূলধন পরিশোধ ব্যয় নয়।", style = MaterialTheme.typography.bodySmall)
                err?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            } },
            confirmButton = { Button(onClick = {
                try { val amt = Money.parsePoisha(amount); val acc = accountId ?: throw IllegalArgumentException("অ্যাকাউন্ট নির্বাচন করুন।")
                    vm.run("কিস্তি পরিশোধ হয়েছে।") { vm.repo.payNgoInstallment(l.id, null, amt, System.currentTimeMillis(), acc) }; payFor = null
                } catch (e: Exception) { err = e.message }
            }) { Text("পরিশোধ করুন") } },
            dismissButton = { TextButton(onClick = { payFor = null }) { Text("বাতিল") } })
    }
}

@Composable
private fun BankSection(vm: MainViewModel) {
    val loans by vm.bankLoans.collectAsState()
    val accounts by vm.accounts.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var payFor by remember { mutableStateOf<BankLoanAccount?>(null) }
    var scheduleFor by remember { mutableStateOf<BankLoanAccount?>(null) }
    Column {
        Spacer(Modifier.height(8.dp))
        Button(onClick = { showAdd = true }) { Text("নতুন ব্যাংক ঋণ") }
        Text("পদ্ধতি নিজে বেছে নিন: রিডিউসিং ব্যালেন্স / ফ্ল্যাট / ম্যানুয়াল (নিজের দেওয়া কিস্তি)। তথ্য অসম্পূর্ণ হলে অ্যাপ সুদ অনুমান করবে না।", style = MaterialTheme.typography.bodySmall)
        if (loans.isEmpty()) EmptyState("কোনো ব্যাংক ঋণ নেই।", "ব্যাংক ঋণ যোগ করুন") { showAdd = true }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(loans, key = { it.id }) { l ->
                val paidFlow = remember(l.id) { vm.repo.observeBankPaid(l.id) }
                val paid by paidFlow.collectAsState(initial = 0L)
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${l.bankName} ${l.nickname}", style = MaterialTheme.typography.titleMedium)
                        Text("মূলধন ${Money.formatPoisha(l.principalPoisha)} • পরিশোধ ${Money.formatPoisha(paid)} • পদ্ধতি: ${l.method}")
                        l.contractualTotalPayablePoisha?.let { Text("চুক্তিভিত্তিক বকেয়া: ${Money.formatPoisha((it - paid).coerceAtLeast(0))}") }
                        l.annualRatePercent?.let { Text("বার্ষিক সুদহার: ${Money.toBengaliDigits(it)}%") }
                        Row {
                            TextButton(onClick = { payFor = l }) { Text("কিস্তি পরিশোধ") }
                            TextButton(onClick = { scheduleFor = l }) { Text("কিস্তির তালিকা") }
                        }
                    }
                }
            }
        }
    }
    if (showAdd) {
        var bank by remember { mutableStateOf("") }; var principal by remember { mutableStateOf("") }
        var rate by remember { mutableStateOf("") }; var method by remember { mutableStateOf("MANUAL") }
        var tenure by remember { mutableStateOf("") }; var instAmount by remember { mutableStateOf("") }
        var fee by remember { mutableStateOf("") }; var totalPayable by remember { mutableStateOf("") }
        var accountId by remember { mutableStateOf<Long?>(accounts.firstOrNull()?.id) }; var err by remember { mutableStateOf<String?>(null) }
        AlertDialog(onDismissRequest = { showAdd = false }, title = { Text("নতুন ব্যাংক ঋণ") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField("ব্যাংকের নাম *", bank, { bank = it })
                LabeledField("ঋণের মূলধন (টাকা) *", principal, { principal = it }, keyboard = KeyboardType.Decimal)
                SimplePicker("হিসাবের পদ্ধতি", listOf("MANUAL" to "ম্যানুয়াল (নিজের কিস্তি তালিকা)", "REDUCING_BALANCE" to "রিডিউসিং ব্যালেন্স", "FLAT" to "ফ্ল্যাট সুদ"), method) { method = it }
                LabeledField("বার্ষিক সুদহার % (রিডিউসিং/ফ্ল্যাটের জন্য আবশ্যক)", rate, { rate = it }, keyboard = KeyboardType.Decimal)
                LabeledField("মেয়াদ (মাস)", tenure, { tenure = it }, keyboard = KeyboardType.Number)
                LabeledField("কিস্তির পরিমাণ (টাকা, ম্যানুয়ালের জন্য আবশ্যক)", instAmount, { instAmount = it }, keyboard = KeyboardType.Decimal)
                LabeledField("প্রসেসিং ফি (টাকা)", fee, { fee = it }, keyboard = KeyboardType.Decimal)
                LabeledField("চুক্তিভিত্তিক মোট পরিশোধযোগ্য (টাকা, থাকলে)", totalPayable, { totalPayable = it }, keyboard = KeyboardType.Decimal)
                AccountPicker("ঋণের টাকা যে অ্যাকাউন্টে এসেছে", accounts, accountId) { accountId = it }
                err?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            } },
            confirmButton = { Button(onClick = {
                try {
                    val p = Money.parsePoisha(principal); val acc = accountId ?: throw IllegalArgumentException("অ্যাকাউন্ট নির্বাচন করুন।")
                    if (bank.isBlank()) throw IllegalArgumentException("ব্যাংকের নাম আবশ্যক।")
                    if (method != "MANUAL" && rate.isBlank()) throw IllegalArgumentException("এই পদ্ধতির জন্য সুদহার আবশ্যক — নাহলে ম্যানুয়াল পদ্ধতি বেছে নিন।")
                    val loan = BankLoanAccount(bankName = bank.trim(), principalPoisha = p, disbursedAt = System.currentTimeMillis(),
                        annualRatePercent = rate.takeIf { it.isNotBlank() }, method = method, tenureMonths = tenure.toIntOrNull(),
                        installmentPoisha = instAmount.takeIf { it.isNotBlank() }?.let { Money.parsePoisha(it) },
                        processingFeePoisha = fee.takeIf { it.isNotBlank() }?.let { Money.parsePoisha(it) } ?: 0,
                        contractualTotalPayablePoisha = totalPayable.takeIf { it.isNotBlank() }?.let { Money.parsePoisha(it) },
                        disbursementAccountId = acc)
                    vm.run("ব্যাংক ঋণ সংরক্ষণ হয়েছে।") { vm.repo.createBankLoan(loan) }; showAdd = false
                } catch (e: Exception) { err = e.message }
            }) { Text("সংরক্ষণ") } },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("বাতিল") } })
    }
    payFor?.let { l ->
        var amount by remember(l.id) { mutableStateOf("") }; var accountId by remember(l.id) { mutableStateOf<Long?>(accounts.firstOrNull()?.id) }; var err by remember(l.id) { mutableStateOf<String?>(null) }
        AlertDialog(onDismissRequest = { payFor = null }, title = { Text("ব্যাংক কিস্তি পরিশোধ") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField("পরিমাণ (টাকা) *", amount, { amount = it }, keyboard = KeyboardType.Decimal)
                AccountPicker("যে অ্যাকাউন্ট থেকে", accounts, accountId) { accountId = it }
                err?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            } },
            confirmButton = { Button(onClick = {
                try { val amt = Money.parsePoisha(amount); val acc = accountId ?: throw IllegalArgumentException("অ্যাকাউন্ট নির্বাচন করুন।")
                    vm.run("কিস্তি পরিশোধ হয়েছে।") { vm.repo.payBankInstallment(l.id, null, amt, null, null, System.currentTimeMillis(), acc) }; payFor = null
                } catch (e: Exception) { err = e.message }
            }) { Text("পরিশোধ করুন") } },
            dismissButton = { TextButton(onClick = { payFor = null }) { Text("বাতিল") } })
    }
    scheduleFor?.let { l ->
        val flow = remember(l.id) { vm.repo.observeBankSchedule(l.id) }
        val sched by flow.collectAsState(initial = emptyList())
        AlertDialog(onDismissRequest = { scheduleFor = null }, title = { Text("${l.bankName} — কিস্তির তালিকা") },
            text = { LazyColumn(Modifier.heightIn(max = 420.dp)) {
                if (sched.isEmpty()) item { Text("কোনো কিস্তি তালিকা তৈরি হয়নি (মেয়াদ/কিস্তি দেওয়া হয়নি)।") }
                items(sched, key = { it.id }) { s ->
                    ListItem(headlineContent = { Text("${Dates.format(s.dueAt)} — ${Money.formatPoisha(s.expectedPoisha)}") },
                        supportingContent = { Text(listOfNotNull(s.interestPoisha?.let { "সুদ ${Money.formatPoisha(it)}" }, s.principalPoisha?.let { "আসল ${Money.formatPoisha(it)}" }).joinToString(" • ").ifBlank { "সুদ/আসলের বিভাজন জানা নেই (ম্যানুয়াল)" }) })
                }
            } },
            confirmButton = { TextButton(onClick = { scheduleFor = null }) { Text("বন্ধ করুন") } })
    }
}
