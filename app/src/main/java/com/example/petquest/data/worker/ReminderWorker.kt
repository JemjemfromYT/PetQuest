// ============================================================
// FILE: app/src/main/java/com/example/petquest/worker/ReminderWorker.kt
//       (NEW FILE — create this file and the worker/ package)
// ============================================================

package com.example.petquest.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.example.petquest.MainActivity
import com.example.petquest.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

class ReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME    = "petquest_daily_reminder"
        const val CHANNEL_ID   = "petquest_reminder_channel"
        const val CHANNEL_NAME = "Daily Pet Reminders"
        const val NOTIFICATION_ID = 1001

        /**
         * Schedule (or reschedule) a one-time work request that fires at the
         * given hour:minute. If a request with the same name already exists it
         * is replaced, so calling this after a time change is safe.
         */
        fun schedule(context: Context, hour: Int, minute: Int) {
            val delay = calculateDelayMs(hour, minute)
            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .addTag(WORK_NAME)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }

        /**
         * Cancel any pending reminder work. Call when the user disables
         * notifications.
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        /**
         * Returns milliseconds until the next occurrence of hour:minute.
         * If that time has already passed today, schedules for tomorrow.
         */
        fun calculateDelayMs(hour: Int, minute: Int): Long {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (!target.after(now)) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
            return target.timeInMillis - now.timeInMillis
        }
    }

    override suspend fun doWork(): Result {
        val prefsRepo = UserPreferencesRepository(applicationContext)

        // Double-check the toggle — user may have turned it off between scheduling
        val enabled = prefsRepo.notificationsEnabled.first()
        if (!enabled) return Result.success()

        // Ensure the channel exists (idempotent on API 26+)
        createNotificationChannel()

        // Show the notification
        showNotification()

        // Reschedule for the same time tomorrow
        val hour   = prefsRepo.notificationHour.first()
        val minute = prefsRepo.notificationMinute.first()
        schedule(applicationContext, hour, minute)

        return Result.success()
    }

    // ─── Private helpers ──────────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminds you to complete your daily pet care tasks"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun showNotification() {
        // Tapping the notification opens MainActivity (lands on Home tab)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("🐾 PetQuest — Daily Reminder")
            .setContentText("Your pets are waiting! Complete today's care tasks.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Your pets are waiting! Open PetQuest to complete today's care tasks and keep your streak going. 🔥")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // NotificationManagerCompat checks POST_NOTIFICATIONS permission internally
        // on API 33+. If permission was revoked after scheduling, the call is silently
        // ignored — the worker still runs and reschedules for the next day.
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}
