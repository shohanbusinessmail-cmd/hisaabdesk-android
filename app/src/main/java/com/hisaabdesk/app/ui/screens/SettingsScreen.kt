
package com.hisaabdesk.app.ui.screens

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hisaabdesk.app.core.backup.BackupManager
import com.hisaabdesk.app.core.notifications.ReminderWorker
import com.hisaabdesk.app.ui.MainViewModel
import com.hisaabdesk.app.ui.components.*
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val reminders by vm.reminders.collectAsState()
    var pinSection by remember { mutableStateOf(false) }
    var currentPin by remember { mutableStateOf("") }; var newPin by remember { mutableStateOf("") }; var confirmPin by remember { mutableStateOf("") }
    var pinMsg by remember { mutableStateOf<String?>(null) }
    var backupPassword by remember { mutableStateOf("") }
    var useEncryption by remember { mutableStateOf(true) }
    var restorePassword by remember { mutableStateOf("") }
    var pendingRestoreText by remember { mutableStateOf<String?>(null) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var warnUnencrypted by remember { mutableStateOf(false) }
    var exportText by remember { mutableStateOf<String?>(null) }

    val createDoc = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null && exportText != null) {
            scope.launch {
                try { context.contentResolver.openOutputStream(uri)?.use { it.write(exportText!!.toByteArray(Charsets.UTF_8)) }; vm.run("ব্যাকআপ সংরক্ষণ হয়েছে।") { } }
                catch (e: Exception) { vm.run("ব্যাকআপ সংরক্ষণ করা যায়নি।") { throw e } }
            }
        }
        exportText = null
    }
    val openDoc = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            try {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: throw IllegalArgumentException("ফাইল পড়া যায়নি।")
                pendingRestoreText = text; showRestoreConfirm = true
            } catch (e: Exception) { pinMsg = "ব্যাকআপ ফাইলটি যাচাই করা যায়নি। বর্তমান ডেটা পরিবর্তন করা হয়নি।" }
        }
    }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("সেটিংস", style = MaterialTheme.typography.headlineSmall) }
        item {
            SectionCard("নিরাপত্তা (পিন)") {
                Text("পিন শুধু PBKDF2 হ্যাশ আকারে ফোনে সংরক্ষিত থাকে — আসল পিন কোথাও লেখা থাকে না। পিন ভুলে গেলে সার্ভার থেকে পুনরুদ্ধার সম্ভব নয়; একমাত্র উপায়: আগে নেওয়া ব্যাকআপ নতুন করে রিস্টোর করা (নতুন পিনসহ)।")
                Spacer(Modifier.height(8.dp))
                Button(onClick = { pinSection = !pinSection }) { Text("পিন পরিবর্তন করুন") }
                if (pinSection) {
                    LabeledField("বর্তমান পিন", currentPin, { currentPin = it }, keyboard = KeyboardType.NumberPassword)
                    LabeledField("নতুন পিন (৪–৬ সংখ্যা)", newPin, { newPin = it }, keyboard = KeyboardType.NumberPassword)
                    LabeledField("নতুন পিন আবার লিখুন", confirmPin, { confirmPin = it }, keyboard = KeyboardType.NumberPassword)
                    pinMsg?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Button(onClick = {
                        scope.launch {
                            when {
                                !vm.pinManager.verify(currentPin) -> pinMsg = "বর্তমান পিন সঠিক নয়।"
                                newPin != confirmPin -> pinMsg = "নতুন পিন দুটি মিলছে না।"
                                newPin.length !in 4..6 || !newPin.all { it.isDigit() } -> pinMsg = "পিন ৪–৬ সংখ্যার হতে হবে।"
                                else -> { vm.pinManager.setPin(newPin); pinMsg = null; currentPin = ""; newPin = ""; confirmPin = ""; vm.run("পিন পরিবর্তন হয়েছে।") {} }
                            }
                        }
                    }) { Text("পিন সংরক্ষণ করুন") }
                }
                Text("ডাটাবেস সুরক্ষা: এই সংস্করণে ডাটাবেস আলাদাভাবে এনক্রিপ্ট করা নয়; ফোনের নিজস্ব এনক্রিপশন ও অ্যাপ পিন আপনার তথ্য সুরক্ষা দেয়। এনক্রিপ্ট করা ব্যাকআপ ব্যবহার করুন।", style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            SectionCard("ব্যাকআপ ও পুনরুদ্ধার") {
                Text("সম্পূর্ণ ব্যাকআপে সব হিসাব অন্তর্ভুক্ত থাকে। পিন কখনো ব্যাকআপে যায় না।")
                Row { Checkbox(useEncryption, { useEncryption = it }); Text("পাসওয়ার্ড দিয়ে এনক্রিপ্ট করুন (প্রস্তাবিত)") }
                if (useEncryption) LabeledField("ব্যাকআপ পাসওয়ার্ড", backupPassword, { backupPassword = it }, keyboard = KeyboardType.Password)
                Button(onClick = {
                    scope.launch {
                        try {
                            val json = vm.backupManager.exportJson()
                            exportText = if (useEncryption) {
                                require(backupPassword.length >= 6) { "এনক্রিপ্ট করা ব্যাকআপের জন্য অন্তত ৬ অক্ষরের পাসওয়ার্ড দিন।" }
                                BackupManager.encrypt(json, backupPassword)
                            } else json
                            if (!useEncryption) warnUnencrypted = true else createDoc.launch("hisaabdesk-backup.json")
                        } catch (e: Exception) { pinMsg = e.message }
                    }
                }) { Text("ব্যাকআপ ফাইল তৈরি করুন") }
                Spacer(Modifier.height(8.dp))
                LabeledField("রিস্টোরের পাসওয়ার্ড (এনক্রিপ্ট করা ফাইলের জন্য)", restorePassword, { restorePassword = it }, keyboard = KeyboardType.Password)
                OutlinedButton(onClick = { openDoc.launch(arrayOf("application/json", "text/plain", "*/*")) }) { Text("ব্যাকআপ ফাইল বেছে পুনরুদ্ধার করুন") }
                pinMsg?.let { Spacer(Modifier.height(4.dp)); Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
        item {
            SectionCard("রিমাইন্ডার") {
                Text("Android ব্যাটারি সাশ্রয়ের কারণে রিমাইন্ডার কিছুটা দেরিতে আসতে পারে — ঠিক সময়ের গ্যারান্টি দেওয়া যায় না। নোটিফিকেশনে টাকার পরিমাণ দেখানো হয় না।")
                if (reminders.isEmpty()) Text("কোনো রিমাইন্ডার নেই। নিচে যোগ করুন।")
                reminders.forEach { r ->
                    ListItem(headlineContent = { Text(r.title) }, supportingContent = { Text("${com.hisaabdesk.app.core.Dates.format(r.dueAt)} • ${if (r.enabled) "চালু" else "বন্ধ"}") },
                        trailingContent = { TextButton(onClick = { vm.run("রিমাইন্ডার মুছে ফেলা হয়েছে।") { vm.repo.deleteReminder(r.id) } }) { Text("মুছুন") } })
                }
                var title by remember { mutableStateOf("") }
                LabeledField("নতুন রিমাইন্ডারের নাম", title, { title = it })
                Button(onClick = {
                    if (title.isNotBlank()) {
                        val due = System.currentTimeMillis() + 24L * 3600 * 1000
                        vm.run("রিমাইন্ডার যোগ হয়েছে (আগামীকাল)।") {
                            vm.repo.addReminder(com.hisaabdesk.app.data.local.entity.ScheduledReminder(title = title.trim(), kind = "GENERAL", dueAt = due))
                            ReminderWorker.schedule(context, title.trim(), 24L * 3600 * 1000)
                        }; title = ""
                    }
                }) { Text("আগামীকালের জন্য রিমাইন্ডার যোগ করুন") }
                // notification permission
                val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
                OutlinedButton(onClick = { if (android.os.Build.VERSION.SDK_INT >= 33) permLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS) }) { Text("নোটিফিকেশনের অনুমতি দিন") }
            }
        }
    }

    if (warnUnencrypted) {
        AlertDialog(onDismissRequest = { warnUnencrypted = false }, title = { Text("সতর্কতা: এনক্রিপ্টবিহীন ব্যাকআপ") },
            text = { Text("এই ফাইলে আপনার আর্থিক তথ্য পড়া যায় এমন অবস্থায় থাকবে। ফাইলটি শেয়ার বা ক্লাউডে রাখার আগে ঝুঁকি বুঝে নিন।") },
            confirmButton = { Button(onClick = { warnUnencrypted = false; createDoc.launch("hisaabdesk-backup.json") }) { Text("বুঝেছি, তৈরি করুন") } },
            dismissButton = { TextButton(onClick = { warnUnencrypted = false; exportText = null }) { Text("বাতিল") } })
    }
    if (showRestoreConfirm && pendingRestoreText != null) {
        AlertDialog(onDismissRequest = { showRestoreConfirm = false }, title = { Text("ব্যাকআপ পুনরুদ্ধার করবেন?") },
            text = { Text("পুনরুদ্ধার করলে বর্তমান সব হিসাব ব্যাকআপের হিসাব দিয়ে প্রতিস্থাপিত হবে। ফাইল যাচাই ব্যর্থ হলে বর্তমান ডেটা অপরিবর্তিত থাকবে। এনক্রিপ্ট করা ফাইলের জন্য উপরে পাসওয়ার্ড লিখে থাকতে হবে।") },
            confirmButton = { Button(onClick = {
                val raw = pendingRestoreText!!
                scope.launch {
                    try {
                        val json = if (BackupManager.isEncrypted(raw)) {
                            require(restorePassword.isNotBlank()) { "এনক্রিপ্ট করা ব্যাকআপের পাসওয়ার্ড দিন।" }
                            BackupManager.decrypt(raw, restorePassword)
                        } else raw
                        vm.backupManager.restore(json)
                        vm.run("পুনরুদ্ধার সম্পন্ন হয়েছে।") {}
                    } catch (e: Exception) { pinMsg = e.message ?: "ব্যাকআপ ফাইলটি যাচাই করা যায়নি। বর্তমান ডেটা পরিবর্তন করা হয়নি।" }
                }
                showRestoreConfirm = false; pendingRestoreText = null
            }) { Text("পুনরুদ্ধার করুন") } },
            dismissButton = { TextButton(onClick = { showRestoreConfirm = false }) { Text("বাতিল") } })
    }
}
