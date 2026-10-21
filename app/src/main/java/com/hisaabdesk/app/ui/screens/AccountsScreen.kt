
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
import com.hisaabdesk.app.data.local.entity.FinancialAccount
import com.hisaabdesk.app.ui.MainViewModel
import com.hisaabdesk.app.ui.components.*

@Composable
fun AccountsScreen(vm: MainViewModel, openTransfer: Boolean = false) {
    val accounts by vm.accounts.collectAsState()
    var showCreate by remember { mutableStateOf(false) }
    var showTransfer by remember { mutableStateOf(openTransfer) }
    var selected by remember { mutableStateOf<FinancialAccount?>(null) }
    var query by remember { mutableStateOf("") }

    Scaffold(floatingActionButton = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FloatingActionButton(onClick = { showTransfer = true }) { Text("⇄", style = MaterialTheme.typography.titleMedium) }
            FloatingActionButton(onClick = { showCreate = true }) { Icon(Icons.Default.Add, contentDescription = "নতুন অ্যাকাউন্ট") }
        }
    }) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text("অ্যাকাউন্ট", style = MaterialTheme.typography.headlineSmall) }
            item { LabeledField("অ্যাকাউন্ট খুঁজুন", query, { query = it }) }
            val filtered = accounts.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
            if (filtered.isEmpty()) item { EmptyState("কোনো অ্যাকাউন্ট পাওয়া যায়নি।", "অ্যাকাউন্ট তৈরি করুন") { showCreate = true } }
            items(filtered, key = { it.id }) { a ->
                val balFlow = remember(a.id) { vm.repo.accountBalance(a.id) }
                val ledgerSum by balFlow.collectAsState(initial = 0L)
                val balance = a.openingBalancePoisha + ledgerSum
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(a.name + if (a.archived) " (আর্কাইভ)" else "", style = MaterialTheme.typography.titleMedium); MoneyText(balance)
                        }
                        Text("${accountTypeBn(a.type)}${if (a.institution.isNotBlank()) " • ${a.institution}" else ""}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { selected = a }) { Text("বিস্তারিত ও ইতিহাস") }
                            TextButton(onClick = { vm.run("অ্যাকাউন্ট হালনাগাদ হয়েছে।") { vm.repo.updateAccount(a.copy(archived = !a.archived)) } }) { Text(if (a.archived) "পুনরায় চালু" else "আর্কাইভ") }
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        var name by remember { mutableStateOf("") }; var type by remember { mutableStateOf("CASH") }
        var institution by remember { mutableStateOf("") }; var opening by remember { mutableStateOf("") }; var err by remember { mutableStateOf<String?>(null) }
        AlertDialog(onDismissRequest = { showCreate = false }, title = { Text("নতুন অ্যাকাউন্ট") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField("অ্যাকাউন্টের নাম *", name, { name = it })
                SimplePicker("ধরন", listOf("CASH" to "নগদ", "BANK" to "ব্যাংক", "MFS" to "মোবাইল ব্যাংকিং (বিকাশ/নগদ ইত্যাদি)", "SAVINGS" to "সঞ্চয় অ্যাকাউন্ট", "OTHER" to "অন্যান্য"), type) { type = it }
                LabeledField("প্রতিষ্ঠান/প্রোভাইডার", institution, { institution = it })
                LabeledField("শুরুর ব্যালেন্স (টাকা)", opening, { opening = it }, keyboard = KeyboardType.Decimal)
                Text("শুরুর ব্যালেন্স আপনার বর্তমান আসল টাকা — এটি আয় হিসেবে গণনা হবে না।", style = MaterialTheme.typography.bodySmall)
                err?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            } },
            confirmButton = { Button(onClick = {
                try { val ob = if (opening.isBlank()) 0L else Money.parsePoisha(opening)
                    if (name.isBlank()) { err = "অ্যাকাউন্টের নাম আবশ্যক।" } else { vm.run("অ্যাকাউন্ট তৈরি হয়েছে।") { vm.repo.createAccount(FinancialAccount(name = name.trim(), type = type, institution = institution.trim(), openingBalancePoisha = ob)) }; showCreate = false }
                } catch (e: Exception) { err = e.message }
            }) { Text("সংরক্ষণ") } },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("বাতিল") } })
    }

    if (showTransfer) {
        var fromId by remember { mutableStateOf<Long?>(accounts.firstOrNull()?.id) }
        var toId by remember { mutableStateOf<Long?>(accounts.getOrNull(1)?.id) }
        var amount by remember { mutableStateOf("") }; var note by remember { mutableStateOf("") }
        var allowOd by remember { mutableStateOf(false) }; var err by remember { mutableStateOf<String?>(null) }
        AlertDialog(onDismissRequest = { showTransfer = false }, title = { Text("টাকা স্থানান্তর") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AccountPicker("যে অ্যাকাউন্ট থেকে", accounts, fromId) { fromId = it }
                AccountPicker("যে অ্যাকাউন্টে", accounts, toId) { toId = it }
                LabeledField("পরিমাণ (টাকা) *", amount, { amount = it }, keyboard = KeyboardType.Decimal)
                LabeledField("নোট", note, { note = it })
                Row { Checkbox(allowOd, { allowOd = it }); Text("পর্যাপ্ত ব্যালেন্স না থাকলেও অনুমতি দিন (ওভারড্রাফট)") }
                err?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            } },
            confirmButton = { Button(onClick = {
                try { val amt = Money.parsePoisha(amount); val f = fromId; val t = toId
                    if (f == null || t == null) err = "দুটি অ্যাকাউন্ট নির্বাচন করুন।" else { vm.run("স্থানান্তর সম্পন্ন হয়েছে।") { vm.repo.transfer(f, t, amt, System.currentTimeMillis(), note, allowOd) }; showTransfer = false }
                } catch (e: Exception) { err = e.message }
            }) { Text("স্থানান্তর করুন") } },
            dismissButton = { TextButton(onClick = { showTransfer = false }) { Text("বাতিল") } })
    }

    selected?.let { a ->
        val historyFlow = remember(a.id) { vm.repo.observeAccountLedger(a.id) }
        val history by historyFlow.collectAsState(initial = emptyList())
        AlertDialog(onDismissRequest = { selected = null }, title = { Text(a.name) },
            text = { LazyColumn(Modifier.heightIn(max = 420.dp)) {
                item { Text("খোলা: ${Dates.format(a.createdAt)} • ধরন: ${accountTypeBn(a.type)}") ; if (a.notes.isNotBlank()) Text(a.notes) }
                if (history.isEmpty()) item { Text("এই অ্যাকাউন্টে এখনও কোনো লেনদেন নেই।") }
                items(history, key = { it.id }) { e -> ListItem(headlineContent = { Text("${typeLabelBn(e.type)} — ${e.description}") }, supportingContent = { Text(Dates.format(e.occurredAt)) }, trailingContent = { MoneyText(e.amountPoisha) }) }
            } },
            confirmButton = { TextButton(onClick = { selected = null }) { Text("বন্ধ করুন") } })
    }
}

fun accountTypeBn(type: String): String = when (type) {
    "CASH" -> "নগদ"; "BANK" -> "ব্যাংক"; "MFS" -> "মোবাইল ব্যাংকিং"; "SAVINGS" -> "সঞ্চয়"; else -> "অন্যান্য"
}
