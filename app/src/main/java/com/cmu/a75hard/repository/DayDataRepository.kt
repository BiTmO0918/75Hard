package com.cmu.a75hard.repository

import android.content.Context
import com.cmu.a75hard.components.ChallengeProgressManager
import com.cmu.a75hard.model.DayData.DayData
import com.cmu.a75hard.model.DayData.DayDataDao
import com.cmu.a75hard.model.user.UserDao
import com.cmu.a75hard.utils.isInternetAvailable
import com.cmu.a75hard.viewmodel.FirestoreViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DayDataRepository(private val dayDataDao: DayDataDao, private val userDao: UserDao) {

    // Método para buscar todos os pesos
    suspend fun getAllWeights(): List<Float> {
        return withContext(Dispatchers.IO) {
            dayDataDao.getAllDaysData().mapNotNull { it.weight }
        }
    }

    // Método para buscar mudanças de peso diárias
    suspend fun getDailyWeightChanges(): List<Float> {
        val weights = getAllWeights()
        return weights.zipWithNext { prev, current -> current - prev }
    }

    // Método para atualizar o peso de um dia
    suspend fun updateWeightForDay(dayNumber: Int, weight: Float) {
        withContext(Dispatchers.IO) {
            dayDataDao.updateWeightForDay(dayNumber, weight)
        }
    }

    suspend fun insertOrUpdateDayData(dayData: DayData) {
        withContext(Dispatchers.IO) {
            dayDataDao.insertOrUpdateDayData(dayData)
        }
    }

    suspend fun getDayData(dayNumber: Int): DayData? {
        return withContext(Dispatchers.IO) {
            dayDataDao.getDayData(dayNumber)
        }
    }

    suspend fun getDayDataForUser(dayNumber: Int, userId: Int): DayData? {
        return withContext(Dispatchers.IO) {
            dayDataDao.getDayDataForUser(dayNumber, userId)
        }
    }

    suspend fun clearAllData() {
        withContext(Dispatchers.IO) {
            dayDataDao.clearAllData()
        }
    }

    suspend fun updateDayCompletionStatus(dayNumber: Int, isCompleted: Boolean) {
        withContext(Dispatchers.IO) {
            dayDataDao.updateDayCompletionStatus(dayNumber, isCompleted)
        }
    }

    suspend fun getAllProgressUrls(): List<DayDataDao.ProgressImage> {
        return withContext(Dispatchers.IO) {
            dayDataDao.getAllProgressUrls()
        }
    }

    suspend fun getWeightForDay(dayNumber: Int): Float? {
        return withContext(Dispatchers.IO) {
            dayDataDao.getWeightForDay(dayNumber)
        }
    }

    suspend fun areAllTasksCompletedForDays(userId: Int, untilDayNumber: Int): Boolean {
        return withContext(Dispatchers.IO) {
            for (day in 1 until untilDayNumber) {
                val dayData = dayDataDao.getDayDataForUser(day, userId)
                val allTasksCompleted = dayData?.let {
                    isCompleted(it)
                } ?: false
                if (!allTasksCompleted) {
                    return@withContext false
                }
            }
            true
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

    suspend fun syncDayDataToFirestore(userId: Int, firestoreViewModel: FirestoreViewModel, context: Context) {
        if (isInternetAvailable(context)) {
            val userEmail = userDao.getEmailByUserId(userId) // Obter email do utilizador

            if (userEmail != null) {
                val userDayData = dayDataDao.getAllDaysDataForUser(userId)
                for (dayData in userDayData) {
                    firestoreViewModel.saveDayDataToFirestore(dayData, userEmail) { success, error ->
                        if (success) {
                            println("Day ${dayData.dayNumber} synchronized successfully for user $userEmail!")
                        } else {
                            println("Failed to sync day ${dayData.dayNumber} for user $userEmail: $error")
                        }
                    }
                }

                // Após sincronizar os DayData, atualizar o utilizador no Firestore também
                val user = userDao.getUserById(userId)
                if (user != null) {
                    firestoreViewModel.updateUserInFirestore(user) { success, error ->
                        if (success) {
                            println("User data synchronized successfully for $userEmail!")
                        } else {
                            println("Failed to sync user data for $userEmail: $error")
                        }
                    }
                }

            } else {
                println("Error: Email not found for userId $userId")
            }
        } else {
            println("No internet connection. Cannot sync data for userId $userId.")
        }
    }

    suspend fun fetchAndStoreDayDataFromFirestore(
        userId: Int,
        firestoreViewModel: FirestoreViewModel,
        context: Context,
        challengeProgressManager: ChallengeProgressManager,
        dayDataDao: DayDataDao
    ) {
        val userEmail = userDao.getEmailByUserId(userId) // Obter email do utilizador

        if (userEmail != null && isInternetAvailable(context)) {
            firestoreViewModel.getDayDataFromFirestore(userEmail) { dayDataList, error ->
                if (dayDataList != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        // Armazena os DayData localmente
                        for (dayData in dayDataList) {
                            val isCompleted = isCompleted(dayData)
                            val localDayData = dayData.copy(userId = userId, isCompleted = isCompleted)
                            dayDataDao.insertOrUpdateDayData(localDayData)
                        }
                        println("DayData loaded and stored locally for user $userEmail")

                        // Ajusta o currentDay
                        challengeProgressManager.adjustStartDateAndCurrentDayAfterFirestoreData(userId, dayDataDao)

                        // Agora carregar o utilizador do Firestore e atualizar localmente (caso não exista)
                        firestoreViewModel.getUserFromFirestore(userEmail) { fetchedUser, userError ->
                            CoroutineScope(Dispatchers.IO).launch {
                                if (fetchedUser != null) {
                                    val localUser = userDao.getUserByEmail(userEmail)
                                    if (localUser == null) {
                                        // Inserir utilizador localmente
                                        userDao.insert(fetchedUser)
                                    } else {
                                        // Atualizar utilizador localmente
                                        val updatedLocalUser = localUser.copy(
                                            firstName = fetchedUser.firstName,
                                            lastName = fetchedUser.lastName,
                                            email = fetchedUser.email,
                                            address = fetchedUser.address,
                                            city = fetchedUser.city,
                                            height = fetchedUser.height,
                                            weightLost = fetchedUser.weightLost
                                        )
                                        userDao.update(updatedLocalUser)
                                    }

                                    // Agora que o utilizador está atualizado localmente, recalcular weightLost
                                    // e atualizar no Firestore, garantindo coerência
                                    updateWeightLost(userId, firestoreViewModel, context)
                                } else {
                                    println("Error fetching user from Firestore: $userError")
                                }
                            }
                        }
                    }
                } else {
                    println("Error fetching DayData for user $userEmail: $error")
                }
            }
        } else {
            println("Error: Email not found or no internet connection for userId $userId")
        }
    }

    // Mantemos apenas a versão com parâmetros opcionais
    suspend fun updateWeightLost(userId: Int, firestoreViewModel: FirestoreViewModel? = null, context: Context? = null) {
        withContext(Dispatchers.IO) {
            val firstDayWeight = dayDataDao.getDayDataForUser(1, userId)?.weight ?: return@withContext
            val latestWeight = dayDataDao.getAllDaysData().lastOrNull { it.userId == userId }?.weight ?: return@withContext

            val weightLost = if (latestWeight < firstDayWeight) {
                firstDayWeight - latestWeight
            } else {
                0.0F
            }

            val user = userDao.getUserById(userId)
            if (user != null) {
                val updatedUser = user.copy(weightLost = weightLost)
                userDao.update(updatedUser)

                // Se tivermos acesso ao firestoreViewModel e internet, atualizar no Firestore
                if (firestoreViewModel != null && context != null && isInternetAvailable(context)) {
                    firestoreViewModel.updateUserInFirestore(updatedUser) { success, error ->
                        if (success) {
                            println("User weightLost updated in Firestore successfully.")
                        } else {
                            println("Failed to update user in Firestore: $error")
                        }
                    }
                }
            }
        }
    }

    suspend fun markDayAsCompleted(dayNumber: Int, userId: Int) {
        withContext(Dispatchers.IO) {
            dayDataDao.updateDayCompletionStatus(dayNumber, true)
        }
    }

    suspend fun getAllWeightsForUser(userId: Int): List<Float> {
        return withContext(Dispatchers.IO) {
            dayDataDao.getAllDaysDataForUser(userId).mapNotNull { it.weight }
        }
    }

    suspend fun getDailyWeightChangesForUser(userId: Int): List<Float> {
        val weights = getAllWeightsForUser(userId)
        return weights.zipWithNext { prev, current -> current - prev }
    }

    suspend fun getProgressUrlsForUser(userId: Int): List<DayDataDao.ProgressImage> {
        return withContext(Dispatchers.IO) {
            dayDataDao.getProgressUrlsForUser(userId)
        }
    }
}
