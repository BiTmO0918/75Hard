package com.cmu.a75hard.workers

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object ScheduleReminder {

    // Agendamento diário
    fun scheduleDailyReminder(context: Context) {
        val dailyWorkRequest = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "daily_reminder_work",
            ExistingPeriodicWorkPolicy.REPLACE,
            dailyWorkRequest
        )
    }

    // Agendamento a cada 7 horas
    fun scheduleSevenHourReminder(context: Context) {
        val sevenHourWorkRequest = PeriodicWorkRequestBuilder<ReminderWorker>(7, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "seven_hour_reminder_work",
            ExistingPeriodicWorkPolicy.REPLACE,
            sevenHourWorkRequest
        )
    }
}
