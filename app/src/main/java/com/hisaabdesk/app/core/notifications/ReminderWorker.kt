
package com.hisaabdesk.app.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.hisaabdesk.app.R
import java.util.concurrent.TimeUnit

/** Generic reminder notifications — no amounts shown by default (privacy). */
class ReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val title = inputData.getString("title") ?: "হিসাবডেস্ক রিমাইন্ডার"
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel("reminders", "রিমাইন্ডার", NotificationManager.IMPORTANCE_DEFAULT))
        val n = NotificationCompat.Builder(applicationContext, "reminders")
            .setSmallIcon(R.drawable.ic_launcher_foreground).setContentTitle(title)
            .setContentText("একটি নির্ধারিত পেমেন্ট বা কাজের সময় হয়েছে। অ্যাপ খুলে বিস্তারিত দেখুন।")
            .setAutoCancel(true).build()
        nm.notify(title.hashCode(), n)
        return Result.success()
    }
    companion object {
        fun schedule(context: Context, title: String, delayMillis: Long) {
            val req = OneTimeWorkRequestBuilder<ReminderWorker>().setInitialDelay(delayMillis.coerceAtLeast(0), TimeUnit.MILLISECONDS)
                .setInputData(workDataOf("title" to title)).addTag("reminder").build()
            WorkManager.getInstance(context).enqueue(req)
        }
    }
}
