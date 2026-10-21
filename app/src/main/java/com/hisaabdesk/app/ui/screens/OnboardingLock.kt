
package com.hisaabdesk.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hisaabdesk.app.core.Money
import com.hisaabdesk.app.data.local.entity.FinancialAccount
import com.hisaabdesk.app.ui.MainViewModel
import com.hisaabdesk.app.ui.components.LabeledField
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(vm: MainViewModel, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(0) }
    var pin by remember { mutableStateOf("") }; var confirm by remember { mutableStateOf("") }
    var accName by remember { mutableStateOf("") }; var opening by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("হিসাবডেস্কে স্বাগতম", style = MaterialTheme.typography.headlineSmall)
        when (step) {
            0 -> {
                Text("হিসাবডেস্ক আপনার ব্যক্তিগত ও পারিবারিক আয়ের-ব্যয়, দোকানের বাকি, ধার-দেনা, ঋণ ও সঞ্চয়ের হিসাব রাখার অ্যাপ।")
                Text("আপনার সব তথ্য শুধু এই ফোনেই সংরক্ষিত থাকে। ইন্টারনেট ছাড়াই সব কাজ করে। কোনো নমুনা লেনদেন বা কাল্পনিক ব্যালেন্স যোগ করা হবে না — হিসাব শুরু হবে একদম শূন্য থেকে, আপনার আসল তথ্য দিয়ে।")
                Button(onClick = { step = 1 }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("শুরু করুন") }
            }
            1 -> {
                Text("অ্যাপ পিন তৈরি করুন", style = MaterialTheme.typography.titleMedium)
                Text("এই পিন দিয়ে অ্যাপ খুলতে হবে। পিন ফোনে শুধু সুরক্ষিত হ্যাশ আকারে থাকে। পিন ভুলে গেলে ব্যাকআপ ছাড়া ফেরার উপায় নেই — তাই পিন মনে রাখুন এবং নিয়মিত এনক্রিপ্ট করা ব্যাকআপ নিন।")
                LabeledField("পিন (৪–৬ সংখ্যা)", pin, { pin = it.filter { c -> c.isDigit() }.take(6) }, keyboard = KeyboardType.NumberPassword)
                LabeledField("পিন আবার লিখুন", confirm, { confirm = it.filter { c -> c.isDigit() }.take(6) }, keyboard = KeyboardType.NumberPassword)
                err?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(onClick = {
                    when {
                        pin.length !in 4..6 -> err = "পিন ৪–৬ সংখ্যার হতে হবে।"
                        pin != confirm -> err = "পিন দুটি মিলছে না। আবার লিখুন।"
                        else -> scope.launch { vm.pinManager.setPin(pin); step = 2 }
                    }
                }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("পিন সংরক্ষণ করুন") }
            }
            else -> {
                Text("প্রথম অ্যাকাউন্ট তৈরি করুন", style = MaterialTheme.typography.titleMedium)
                Text("যেমন: ‘নগদ টাকা’। শুরুর ব্যালেন্সে এখন আপনার কাছে আসলে যত টাকা আছে তা লিখুন — এটি আয় হিসেবে গণনা হবে না। চাইলে খালি রেখে পরে যোগ করতে পারবেন।")
                LabeledField("অ্যাকাউন্টের নাম", accName, { accName = it })
                LabeledField("শুরুর ব্যালেন্স (টাকা)", opening, { opening = it }, keyboard = KeyboardType.Decimal)
                err?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(onClick = {
                    scope.launch {
                        try {
                            if (accName.isNotBlank()) {
                                val ob = if (opening.isBlank()) 0L else Money.parsePoisha(opening)
                                vm.repo.createAccount(FinancialAccount(name = accName.trim(), type = "CASH", openingBalancePoisha = ob))
                            }
                            onDone()
                        } catch (e: Exception) { err = e.message }
                    }
                }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("সম্পন্ন — ড্যাশবোর্ডে যান") }
                TextButton(onClick = onDone) { Text("এখন নয়, সরাসরি ড্যাশবোর্ডে যান") }
            }
        }
    }
}

@Composable
fun PinLockScreen(vm: MainViewModel, onUnlocked: () -> Unit) {
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    var attempts by remember { mutableStateOf(0) }
    var lockedUntil by remember { mutableStateOf(0L) }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Spacer(Modifier.height(40.dp))
        Text("হিসাবডেস্ক", style = MaterialTheme.typography.headlineSmall)
        Text("চালিয়ে যেতে আপনার পিন দিন।")
        LabeledField("পিন", pin, { pin = it.filter { c -> c.isDigit() }.take(6) }, keyboard = KeyboardType.NumberPassword)
        err?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = {
            scope.launch {
                val now = System.currentTimeMillis()
                if (now < lockedUntil) { err = "অনেকবার ভুল পিন দেওয়া হয়েছে। কিছুক্ষণ অপেক্ষা করুন।"; return@launch }
                if (vm.pinManager.verify(pin)) { attempts = 0; onUnlocked() }
                else {
                    attempts++
                    val waitSec = minOf(30, 1 shl minOf(attempts, 5))
                    lockedUntil = now + waitSec * 1000L
                    err = "পিন সঠিক নয়। আবার চেষ্টা করুন।"
                    delay(waitSec * 1000L)
                }
            }
        }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("খুলুন") }
        Text("পিন ভুলে গেছেন? সার্ভার না থাকায় দূর থেকে পিন বদলানো যায় না। আগে নেওয়া ব্যাকআপ থাকলে অ্যাপের ডেটা মুছে নতুন করে ইনস্টল করে ব্যাকআপ রিস্টোর করুন (নতুন পিন তৈরি হবে)।", style = MaterialTheme.typography.bodySmall)
    }
}
