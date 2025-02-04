package com.cmu.a75hard.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmu.a75hard.model.user.UserDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import at.favre.lib.crypto.bcrypt.BCrypt
import com.cmu.a75hard.R
import com.cmu.a75hard.model.user.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val userId: Int) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(private val userDao: UserDao, private val firestoreViewModel: FirestoreViewModel) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> get() = _authState

    private val _registerState = MutableStateFlow<AuthState>(AuthState.Idle)
    val registerState: StateFlow<AuthState> get() = _registerState

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance() // Instância do Firebase Authentication

    fun login(email: String, password: String, context: Context) {
        if (email.isEmpty() || password.isEmpty()) {
            _authState.value = AuthState.Error(context.getString(R.string.fill_all_fields))
            return
        }

        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val localUser = userDao.getUserByEmail(email)

            if (localUser != null) {
                val result = BCrypt.verifyer().verify(password.toCharArray(), localUser.password)
                if (result.verified) {
                    _authState.value = AuthState.Success(localUser.userId)
                } else {
                    _authState.value = AuthState.Error(context.getString(R.string.invalid_credentials))
                }
            } else {
                try {
                    val authResult = FirebaseAuth.getInstance()
                        .signInWithEmailAndPassword(email, password)
                        .await()

                    val firebaseUserId = authResult.user?.uid
                    if (firebaseUserId != null) {
                        val firestore = FirebaseFirestore.getInstance()
                        val userDoc = firestore.collection("users").document(email).get().await()

                        if (userDoc.exists()) {
                            val hashedPassword =
                                BCrypt.withDefaults().hashToString(12, password.toCharArray())
                            val newUser = User(
                                firstName = userDoc.getString("firstName") ?: "",
                                lastName = userDoc.getString("lastName") ?: "",
                                email = email,
                                address = userDoc.getString("address") ?: "",
                                city = userDoc.getString("city") ?: "",
                                height = userDoc.getLong("height")?.toInt() ?: 0,
                                password = hashedPassword,
                                weightLost = userDoc.getDouble("weightLost")?.toFloat() ?: 0.0f
                            )

                            val userId = userDao.insert(newUser).toInt()
                            _authState.value = AuthState.Success(userId)
                        } else {
                            // Exibe mensagem genérica se o documento do usuário não existir
                            _authState.value = AuthState.Error(context.getString(R.string.login_failed))
                        }
                    } else {
                        // Exibe mensagem genérica se o Firebase retornar UID nulo
                        _authState.value = AuthState.Error(context.getString(R.string.login_failed))
                    }
                } catch (e: Exception) {
                    // Exibe mensagem genérica para qualquer exceção capturada
                    _authState.value = AuthState.Error(context.getString(R.string.login_failed))
                }
            }
        }
    }

    fun register(
        firstName: String,
        lastName: String,
        email: String,
        address: String,
        city: String,
        height: String,
        password: String,
        context: Context
    ) {
        _registerState.value = AuthState.Loading
        viewModelScope.launch {
            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() ||
                address.isEmpty() || city.isEmpty() || height.isEmpty() || password.isEmpty()
            ) {
                _registerState.value = AuthState.Error(context.getString(R.string.fill_all_fields))
                return@launch
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                _registerState.value = AuthState.Error(context.getString(R.string.invalid_email_format))
                return@launch
            }

            val heightValue = height.toIntOrNull()
            if (heightValue == null || heightValue !in 50..300) {
                _registerState.value = AuthState.Error(context.getString(R.string.invalid_height))
                return@launch
            }

            val existingUser = userDao.getUserByEmail(email)
            if (existingUser != null) {
                _registerState.value = AuthState.Error(context.getString(R.string.email_already_registered))
                return@launch
            }

            val hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray())

            val newUser = User(
                firstName = firstName,
                lastName = lastName,
                email = email,
                address = address,
                city = city,
                height = heightValue,
                password = hashedPassword
            )

            try {
                FirebaseAuth.getInstance()
                    .createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            viewModelScope.launch {
                                val userId = userDao.insert(newUser).toInt()

                                firestoreViewModel.saveUserToFirestore(newUser) { success, error ->
                                    if (success) {
                                        _registerState.value = AuthState.Success(userId)
                                    } else {
                                        _registerState.value = AuthState.Error(
                                            context.getString(R.string.registration_failed)
                                        )
                                    }
                                }
                            }
                        } else {
                            // Simplificar a mensagem de erro com base no código do Firebase
                            val errorMessage = when (task.exception?.message) {
                                null -> context.getString(R.string.unknown_error)
                                else -> {
                                    if (task.exception?.message?.contains("password", true) == true) {
                                        context.getString(R.string.invalid_password)
                                    } else {
                                        context.getString(R.string.registration_failed)
                                    }
                                }
                            }

                            _registerState.value = AuthState.Error(errorMessage)
                        }
                    }
            } catch (e: Exception) {
                _registerState.value = AuthState.Error(context.getString(R.string.registration_failed))
            }
        }
    }





    // Verifica se o usuário está logado ao inicializar o aplicativo
    fun isUserLoggedIn(context: Context): Boolean {
        val sharedPref = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("userId", -1)
        return userId != -1
    }

    fun logout(context: Context) {
        val sharedPref = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
        sharedPref.edit().remove("userId").apply()
        _authState.value = AuthState.Idle
    }

    // Novos métodos para redefinir o estado
    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }

    fun resetRegisterState() {
        _registerState.value = AuthState.Idle
    }

}

