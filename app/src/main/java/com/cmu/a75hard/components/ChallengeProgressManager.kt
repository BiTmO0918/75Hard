package com.cmu.a75hard.components

import android.content.Context
import android.content.SharedPreferences
import com.cmu.a75hard.model.DayData.DayData
import com.cmu.a75hard.model.DayData.DayDataDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class ChallengeProgressManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("challenge_prefs", Context.MODE_PRIVATE)
    private val notificationPrefs: SharedPreferences = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)

    var currentDay: Int
        get() = prefs.getInt("current_day", 1)
        set(value) = prefs.edit().putInt("current_day", value).apply()

    var startDate: Long
        get() = prefs.getLong("start_date", System.currentTimeMillis())
        set(value) = prefs.edit().putLong("start_date", value).apply()

    fun isChallengeStarted(): Boolean {
        return prefs.contains("start_date")
    }

    fun resetProgress() {
        prefs.edit().clear().apply()
        currentDay = 1
        startDate = System.currentTimeMillis()
    }

    var isNotificationsEnabled: Boolean
        get() = notificationPrefs.getBoolean("notifications_enabled", false)
        set(enabled) = notificationPrefs.edit().putBoolean("notifications_enabled", enabled).apply()

    /**
     * Ajusta o startDate e currentDay com base nos dados carregados do Firestore.
     * Esta função deve ser chamada após o fetch dos dados do Firestore estar concluído
     * e os dados estarem armazenados localmente.
     *
     * Lógica:
     * 1. Encontra o último dia completado.
     * 2. Define o currentDay como último dia completo + 1.
     * 3. Ajusta o startDate para que o cálculo baseado no tempo mantenha a coerência.
     */
    suspend fun adjustStartDateAndCurrentDayAfterFirestoreData(userId: Int, dayDataDao: DayDataDao) {
        withContext(Dispatchers.IO) {
            val allDays = dayDataDao.getAllDaysDataForUser(userId)
            val lastCompletedDay = allDays.filter { isCompleted(it) }.maxOfOrNull { it.dayNumber } ?: 0

            val baselineDay = lastCompletedDay + 1
            currentDay = baselineDay

            // Ajuste do startDate:
            // Queremos que o currentDay baseie-se no tempo decorrido. Se currentDay é baselineDay,
            // significa que se passaram (baselineDay - 1) dias desde o início.
            // Assim, definimos o startDate para (baselineDay - 1) dias atrás.
            val now = System.currentTimeMillis()
            val oneDayInMillis = TimeUnit.DAYS.toMillis(1)
            val adjustedStartDate = now - ((baselineDay - 1) * oneDayInMillis)
            startDate = adjustedStartDate
        }
    }

    private fun isCompleted(dayData: DayData): Boolean {
        return dayData.diet &&
                dayData.reading &&
                dayData.waterIntake >= 3.7 &&
                dayData.progressPictureUrl != null &&
                dayData.noAlcohol &&
                dayData.indoorWorkout != null &&
                dayData.outdoorWorkout != null
    }

    fun updateCurrentDayFromTime() {
        val daysPassed = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - startDate).toInt() + 1
        currentDay = daysPassed
    }


}
