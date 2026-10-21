
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
import com.hisaabdesk.app.data.local.entity.*
import com.hisaabdesk.app.ui.MainViewModel
import com.hisaabdesk.app.ui.components.*

@Composable
fun ShopsScreen(vm: MainViewModel) {
    val shops by vm.shops.collectAsState()
    val accounts by vm.accounts.collectAsState()
    val totalOut by vm.totalShopOutstanding.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Shop?>(null) }

    Scaffold(floatingActionButton = { FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, contentDescription = "নতুন দোকান") } }) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text("দোকানের বাকি", style = MaterialTheme.typography.headlineSmall) }
            item { SectionCard("সব দোকানে মোট বকেয়া") { MoneyText(totalOut, style = MaterialTheme.typography.headlineSmall) } }
            if (shops.isEmpty()) item { EmptyState("এখনও কোনো দোকান যোগ করা হয়নি।", "দোকান যোগ করুন") { showAdd = true } }
            items(shops, key = { it.id }) { s ->
                val purchasedFlow = remember(s.id) { vm.repo.observeShopPurchased(s.id) }
                val paidFlow = remember(s.id) { vm.repo.observeShopPaid(s.id) }
                val purchased by purchasedFlow.collectAsState(initial = 0L)
                val paid by paidFlow.collectAsState(initial = 0L)
                val outstanding = (purchased - paid).coerceAtLeast(0)
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(s.name + if (s.archived) " (আর্কাইভ)" else "", style = MaterialTheme.typography.titleMedium); MoneyText(outstanding) }
                        if (s.keeperName.isNotBlank()) Text("দোকানদার: ${s.keeperName}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row {
                            TextButton(onClick = { selected = s }) { Text("বিস্তারিত, কেনাকাটা ও পেমেন্ট") }
                            TextButton(onClick = { vm.run("দোকান হালনাগাদ হয়েছে।") { vm.repo.updateShop(s.copy(archived = !s.archived)) } }) { Text(if (s.archived) "পুনরায় চালু" else "আর্কাইভ") }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        var name by remember { mutableStateOf("") }; var keeper by remember { mutableStateOf("") }; var phone by remember { mutableStateOf("") }; var address by remember { mutableStateOf("") }; var err by remember { mutableStateOf<String?>(null) }
        AlertDialog(onDismissRequest = { showAdd = false }, title = { Text("নতুন দোকান") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField("দোকানের নাম *", name, { name = it }); LabeledField("দোকানদারের নাম", keeper, { keeper = it })
                LabeledField("ফোন", phone, { phone = it }, keyboard = KeyboardType.Phone); LabeledField("ঠিকানা", address, { address = it })
                err?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            } },
            confirmButton = { Button(onClick = { if (name.isBlank()) err = "দোকানের নাম আবশ্যক।" else { vm.run("দোকান যোগ হয়েছে।") { vm.repo.addShop(Shop(name = name.trim(), keeperName = keeper.trim(), phone = phone.trim(), address = address.trim())) }; showAdd = false } }) { Text("সংরক্ষণ") } },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("বাতিল") } })
    }

    selected?.let { shop -> ShopDetailDialog(vm, shop) { selected = null } }
}

@Composable
private fun ShopDetailDialog(vm: MainViewModel, shop: Shop, onClose: () -> Unit) {
    val accounts by vm.accounts.collectAsState()
    val purchasesFlow = remember(shop.id) { vm.repo.observeShopPurchases(shop.id) }
    val paymentsFlow = remember(shop.id) { vm.repo.observeShopPayments(shop.id) }
    val purchases by purchasesFlow.collectAsState(initial = emptyList())
    val payments by paymentsFlow.collectAsState(initial = emptyList())
    val purchasedFlow = remember(shop.id) { vm.repo.observeShopPurchased(shop.id) }
    val paidFlow = remember(shop.id) { vm.repo.observeShopPaid(shop.id) }
    val purchased by purchasedFlow.collectAsState(initial = 0L)
    val paid by paidFlow.collectAsState(initial = 0L)
    val outstanding = (purchased - paid).coerceAtLeast(0)
    var showPurchase by remember { mutableStateOf(false) }
    var showPay by remember { mutableStateOf(false) }

    AlertDialog(onDismissRequest = onClose, title = { Text(shop.name) },
        text = { LazyColumn(Modifier.heightIn(max = 480.dp)) {
            item { Text("বকেয়া: ${Money.formatPoisha(outstanding)}", style = MaterialTheme.typography.titleMedium) }
            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showPurchase = true }) { Text("কেনাকাটা যোগ") }
                Button(onClick = { showPay = true }) { Text("পেমেন্ট") }
            } }
            item { Text("কেনাকাটার ইতিহাস", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp)) }
            if (purchases.isEmpty()) item { Text("এখনও কোনো কেনাকাটা নেই।") }
            items(purchases, key = { it.id }) { p ->
                ListItem(headlineContent = { Text("মোট ${Money.formatPoisha(p.totalPoisha)} • নগদ ${Money.formatPoisha(p.paidNowPoisha)} • বাকি ${Money.formatPoisha(p.totalPoisha - p.paidNowPoisha)}") },
                    supportingContent = { Text(Dates.format(p.purchasedAt) + if (p.notes.isNotBlank()) " • ${p.notes}" else "") })
            }
            item { Text("পেমেন্টের ইতিহাস", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp)) }
            if (payments.isEmpty()) item { Text("এখনও কোনো পেমেন্ট নেই।") }
            items(payments, key = { it.id }) { p ->
                ListItem(headlineContent = { Text(Money.formatPoisha(p.amountPoisha)) }, supportingContent = { Text("${Dates.format(p.paidAt)} ${p.method}") })
            }
        } },
        confirmButton = { TextButton(onClick = onClose) { Text("বন্ধ করুন") } })

    if (showPurchase) {
        // items as lines: "পণ্যের নাম | পরিমাণ | একক মূল্য" per line — simple, precise, no fake data
        var lines by remember { mutableStateOf("") }
        var paidNow by remember { mutableStateOf("") }
        var accountId by remember { mutableStateOf<Long?>(accounts.firstOrNull()?.id) }
        var err by remember { mutableStateOf<String?>(null) }
        AlertDialog(onDismissRequest = { showPurchase = false }, title = { Text("কেনাকাটা যোগ করুন") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("প্রতি লাইনে: পণ্যের নাম | পরিমাণ | দাম (টাকা)\nউদাহরণ: চাল | ৫ কেজি | ৩২০", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(value = lines, onValueChange = { lines = it }, label = { Text("পণ্যের তালিকা *") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                LabeledField("এখন নগদ পরিশোধ (টাকা)", paidNow, { paidNow = it }, keyboard = KeyboardType.Decimal)
                AccountPicker("নগদ পেমেন্টের অ্যাকাউন্ট", accounts, accountId) { accountId = it }
                err?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            } },
            confirmButton = { Button(onClick = {
                try {
                    val items = lines.lines().filter { it.isNotBlank() }.map { line ->
                        val parts = line.split("|").map { it.trim() }
                        require(parts.size == 3) { "প্রতি লাইন ‘নাম | পরিমাণ | দাম’ আকারে লিখুন।" }
                        val unit = Money.parsePoisha(parts[2])
                        // quantity may be like "৫ কেজি" or numeric; line total = unit price * numeric qty when numeric, else unit price is treated as line total when qty has unit text — documented: দাম column = মোট লাইন মূল্য if পরিমাণে একক থাকে
                        val qtyNum = parts[1].filter { it.isDigit() || it == '.' }
                        val lineTotal = if (qtyNum.isNotEmpty() && parts[1].trim() == qtyNum) {
                            val q = qtyNum.toBigDecimal(); (unit.toBigDecimal() * q).toLong()
                        } else unit
                        ShopPurchaseItem(purchaseId = 0, productName = parts[0], quantityText = parts[1], unitPricePoisha = unit, lineTotalPoisha = lineTotal)
                    }
                    require(items.isNotEmpty()) { "অন্তত একটি পণ্য যোগ করুন।" }
                    val paid = if (paidNow.isBlank()) 0L else Money.parsePoisha(paidNow)
                    vm.run("কেনাকাটা সংরক্ষণ হয়েছে।") { vm.repo.recordShopPurchase(shop.id, System.currentTimeMillis(), items, paid, if (paid > 0) accountId else null, null, "") }
                    showPurchase = false
                } catch (e: Exception) { err = e.message }
            }) { Text("সংরক্ষণ") } },
            dismissButton = { TextButton(onClick = { showPurchase = false }) { Text("বাতিল") } })
    }

    if (showPay) {
        var amount by remember { mutableStateOf("") }; var accountId by remember { mutableStateOf<Long?>(accounts.firstOrNull()?.id) }
        var method by remember { mutableStateOf("নগদ") }; var err by remember { mutableStateOf<String?>(null) }
        AlertDialog(onDismissRequest = { showPay = false }, title = { Text("দোকানের পেমেন্ট") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("বর্তমান বকেয়া: ${Money.formatPoisha(outstanding)}")
                LabeledField("পেমেন্টের পরিমাণ (টাকা) *", amount, { amount = it }, keyboard = KeyboardType.Decimal)
                AccountPicker("যে অ্যাকাউন্ট থেকে", accounts, accountId) { accountId = it }
                LabeledField("মাধ্যম", method, { method = it })
                Text("এই পেমেন্ট নতুন ব্যয় হিসেবে গণনা হবে না — শুধু বকেয়া কমবে।", style = MaterialTheme.typography.bodySmall)
                err?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            } },
            confirmButton = { Button(onClick = {
                try { val amt = Money.parsePoisha(amount); val acc = accountId ?: throw IllegalArgumentException("অ্যাকাউন্ট নির্বাচন করুন।")
                    vm.run("পেমেন্ট সংরক্ষণ হয়েছে।") { vm.repo.payShop(shop.id, amt, System.currentTimeMillis(), acc, method.trim(), "") }; showPay = false
                } catch (e: Exception) { err = e.message }
            }) { Text("পেমেন্ট করুন") } },
            dismissButton = { TextButton(onClick = { showPay = false }) { Text("বাতিল") } })
    }
}
