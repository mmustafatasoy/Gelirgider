package com.eymen.gelir.reminders

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.eymen.gelir.MainActivity
import com.eymen.gelir.data.FinanceDatabase
import com.eymen.gelir.domain.ReminderRules
import java.time.LocalDate
import java.time.YearMonth
import java.util.concurrent.TimeUnit

/** Android may defer a daily worker; notification exact timing is not guaranteed. */
object ReminderScheduler {
    private const val PERIODIC_NAME = "eymen-gelir-payment-reminders"
    private const val ONE_TIME_NAME = "eymen-gelir-payment-check-now"

    fun ensureScheduled(context: Context) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<PaymentReminderWorker>(24, TimeUnit.HOURS).build()
        )
    }

    fun checkNow(context: Context) {
        ensureScheduled(context)
        WorkManager.getInstance(context).enqueueUniqueWork(
            ONE_TIME_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<PaymentReminderWorker>().build()
        )
    }
}

class PaymentReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return Result.success()
        }
        return try {
            val notifications = context.getSystemService(NotificationManager::class.java)
            val channelId = "eymen_gelir_fatura_hatirlatma"
            notifications.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "Fatura ve aidat hatırlatmaları",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = "Seçilen ödeme günleri için hatırlatıcı" }
            )
            val today = LocalDate.now()
            val currentMonth = YearMonth.from(today).toString()
            val dao = FinanceDatabase.get(context).financeDao()
            dao.allPaymentReminders().filter { it.enabled }.forEach { reminder ->
                if (!ReminderRules.shouldNotify(
                        today, reminder.dayOfMonth, reminder.lastNotifiedMonth
                    )) return@forEach
                val due = ReminderRules.dueDate(YearMonth.from(today), reminder.dayOfMonth)
                val activityIntent = Intent(context, MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    reminder.id.toInt(),
                    activityIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val message = reminder.category + " — " + reminder.title +
                    " · Son ödeme: " + due.dayOfMonth + "/" + due.monthValue
                val notification = Notification.Builder(context, channelId)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("EYMEN GELİR · Ödeme hatırlatması")
                    .setContentText(message)
                    .setStyle(Notification.BigTextStyle().bigText(message))
                    .setCategory(Notification.CATEGORY_REMINDER)
                    .setVisibility(Notification.VISIBILITY_PRIVATE)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .build()
                // Idempotent database claim prevents duplicate reminders in concurrent runs.
                if (dao.markReminderNotified(reminder.id, currentMonth) == 1) {
                    notifications.notify(reminder.id.toInt(), notification)
                }
            }
            Result.success()
        } catch (error: SecurityException) {
            // Permission may be revoked between the initial check and notification.
            Result.success()
        } catch (error: Exception) {
            if (runAttemptCount < 2) Result.retry() else Result.failure()
        }
    }
}
