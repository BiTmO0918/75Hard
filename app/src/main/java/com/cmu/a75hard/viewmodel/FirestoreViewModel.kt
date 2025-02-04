package com.cmu.a75hard.viewmodel

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import com.cmu.a75hard.model.DayData.DayData
import com.cmu.a75hard.model.DayData.WorkoutData
import com.cmu.a75hard.model.user.User
import com.google.firebase.firestore.FirebaseFirestore

class FirestoreViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    fun saveUserToFirestore(user: User, onComplete: (Boolean, String?) -> Unit) {
        val userMap = mapOf(
            "firstName" to user.firstName,
            "lastName" to user.lastName,
            "email" to user.email,
            "address" to user.address,
            "city" to user.city,
            "height" to user.height,
            "weightLost" to user.weightLost
        )

        firestore.collection("users")
            .document(user.email) // Usa o email como ID único
            .set(userMap)
            .addOnSuccessListener {
                onComplete(true, null) // Sucesso
            }
            .addOnFailureListener { exception ->
                onComplete(false, exception.message) // Falhou
            }
    }

    // Função para buscar um usuário do Firestore
    fun getUserFromFirestore(email: String, onComplete: (User?, String?) -> Unit) {
        firestore.collection("users")
            .document(email)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val user = User(
                        firstName = document.getString("firstName") ?: "",
                        lastName = document.getString("lastName") ?: "",
                        email = document.getString("email") ?: "",
                        address = document.getString("address") ?: "",
                        city = document.getString("city") ?: "",
                        height = document.getLong("height")?.toInt() ?: 0,
                        weightLost = document.getDouble("weightLost")?.toFloat() ?: 0.0f,
                        password = "" // A senha não será retornada pelo Firestore
                    )
                    onComplete(user, null)
                } else {
                    onComplete(null, "User not found")
                }
            }
            .addOnFailureListener { exception ->
                onComplete(null, exception.message)
            }
    }

    // Função para atualizar um usuário no Firestore
    fun updateUserInFirestore(user: User, onComplete: (Boolean, String?) -> Unit) {
        val userMap = mapOf(
            "firstName" to user.firstName,
            "lastName" to user.lastName,
            "address" to user.address,
            "city" to user.city,
            "height" to user.height,
            "weightLost" to user.weightLost
        )

        firestore.collection("users")
            .document(user.email)
            .update(userMap)
            .addOnSuccessListener {
                onComplete(true, null)
            }
            .addOnFailureListener { exception ->
                onComplete(false, exception.message)
            }
    }

    fun saveFeedback(userId: Int, feedbackText: String, onComplete: (Boolean, String?) -> Unit) {
        val firestore = FirebaseFirestore.getInstance()

        val feedbackData = mapOf(
            "userId" to userId,
            "feedbackText" to feedbackText,
            "timestamp" to System.currentTimeMillis() // Adiciona um timestamp
        )

        firestore.collection("feedback")
            .add(feedbackData)
            .addOnSuccessListener {
                onComplete(true, null) // Sucesso
            }
            .addOnFailureListener { exception ->
                onComplete(false, exception.message) // Falha
            }
    }

    fun saveDayDataToFirestore(dayData: DayData, userEmail: String, onComplete: (Boolean, String?) -> Unit) {
        val dayDataMap = mapOf(
            "dayNumber" to dayData.dayNumber,
            "weight" to dayData.weight,
            "reading" to dayData.reading,
            "waterIntake" to dayData.waterIntake,
            "progressPictureUrl" to dayData.progressPictureUrl,
            "diet" to dayData.diet,
            "noAlcohol" to dayData.noAlcohol,
            "indoorWorkout" to dayData.indoorWorkout,
            "outdoorWorkout" to dayData.outdoorWorkout
        )

        firestore.collection("users")
            .document(userEmail) // Usa o email como ID do documento do usuário
            .collection("dayData")
            .document(dayData.dayNumber.toString()) // Cada dia é um documento único
            .set(dayDataMap)
            .addOnSuccessListener { onComplete(true, null) }
            .addOnFailureListener { exception -> onComplete(false, exception.message) }
    }

    fun getDayDataFromFirestore(
        userEmail: String,
        onComplete: (List<DayData>?, String?) -> Unit
    ) {
        val dayDataList = mutableListOf<DayData>()

        firestore.collection("users")
            .document(userEmail)
            .collection("dayData")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val indoorWorkoutMap = document.get("indoorWorkout") as? Map<String, Any>
                    val outdoorWorkoutMap = document.get("outdoorWorkout") as? Map<String, Any>

                    // Construção do objeto DayData
                    val dayData = DayData(
                        dayNumber = document.getLong("dayNumber")?.toInt() ?: 0,
                        userId = -1, // Atualize com o userId real se necessário
                        isCompleted = document.getBoolean("isCompleted") ?: false,
                        waterIntake = document.getDouble("waterIntake") ?: 0.0, // Double é direto
                        weight = document.getDouble("weight")?.toFloat(),       // Convertendo para Float
                        progressPictureUrl = document.getString("progressPictureUrl"),
                        diet = document.getBoolean("diet") ?: false,
                        reading = document.getBoolean("reading") ?: false,
                        noAlcohol = document.getBoolean("noAlcohol") ?: false,
                        indoorWorkout = mapWorkoutData(indoorWorkoutMap),
                        outdoorWorkout = mapWorkoutData(outdoorWorkoutMap)
                    )

                    dayDataList.add(dayData)
                }
                onComplete(dayDataList, null) // Sucesso
            }
            .addOnFailureListener { exception ->
                onComplete(null, exception.message) // Falha
            }
    }

    fun mapWorkoutData(workoutMap: Map<String, Any>?): WorkoutData? {
        return workoutMap?.let {
            WorkoutData(
                duration = it["duration"] as? String ?: "0 min",
                maxSpeed = (it["maxSpeed"] as? Number)?.toDouble() ?: 0.0,           // Convertendo para Double
                pace = it["pace"] as? String ?: "0:00",
                caloriesBurned = (it["caloriesBurned"] as? Number)?.toInt() ?: 0,
                distance = (it["distance"] as? Number)?.toDouble() ?: 0.0,           // Convertendo para Double
                averageHeartRate = (it["averageHeartRate"] as? Number)?.toInt() ?: 0,
                maxAcceleration = (it["maxAcceleration"] as? Number)?.toDouble() ?: 0.0, // Convertendo para Double
                steps = (it["steps"] as? Number)?.toInt() ?: 0
            )
        }
    }


    fun getAllUsersFromFirestore(onComplete: (List<User>?, String?) -> Unit) {
        firestore.collection("users")
            .get()
            .addOnSuccessListener { result ->
                val userList = mutableListOf<User>()
                for (document in result) {
                    val user = User(
                        firstName = document.getString("firstName") ?: "",
                        lastName = document.getString("lastName") ?: "",
                        email = document.getString("email") ?: "",
                        address = document.getString("address") ?: "",
                        city = document.getString("city") ?: "",
                        height = document.getLong("height")?.toInt() ?: 0,
                        weightLost = document.getDouble("weightLost")?.toFloat() ?: 0.0f,
                        password = "" // Senha não retornada
                    )
                    userList.add(user)
                }
                onComplete(userList, null)
            }
            .addOnFailureListener { exception ->
                onComplete(null, exception.message)
            }
    }


}
