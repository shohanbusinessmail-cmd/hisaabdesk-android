
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
fun DebtsScreen(vm: MainViewModel) {
    val persons by vm.persons.collectAsState()
    val debts by vm.debts.collectAsState()
    val accounts by vm.accounts.collectAsState()
    var tab by remember { mutableStateOf(0) }
    var showPerson by remember { mutableStateOf(false) }
    var showDebt by remember { mutableStateOf(false) }
    var payFor by remember { mutableStateOf<PersonalDebt?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("ব্যক্তিগত ধার-দেনা", style = MaterialTheme.typography.headlineSmall)
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }) { Text("ধার-দেনা", Modifier.padding(12.dp)) }
            Tab(selected = tab == 1, onClick = { tab = 1 }) { Text("ব্যক্তি", Modifier.padding(12.dp)) }
        }
        Spacer(Modifier.height(8.dp))
        if (tab == 0) {
            Button(onClick = { showDebt = true }) { Text("নতুন ধার/দেনা যোগ করুন") }
            Spacer(Modifier.height(8.dp))
            if (debts.isEmpty()) EmptyState("কোনো ধার-দেনার রেকর্ড নেই।", "ধার/দেনা যোগ করুন") { showDebt = true }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(debts, key = { it.id }) { d ->
                    val paidFlow = remember(d.id) { vm.repo.observeDebtPaid(d.id) }
                    val paid by paidFlow.collectAsState(initial = 0L)
                    val outstanding = (d.principalPoisha - paid).coerceAtLeast(0)
                    val personName = persons.firstOrNull { it.id == d.personId }?.name ?: "ব্যক্তি"
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("$personName — ${if (d.direction == "BORROWED") "আপনার দেনা" else "আপনার পাওনা"}", style = MaterialTheme.typography.titleMedium)
                            Text("মূল ${Money.formatPoisha(d.principalPoisha)} • পরিশোধ ${Money.formatPoisha(paid)} • বকেয়া ${Money.formatPoisha(outstanding)} • ${d.status}")
                            d.expectedRepayAt?.let { Text("প্রত্যাশিত পরিশোধ: ${Dates.format(it)}${if (it < System.currentTimeMillis() && outstanding > 0) " (মেয়াদ উত্তীর্ণ)" else ""}") }
                            if (outstanding > 0) TextButton(onClick = { payFor = d }) { Text(if (d.direction == "BORROWED") "পরিশোধ করুন" else "ফেরত গ্রহণ করুন") }
                        }
                    }
                }
            }
        } else {
            Button(onClick = { showPerson = true }) { Text("নতুন ব্যক্তি যোগ করুন") }
            Spacer(Modifier.height(8.dp))
            if (persons.isEmpty()) EmptyState("কোনো ব্যক্তি যোগ করা হয়নি।")
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(persons, key = { it.id }) { p ->
                    ListItem(headlineContent = { Text(p.name + if (p.archived) " (আর্কাইভ)" else "") },
                        supportingContent = { Text(listOf(p.phone, p.address).filter { it.isNotBlank() }.joinToString(" • ")) },
                        trailingContent = { TextButton(onClick = { vm.run("ব্যক্তি হালনাগাদ হয়েছে।") { vm.repo.updatePerson(p.copy(archived = !p.archived)) } }) { Text(if (p.archived) "চালু" else "আর্কাইভ") } })
                }
            }
        }
    }

    if (showPerson) {
        var name by remember { mutableStateOf("") }; var phone by remember { mutableStateOf("") }; var address by remember { mutableStateOf("") }; var err by remember { mutableStateOf<String?>(null) }
        AlertDialog(onDismissRequest = { showPerson = false }, title = { Text("নতুন ব্যক্তি") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField("পুরো নাম *", name, { name = it }); LabeledField("ফোন", phone, { phone = it }, keyboard = KeyboardType.Phone); LabeledField("ঠিকানা", address, { address = it })
                err?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            } },
            confirmButton = { Button(onClick = { if (name.isBlank()) err = "নাম আবশ্যক।" else { vm.run("ব্যক্তি যোগ হয়েছে।") { vm.repo.addPerson(Person(name = name.trim(), phone = phone.trim(), address = address.trim())) }; showPerson = false } }) { Text("সংরক্ষণ") } },
            dismissButton = { TextButton(onClick = { showPerson = false }) { Text("বাতিল") } })
    }

    if (showDebt) {
        var personId by remember { mutableStateOf<Long?>(persons.firstOrNull()?.id) }
        var direction by remember { mutableStateOf("BORROWED") }
        var amount by remember { mutableStateOf("") }; var purpose by remember { mutableStateOf("") }
        var accountId by remember { mutableStateOf<Long?>(accounts.firstOrNull()?.id) }; var err by remember { mutableStateOf<String?>(null) }
        AlertDialog(onDismissRequest = { showDebt = false }, title = { Text("নতুন ধার/দেনা") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SimplePicker("ব্যক্তি", persons.map { it.id.toString() to it.name }, personId?.toString() ?: "") { personId = it.toLongOrNull() }
                SimplePicker("ধরন", listOf("BORROWED" to "আমি ধার নিয়েছি (দেনা)", "LENT" to "আমি ধার দিয়েছি (পাওনা)"), direction) { direction = it }
                LabeledField("মূল পরিমাণ (টাকা) *", amount, { amount = it }, keyboard = KeyboardType.Decimal)
                LabeledField("উদ্দেশ্য", purpose, { purpose = it })
                AccountPicker(if (direction == "BORROWED") "যে অ্যাকাউন্টে টাকা এসেছে" else "যে অ্যাকাউন্ট থেকে টাকা গেছে", accounts, accountId) { accountId = it }
                Text("ধার গ্রহণ আয় নয় এবং ধার দেওয়া ব্যয় নয় — এগুলো দায়/পাওনা হিসেবে আলাদা দেখানো হয়।", style = MaterialTheme.typography.bodySmall)
                err?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            } },
            confirmButton = { Button(onClick = {
                try { val amt = Money.parsePoisha(amount); val pid = personId ?: throw IllegalArgumentException("ব্যক্তি নির্বাচন করুন।"); val acc = accountId ?: throw IllegalArgumentException("অ্যাকাউন্ট নির্বাচন করুন।")
                    vm.run("ধার-দেনা সংরক্ষণ হয়েছে।") { vm.repo.createPersonalDebt(pid, direction, amt, System.currentTimeMillis(), null, purpose.trim(), acc) }; showDebt = false
                } catch (e: Exception) { err = e.message }
            }) { Text("সংরক্ষণ") } },
            dismissButton = { TextButton(onClick = { showDebt = false }) { Text("বাতিল") } })
    }

    payFor?.let { d ->
        var amount by remember(d.id) { mutableStateOf("") }; var accountId by remember(d.id) { mutableStateOf<Long?>(accounts.firstOrNull()?.id) }; var err by remember(d.id) { mutableStateOf<String?>(null) }
        AlertDialog(onDismissRequest = { payFor = null }, title = { Text(if (d.direction == "BORROWED") "ধার পরিশোধ" else "ধার ফেরত গ্রহণ") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField("পরিমাণ (টাকা) *", amount, { amount = it }, keyboard = KeyboardType.Decimal)
                AccountPicker("অ্যাকাউন্ট", accounts, accountId) { accountId = it }
                Text("আংশিক পরিশোধ সমর্থিত। বকেয়ার বেশি পরিমাণ গ্রহণ করা হবে না। ফেরত পাওয়া মূলধন নতুন আয় নয়।", style = MaterialTheme.typography.bodySmall)
                err?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            } },
            confirmButton = { Button(onClick = {
                try { val amt = Money.parsePoisha(amount); val acc = accountId ?: throw IllegalArgumentException("অ্যাকাউন্ট নির্বাচন করুন।")
                    vm.run("পরিশোধ সংরক্ষণ হয়েছে।") { vm.repo.repayPersonalDebt(d.id, amt, System.currentTimeMillis(), acc, "নগদ") }; payFor = null
                } catch (e: Exception) { err = e.message }
            }) { Text("সংরক্ষণ") } },
            dismissButton = { TextButton(onClick = { payFor = null }) { Text("বাতিল") } })
    }
}
