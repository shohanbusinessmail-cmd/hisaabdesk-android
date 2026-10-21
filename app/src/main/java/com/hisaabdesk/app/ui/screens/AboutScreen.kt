
package com.hisaabdesk.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hisaabdesk.app.BuildConfig
import com.hisaabdesk.app.ui.components.SectionCard

@Composable
fun AboutScreen() {
    val context = LocalContext.current
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("সম্পর্কে", style = MaterialTheme.typography.headlineSmall) }
        item {
            SectionCard("HisaabDesk (হিসাবডেস্ক)") {
                Text("বাংলাদেশের ব্যবহারকারীদের জন্য তৈরি সম্পূর্ণ অফলাইন ব্যক্তিগত ও পারিবারিক আর্থিক হিসাবের অ্যাপ। আপনার সব তথ্য শুধু এই ফোনেই থাকে — কোনো সার্ভারে পাঠানো হয় না, কোনো অ্যাকাউন্ট লাগে না।")
                Spacer(Modifier.height(8.dp))
                Text("ডেভেলপার: Shohan Khan")
                Text("সংস্করণ: ${BuildConfig.VERSION_NAME} (কোড ${BuildConfig.VERSION_CODE})")
                Text("প্যাকেজ: ${BuildConfig.APPLICATION_ID}")
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:[email-address]"))
                    context.startActivity(intent)
                }) { Text("ইমেইল করুন: [email-address]") }
            }
        }
        item {
            SectionCard("গোপনীয়তা ও ডেটা") {
                Text("• সব আর্থিক তথ্য ফোনের লোকাল ডাটাবেসে থাকে।\n• অ্যাপ ইন্টারনেট ছাড়াই সম্পূর্ণ কাজ করে।\n• পিন PBKDF2 হ্যাশ আকারে থাকে; পিন ভুলে গেলে ব্যাকআপ ছাড়া পুনরুদ্ধার সম্ভব নয়।\n• নিয়মিত এনক্রিপ্ট করা ব্যাকআপ নিয়ে নিরাপদ স্থানে রাখুন।\n• এই অ্যাপ কোনো আর্থিক প্রতিষ্ঠানের সার্টিফিকেশন দাবি করে না।")
            }
        }
        item {
            SectionCard("ওপেন সোর্স লাইসেন্স") {
                Text("এই অ্যাপে ব্যবহৃত প্রধান ওপেন-সোর্স লাইব্রেরি: Jetpack Compose, Material 3, Room, Navigation Compose, DataStore, WorkManager, AndroidX Biometric (Apache License 2.0), Kotlin Coroutines (Apache 2.0)। প্রতিটি লাইব্রেরির পূর্ণ লাইসেন্স তাদের নিজ নিজ প্রকল্পে পাওয়া যাবে।")
            }
        }
    }
}
