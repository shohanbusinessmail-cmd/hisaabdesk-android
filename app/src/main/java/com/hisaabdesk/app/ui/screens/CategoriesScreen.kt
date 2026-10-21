
package com.hisaabdesk.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hisaabdesk.app.data.local.entity.CategorySubcategory
import com.hisaabdesk.app.data.local.entity.TransactionCategory
import com.hisaabdesk.app.ui.MainViewModel
import com.hisaabdesk.app.ui.components.EmptyState
import com.hisaabdesk.app.ui.components.LabeledField
import com.hisaabdesk.app.ui.components.SimplePicker

@Composable
fun CategoriesScreen(vm: MainViewModel) {
    val categories by vm.categories.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var subFor by remember { mutableStateOf<TransactionCategory?>(null) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("ক্যাটাগরি", style = MaterialTheme.typography.headlineSmall)
        Text("কোনো বাধ্যতামূলক ক্যাটাগরি নেই — নিজের পছন্দমতো তৈরি করুন। ব্যবহৃত ক্যাটাগরি মুছে ফেলা হয় না, আর্কাইভ করা হয় যাতে পুরোনো লেনদেনের ইতিহাস অক্ষত থাকে।", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))
        Button(onClick = { showAdd = true }) { Text("নতুন ক্যাটাগরি") }
        Spacer(Modifier.height(8.dp))
        if (categories.isEmpty()) EmptyState("এখনও কোনো ক্যাটাগরি নেই।", "ক্যাটাগরি তৈরি করুন") { showAdd = true }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(categories, key = { it.id }) { c ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${c.name} (${if (c.kind == "INCOME") "আয়" else "ব্যয়"})${if (c.archived) " • আর্কাইভ" else ""}", style = MaterialTheme.typography.titleMedium)
                        Row {
                            TextButton(onClick = { subFor = c }) { Text("সাব-ক্যাটাগরি") }
                            TextButton(onClick = { vm.run("ক্যাটাগরি হালনাগাদ হয়েছে।") { vm.repo.updateCategory(c.copy(archived = !c.archived)) } }) { Text(if (c.archived) "পুনরায় চালু" else "আর্কাইভ") }
                        }
                        SubcategoryList(vm, c.id)
                    }
                }
            }
        }
    }
    if (showAdd) {
        var name by remember { mutableStateOf("") }; var kind by remember { mutableStateOf("EXPENSE") }; var err by remember { mutableStateOf<String?>(null) }
        AlertDialog(onDismissRequest = { showAdd = false }, title = { Text("নতুন ক্যাটাগরি") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField("ক্যাটাগরির নাম *", name, { name = it })
                SimplePicker("ধরন", listOf("INCOME" to "আয়", "EXPENSE" to "ব্যয়"), kind) { kind = it }
                err?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            } },
            confirmButton = { Button(onClick = { if (name.isBlank()) err = "নাম আবশ্যক।" else { vm.run("ক্যাটাগরি তৈরি হয়েছে।") { vm.repo.addCategory(TransactionCategory(name = name.trim(), kind = kind)) }; showAdd = false } }) { Text("সংরক্ষণ") } },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("বাতিল") } })
    }
    subFor?.let { c ->
        var name by remember(c.id) { mutableStateOf("") }
        AlertDialog(onDismissRequest = { subFor = null }, title = { Text("${c.name} — সাব-ক্যাটাগরি যোগ") },
            text = { LabeledField("সাব-ক্যাটাগরির নাম *", name, { name = it }) },
            confirmButton = { Button(onClick = { if (name.isNotBlank()) { vm.run("সাব-ক্যাটাগরি যোগ হয়েছে।") { vm.repo.addSubcategory(CategorySubcategory(categoryId = c.id, name = name.trim())) }; subFor = null } }) { Text("যোগ করুন") } },
            dismissButton = { TextButton(onClick = { subFor = null }) { Text("বাতিল") } })
    }
}

@Composable
private fun SubcategoryList(vm: MainViewModel, categoryId: Long) {
    val flow = remember(categoryId) { vm.repo.observeSubcategories(categoryId) }
    val subs by flow.collectAsState(initial = emptyList())
    if (subs.isNotEmpty()) Text("সাব-ক্যাটাগরি: " + subs.joinToString(", ") { it.name }, style = MaterialTheme.typography.bodySmall)
}
