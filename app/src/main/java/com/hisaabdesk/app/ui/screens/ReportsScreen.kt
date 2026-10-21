
package com.hisaabdesk.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hisaabdesk.app.core.Dates
import com.hisaabdesk.app.core.Money
import com.hisaabdesk.app.ui.MainViewModel
import com.hisaabdesk.app.ui.components.*
import java.time.LocalDate

@Composable
fun ReportsScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val accounts by vm.accounts.collectAsState()
    val categories by vm.categories.collectAsState()
    var rangeLabel by remember { mutableStateOf("MONTH") }
    val today = Dates.today()
    val (from, to) = when (rangeLabel) {
        "TODAY" -> Dates.startOfDayMillis(today) to Dates.endOfDayMillis(today)
        "WEEK" -> Dates.startOfDayMillis(today.minusDays(6)) to Dates.endOfDayMillis(today)
        "YEAR" -> Dates.startOfDayMillis(today.withDayOfYear(1)) to Dates.endOfDayMillis(today.withMonth(12).withDayOfMonth(31))
        else -> Dates.monthRange(today)
    }
    val incomeFlow = remember(from, to) { vm.repo.observeIncome(from, to) }
    val expenseFlow = remember(from, to) { vm.repo.observeExpense(from, to) }
    val income by incomeFlow.collectAsState(initial = 0L)
    val expense by expenseFlow.collectAsState(initial = 0L)
    val rangeFlow = remember(from, to) { vm.repo.observeLedgerRange(from, to) }
    val entries by rangeFlow.collectAsState(initial = emptyList())

    // category-wise expense from real entries
    val byCategory = remember(entries, categories) {
        entries.filter { it.type == "EXPENSE" }.groupBy { it.categoryId }
            .map { (catId, list) -> (categories.firstOrNull { it.id == catId }?.name ?: "ক্যাটাগরি ছাড়া") to list.sumOf { -it.amountPoisha } }
            .sortedByDescending { it.second }
    }
    val maxCat = byCategory.maxOfOrNull { it.second } ?: 1L

    var accountBalances by remember { mutableStateOf<List<Pair<String, Long>>>(emptyList()) }
    LaunchedEffect(accounts) {
        accountBalances = accounts.filter { !it.archived }.map { it.name to vm.repo.accountBalanceOnce(it.id) }
    }
    val shopOut by vm.totalShopOutstanding.collectAsState()

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("রিপোর্ট", style = MaterialTheme.typography.headlineSmall) }
        item {
            SimplePicker("সময়সীমা", listOf("TODAY" to "আজ", "WEEK" to "গত ৭ দিন", "MONTH" to "এই মাস", "YEAR" to "এই বছর"), rangeLabel) { rangeLabel = it }
        }
        item {
            SectionCard("আয় বনাম ব্যয় (${Dates.format(from)} — ${Dates.format(to)})") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("মোট আয়"); MoneyText(income) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("মোট ব্যয়"); MoneyText(expense) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("পার্থক্য (আয় − ব্যয়)"); MoneyText(income - expense) }
                Text("স্থানান্তর, সঞ্চয় চলাচল ও ঋণের মূলধন আয়/ব্যয়ে ধরা হয়নি।", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    val text = buildString {
                        appendLine("হিসাবডেস্ক রিপোর্ট (${Dates.format(from)} — ${Dates.format(to)})")
                        appendLine("আয়: ${Money.formatPoisha(income)}"); appendLine("ব্যয়: ${Money.formatPoisha(expense)}"); appendLine("পার্থক্য: ${Money.formatPoisha(income - expense)}")
                        appendLine("ক্যাটাগরিভিত্তিক ব্যয়:"); byCategory.forEach { appendLine("- ${it.first}: ${Money.formatPoisha(it.second)}") }
                    }
                    val intent = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
                    context.startActivity(Intent.createChooser(intent, "রিপোর্ট শেয়ার করুন"))
                }) { Text("রিপোর্ট শেয়ার করুন") }
            }
        }
        item {
            SectionCard("ক্যাটাগরিভিত্তিক ব্যয়") {
                if (byCategory.isEmpty()) Text("এই সময়ে কোনো ব্যয় নেই।")
                byCategory.forEach { (name, amt) ->
                    Text("$name — ${Money.formatPoisha(amt)}")
                    LinearProgressIndicator(progress = { amt.toFloat() / maxCat.toFloat() }, modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp))
                }
            }
        }
        item {
            SectionCard("অ্যাকাউন্ট ব্যালেন্স") {
                if (accountBalances.isEmpty()) Text("কোনো অ্যাকাউন্ট নেই।")
                accountBalances.forEach { (name, bal) -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(name); MoneyText(bal) } }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("দোকানের মোট বকেয়া (দায়)"); MoneyText(shopOut) }
            }
        }
        item { Text("এই সময়ের লেনদেন (${Money.toBengaliDigits(entries.size.toString())}টি)", style = MaterialTheme.typography.titleMedium) }
        items(entries.take(100), key = { it.id }) { e ->
            ListItem(headlineContent = { Text("${typeLabelBn(e.type)} — ${e.description.ifBlank { e.counterparty }}") },
                supportingContent = { Text(Dates.format(e.occurredAt)) }, trailingContent = { MoneyText(e.amountPoisha) })
        }
    }
}
