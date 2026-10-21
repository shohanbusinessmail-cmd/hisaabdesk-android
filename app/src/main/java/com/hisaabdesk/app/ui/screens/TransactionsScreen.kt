
package com.hisaabdesk.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hisaabdesk.app.core.Dates
import com.hisaabdesk.app.core.Money
import com.hisaabdesk.app.data.local.entity.RecurringTransaction
import com.hisaabdesk.app.ui.MainViewModel
import com.hisaabdesk.app.ui.components.*
import java.time.LocalDate

@Composable
fun TransactionsScreen(vm: MainViewModel, initialAdd: String? = null) {
    val accounts by vm.accounts.collectAsState()
    val categories by vm.categories.collectAsState()
    var query by remember { mutableStateOf("") }
    val ledgerFlow = remember(query) { if (query.isBlank()) vm.repo.observeLedger(100) else vm.repo.searchLedger(query) }
    val entries by ledgerFlow.collectAsState(initial = emptyList())
    var addKind by remember { mutableStateOf(initialAdd) } // INCOME / EXPENSE
    var transferOpen by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<com.hisaabdesk.app.data.local.entity.LedgerEntry?>(null) }
    var tab by remember { mutableStateOf(0) }
    val recurring by vm.recurring.collectAsState()

    Scaffold(floatingActionButton = {
        FloatingActionButton(onClick = { addKind = "EXPENSE" }) { Icon(Icons.Default.Add, contentDescription = "নতুন লেনদেন") }
    }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().padding(16.dp)) {
            Text("লেনদেন", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }) { Text("সব লেনদেন", Modifier.padding(12.dp)) }
                Tab(selected = tab == 1, onClick = { tab = 1 }) { Text("পুনরাবৃত্ত লেনদেন", Modifier.padding(12.dp)) }
            }
            if (tab == 0) {
                Spacer(Modifier.height(8.dp))
                LabeledField("খুঁজুন (বিবরণ/নাম/নোট)", query, { query = it })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                    Button(onClick = { addKind = "INCOME" }) { Text("আয় যোগ করুন") }
                    Button(onClick = { addKind = "EXPENSE" }) { Text("ব্যয় যোগ করুন") }
                    OutlinedButton(onClick = { transferOpen = true }) { Text("স্থানান্তর") }
                }
                if (entries.isEmpty()) EmptyState("কোনো লেনদেন পাওয়া যায়নি।", "আয় যোগ করুন") { addKind = "INCOME" }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(entries, key = { it.id }) { e ->
                        ListItem(
                            headlineContent = { Text("${typeLabelBn(e.type)} — ${e.description.ifBlank { e.counterparty.ifBlank { "লেনদেন" } }}") },
                            supportingContent = { Text("${Dates.format(e.occurredAt)}${if (e.counterparty.isNotBlank()) " • ${e.counterparty}" else ""}${if (e.paymentMethod.isNotBlank()) " • ${e.paymentMethod}" else ""}") },
                            trailingContent = { Column { MoneyText(e.amountPoisha); TextButton(onClick = { deleteTarget = e }) { Text("মুছুন/সংশোধন") } } }
                        )
                    }
                }
            } else {
                RecurringSection(vm)
            }
        }
    }

    addKind?.let { kind ->
        var amount by remember(kind) { mutableStateOf("") }
        var accountId by remember(kind) { mutableStateOf<Long?>(accounts.firstOrNull()?.id) }
        var categoryId by remember(kind) { mutableStateOf<Long?>(null) }
        var party by remember(kind) { mutableStateOf("") }
        var method by remember(kind) { mutableStateOf("নগদ") }
        var desc by remember(kind) { mutableStateOf("") }
        var dateText by remember(kind) { mutableStateOf(Dates.format(Dates.today())) }
        var err by remember(kind) { mutableStateOf<String?>(null) }
        AlertDialog(onDismissRequest = { addKind = null }, title = { Text(if (kind == "INCOME") "আয় যোগ করুন" else "ব্যয় যোগ করুন") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField("পরিমাণ (টাকা) *", amount, { amount = it }, keyboard = KeyboardType.Decimal)
                AccountPicker(if (kind == "INCOME") "যে অ্যাকাউন্টে জমা হবে" else "যে অ্যাকাউন্ট থেকে যাবে", accounts, accountId) { accountId = it }
                CategoryPicker("ক্যাটাগরি", categories.filter { it.kind == kind }, categoryId) { categoryId = it }
                LabeledField(if (kind == "INCOME") "উৎস/প্রদানকারী" else "দোকান/প্রাপক", party, { party = it })
                LabeledField("পেমেন্ট মাধ্যম", method, { method = it })
                LabeledField("বিবরণ", desc, { desc = it })
                LabeledField("তারিখ (দিন/মাস/বছর)", dateText, { dateText = it })
                err?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            } },
            confirmButton = { Button(onClick = {
                try {
                    val amt = Money.parsePoisha(amount); val acc = accountId ?: throw IllegalArgumentException("অ্যাকাউন্ট নির্বাচন করুন।")
                    val at = Dates.startOfDayMillis(Dates.parseDdMmYyyy(dateText)) + (System.currentTimeMillis() % 86400000L)
                    vm.run(if (kind == "INCOME") "আয় সংরক্ষণ হয়েছে।" else "ব্যয় সংরক্ষণ হয়েছে।") {
                        if (kind == "INCOME") vm.repo.addIncome(acc, amt, at, categoryId, party.trim(), method.trim(), desc.trim())
                        else vm.repo.addExpense(acc, amt, at, categoryId, party.trim(), method.trim(), desc.trim())
                    }; addKind = null
                } catch (e: Exception) { err = e.message ?: "তথ্য সঠিক নয়।" }
            }) { Text("সংরক্ষণ") } },
            dismissButton = { TextButton(onClick = { addKind = null }) { Text("বাতিল") } })
    }

    if (transferOpen) {
        // Reuse accounts screen transfer via navigation is complex; inline simple transfer:
        var fromId by remember { mutableStateOf<Long?>(accounts.firstOrNull()?.id) }
        var toId by remember { mutableStateOf<Long?>(accounts.getOrNull(1)?.id) }
        var amount by remember { mutableStateOf("") }; var err by remember { mutableStateOf<String?>(null) }
        AlertDialog(onDismissRequest = { transferOpen = false }, title = { Text("টাকা স্থানান্তর") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AccountPicker("যে অ্যাকাউন্ট থেকে", accounts, fromId) { fromId = it }
                AccountPicker("যে অ্যাকাউন্টে", accounts, toId) { toId = it }
                LabeledField("পরিমাণ (টাকা) *", amount, { amount = it }, keyboard = KeyboardType.Decimal)
                err?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            } },
            confirmButton = { Button(onClick = {
                try { val amt = Money.parsePoisha(amount); val f = fromId; val t = toId
                    if (f == null || t == null) err = "দুটি অ্যাকাউন্ট নির্বাচন করুন।"
                    else { vm.run("স্থানান্তর সম্পন্ন হয়েছে।") { vm.repo.transfer(f, t, amt, System.currentTimeMillis(), "") }; transferOpen = false }
                } catch (e: Exception) { err = e.message }
            }) { Text("স্থানান্তর করুন") } },
            dismissButton = { TextButton(onClick = { transferOpen = false }) { Text("বাতিল") } })
    }

    deleteTarget?.let { e ->
        AlertDialog(onDismissRequest = { deleteTarget = null }, title = { Text("লেনদেন মুছে ফেলবেন?") },
            text = { Text("এই লেনদেনটি মুছে ফেললে সংশ্লিষ্ট অ্যাকাউন্টের ব্যালেন্স সঙ্গে সঙ্গে হালনাগাদ হবে। স্থানান্তরের ক্ষেত্রে দুই দিকের রেকর্ডই মুছে যাবে। হিসাবের ইতিহাসে ভুল থাকলে নতুন সঠিক লেনদেন যোগ করে ভারসাম্য মেলানো ভালো।") },
            confirmButton = { Button(colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), onClick = { vm.run("লেনদেন মোছে ফেলা হয়েছে।") { vm.repo.deleteLedgerEntry(e) }; deleteTarget = null }) { Text("মুছে ফেলুন") } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("বাতিল") } })
    }
}

@Composable
private fun RecurringSection(vm: MainViewModel) {
    val list by vm.recurring.collectAsState()
    val accounts by vm.accounts.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    Column {
        Spacer(Modifier.height(8.dp))
        Button(onClick = { showAdd = true }) { Text("নতুন পুনরাবৃত্ত লেনদেন") }
        Text("অটো-পোস্ট চালু থাকলে নির্ধারিত তারিখে অ্যাপ খোলার পর একবারই লেনদেন যোগ হবে (ডুপ্লিকেট হবে না)। অটো বন্ধ থাকলে ‘পোস্ট করুন’ চাপলেই যোগ হবে।", style = MaterialTheme.typography.bodySmall)
        if (list.isEmpty()) EmptyState("কোনো পুনরাবৃত্ত লেনদেন নেই।")
        LazyColumn {
            items(list, key = { it.id }) { r ->
                ListItem(headlineContent = { Text("${r.title} — ${if (r.type == "INCOME") "আয়" else "ব্যয়"}") },
                    supportingContent = { Text("পরবর্তী: ${Dates.format(r.nextDueAt)} • ${r.frequency} • ${if (r.autoPost) "অটো-পোস্ট চালু" else "ম্যানুয়াল"}") },
                    trailingContent = { Column { MoneyText(r.amountPoisha); Row {
                        TextButton(onClick = { vm.run("লেনদেন পোস্ট হয়েছে।") { vm.repo.postRecurring(r) } }) { Text("পোস্ট করুন") }
                        TextButton(onClick = { vm.run("মুছে ফেলা হয়েছে।") { vm.repo.deleteRecurring(r.id) } }) { Text("মুছুন") }
                    } } })
            }
        }
    }
    if (showAdd) {
        var title by remember { mutableStateOf("") }; var type by remember { mutableStateOf("EXPENSE") }
        var amount by remember { mutableStateOf("") }; var accountId by remember { mutableStateOf<Long?>(accounts.firstOrNull()?.id) }
        var freq by remember { mutableStateOf("MONTHLY") }; var auto by remember { mutableStateOf(false) }; var err by remember { mutableStateOf<String?>(null) }
        AlertDialog(onDismissRequest = { showAdd = false }, title = { Text("পুনরাবৃত্ত লেনদেন") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField("নাম (যেমন: বাসা ভাড়া) *", title, { title = it })
                SimplePicker("ধরন", listOf("INCOME" to "আয়", "EXPENSE" to "ব্যয়"), type) { type = it }
                LabeledField("পরিমাণ (টাকা) *", amount, { amount = it }, keyboard = KeyboardType.Decimal)
                AccountPicker("অ্যাকাউন্ট", accounts, accountId) { accountId = it }
                SimplePicker("পুনরাবৃত্তি", listOf("DAILY" to "প্রতিদিন", "WEEKLY" to "সাপ্তাহিক", "MONTHLY" to "মাসিক", "YEARLY" to "বাৎসরিক"), freq) { freq = it }
                Row { Checkbox(auto, { auto = it }); Text("অটো-পোস্ট (তারিখ এলেই স্বয়ংক্রিয় যোগ হবে)") }
                err?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            } },
            confirmButton = { Button(onClick = {
                try { val amt = Money.parsePoisha(amount); val acc = accountId ?: throw IllegalArgumentException("অ্যাকাউন্ট নির্বাচন করুন।")
                    if (title.isBlank()) err = "নাম আবশ্যক।" else { vm.run("পুনরাবৃত্ত লেনদেন তৈরি হয়েছে।") { vm.repo.addRecurring(RecurringTransaction(title = title.trim(), type = type, amountPoisha = amt, accountId = acc, frequency = freq, nextDueAt = System.currentTimeMillis(), autoPost = auto)) }; showAdd = false }
                } catch (e: Exception) { err = e.message }
            }) { Text("সংরক্ষণ") } },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("বাতিল") } })
    }
}
