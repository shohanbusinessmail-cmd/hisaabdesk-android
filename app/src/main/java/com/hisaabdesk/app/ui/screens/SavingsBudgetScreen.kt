
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
import com.hisaabdesk.app.domain.calculator.FinanceCalculator
import com.hisaabdesk.app.ui.MainViewModel
import com.hisaabdesk.app.ui.components.*

@Composable
fun SavingsBudgetScreen(vm: MainViewModel) {
    var tab by remember { mutableStateOf(0) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("সঞ্চয় ও বাজেট", style = MaterialTheme.typography.headlineSmall)
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }) { Text("সঞ্চয়", Modifier.padding(12.dp)) }
            Tab(selected = tab == 1, onClick = { tab = 1 }) { Text("বাজেট", Modifier.padding(12.dp)) }
        }
        if (tab == 0) SavingsSection(vm) else BudgetsSection(vm)
    }
}

@Composable
private fun SavingsSection(vm: MainViewModel) {
    val savings by vm.savings.collectAsState()
    val accounts by vm.accounts.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var moveFor by remember { mutableStateOf<SavingsAccount?>(null) }
    Column {
        Spacer(Modifier.height(8.dp))
        Button(onClick = { showAdd = true }) { Text("নতুন সঞ্চয় অ্যাকাউন্ট") }
        Text("সঞ্চয়ে জমা ব্যয় নয় এবং সঞ্চয় থেকে উত্তোলন আয় নয় — এগুলো নিজের টাকার স্থানান্তর।", style = MaterialTheme.typography.bodySmall)
        if (savings.isEmpty()) EmptyState("কোনো সঞ্চয় অ্যাকাউন্ট নেই। এগুলো উদাহরণ হিসেবে তৈরি করা হয়নি — নিজে তৈরি করুন।", "সঞ্চয় অ্যাকাউন্ট তৈরি") { showAdd = true }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(savings, key = { it.id }) { s ->
                val balFlow = remember(s.id) { vm.repo.observeSavingsBalance(s.id) }
                val mv by balFlow.collectAsState(initial = 0L)
                val balance = s.openingBalancePoisha + mv
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(s.name, style = MaterialTheme.typography.titleMedium); MoneyText(balance) }
                        if (s.purpose.isNotBlank()) Text("উদ্দেশ্য: ${s.purpose}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row {
                            TextButton(onClick = { moveFor = s }) { Text("জমা / উত্তোলন") }
                            TextButton(onClick = { vm.run("সঞ্চয় হালনাগাদ হয়েছে।") { vm.repo.updateSavings(s.copy(archived = !s.archived)) } }) { Text(if (s.archived) "পুনরায় চালু" else "আর্কাইভ") }
                        }
                    }
                }
            }
        }
    }
    if (showAdd) {
        var name by remember { mutableStateOf("") }; var inst by remember { mutableStateOf("") }; var purpose by remember { mutableStateOf("") }; var opening by remember { mutableStateOf("") }; var err by remember { mutableStateOf<String?>(null) }
        AlertDialog(onDismissRequest = { showAdd = false }, title = { Text("নতুন সঞ্চয় অ্যাকাউন্ট") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField("নাম * (যেমন: জরুরি তহবিল)", name, { name = it }); LabeledField("প্রতিষ্ঠান/স্থান", inst, { inst = it }); LabeledField("উদ্দেশ্য", purpose, { purpose = it })
                LabeledField("শুরুর ব্যালেন্স (টাকা)", opening, { opening = it }, keyboard = KeyboardType.Decimal)
                err?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            } },
            confirmButton = { Button(onClick = {
                try { if (name.isBlank()) err = "নাম আবশ্যক।" else { vm.run("সঞ্চয় অ্যাকাউন্ট তৈরি হয়েছে।") { vm.repo.createSavings(SavingsAccount(name = name.trim(), institution = inst.trim(), purpose = purpose.trim(), openingBalancePoisha = if (opening.isBlank()) 0 else Money.parsePoisha(opening))) }; showAdd = false } }
                catch (e: Exception) { err = e.message }
            }) { Text("সংরক্ষণ") } },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("বাতিল") } })
    }
    moveFor?.let { s ->
        var direction by remember(s.id) { mutableStateOf("DEPOSIT") }; var amount by remember(s.id) { mutableStateOf("") }
        var accountId by remember(s.id) { mutableStateOf<Long?>(accounts.firstOrNull()?.id) }; var err by remember(s.id) { mutableStateOf<String?>(null) }
        AlertDialog(onDismissRequest = { moveFor = null }, title = { Text("${s.name} — জমা/উত্তোলন") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SimplePicker("ধরন", listOf("DEPOSIT" to "সঞ্চয়ে জমা", "WITHDRAW" to "সঞ্চয় থেকে উত্তোলন"), direction) { direction = it }
                LabeledField("পরিমাণ (টাকা) *", amount, { amount = it }, keyboard = KeyboardType.Decimal)
                AccountPicker(if (direction == "DEPOSIT") "যে অ্যাকাউন্ট থেকে যাবে" else "যে অ্যাকাউন্টে আসবে", accounts, accountId) { accountId = it }
                err?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            } },
            confirmButton = { Button(onClick = {
                try { val amt = Money.parsePoisha(amount); val acc = accountId ?: throw IllegalArgumentException("অ্যাকাউন্ট নির্বাচন করুন।")
                    vm.run("সঞ্চয় লেনদেন সংরক্ষণ হয়েছে।") { vm.repo.savingsMovement(s.id, direction, amt, System.currentTimeMillis(), acc, "") }; moveFor = null
                } catch (e: Exception) { err = e.message }
            }) { Text("সংরক্ষণ") } },
            dismissButton = { TextButton(onClick = { moveFor = null }) { Text("বাতিল") } })
    }
}

@Composable
private fun BudgetsSection(vm: MainViewModel) {
    val budgets by vm.budgets.collectAsState()
    val categories by vm.categories.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    Column {
        Spacer(Modifier.height(8.dp))
        Button(onClick = { showAdd = true }) { Text("নতুন বাজেট") }
        Text("বাজেটে শুধু প্রকৃত ব্যয় গণনা হয় — স্থানান্তর, ঋণ পরিশোধ ও সঞ্চয় গণনা হয় না। দোকানের বাকিতে কেনা পণ্য কেনার তারিখেই ব্যয় ধরা হয়; পরে পেমেন্টে আবার ধরা হয় না।", style = MaterialTheme.typography.bodySmall)
        if (budgets.isEmpty()) EmptyState("কোনো বাজেট নেই।", "বাজেট তৈরি করুন") { showAdd = true }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(budgets, key = { it.id }) { b ->
                var used by remember(b.id) { mutableStateOf(0L) }
                LaunchedEffect(b.id) { used = vm.repo.expenseForCategory(b.categoryId, b.startAt, b.endAt) }
                val pct = FinanceCalculator.budgetUsedPercent(b.limitPoisha, used)
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(b.name, style = MaterialTheme.typography.titleMedium)
                        Text("${Dates.format(b.startAt)} — ${Dates.format(b.endAt)}")
                        Text("বাজেট ${Money.formatPoisha(b.limitPoisha)} • ব্যয় ${Money.formatPoisha(used)} • বাকি ${Money.formatPoisha((b.limitPoisha - used))} • ${Money.toBengaliDigits(pct.toString())}%")
                        LinearProgressIndicator(progress = { (pct / 100f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                        if (used > b.limitPoisha) Text("বাজেট অতিক্রম হয়েছে!", color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = { vm.run("বাজেট মোছে ফেলা হয়েছে।") { vm.repo.deleteBudget(b.id) } }) { Text("বাজেট মুছুন") }
                    }
                }
            }
        }
    }
    if (showAdd) {
        var name by remember { mutableStateOf("") }; var limit by remember { mutableStateOf("") }
        var categoryId by remember { mutableStateOf<Long?>(null) }; var err by remember { mutableStateOf<String?>(null) }
        AlertDialog(onDismissRequest = { showAdd = false }, title = { Text("নতুন বাজেট (এই মাস)") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField("বাজেটের নাম *", name, { name = it })
                LabeledField("সীমা (টাকা) *", limit, { limit = it }, keyboard = KeyboardType.Decimal)
                CategoryPicker("ক্যাটাগরি (খালি = সব ব্যয়)", categories.filter { it.kind == "EXPENSE" }, categoryId) { categoryId = it }
                err?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            } },
            confirmButton = { Button(onClick = {
                try { val lim = Money.parsePoisha(limit); if (name.isBlank()) err = "নাম আবশ্যক।"
                    else { val (from, to) = Dates.monthRange(Dates.today()); vm.run("বাজেট তৈরি হয়েছে।") { vm.repo.addBudget(Budget(name = name.trim(), startAt = from, endAt = to, limitPoisha = lim, categoryId = categoryId)) }; showAdd = false } }
                catch (e: Exception) { err = e.message }
            }) { Text("সংরক্ষণ") } },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("বাতিল") } })
    }
}
