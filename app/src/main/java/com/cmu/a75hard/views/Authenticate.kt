package com.cmu.a75hard.views

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cmu.a75hard.R
import com.cmu.a75hard.components.ChallengeProgressManager
import com.cmu.a75hard.model.MyDatabase
import com.cmu.a75hard.repository.DayDataRepository
import com.cmu.a75hard.viewmodel.AuthViewModel
import com.cmu.a75hard.viewmodel.AuthState
import com.cmu.a75hard.viewmodel.FirestoreViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val customFont = FontFamily(
    Font(R.font.sport_font)
)

@Composable
fun LoginScreen(navController: NavController, authViewModel: AuthViewModel = viewModel()) {
    val authState by authViewModel.authState.collectAsState()

    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val context = navController.context

    // Handle state changes
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                val userId = (authState as AuthState.Success).userId
                val sharedPref = navController.context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
                sharedPref.edit().putInt("userId", userId).apply()

                val database = MyDatabase.getDatabase(navController.context)
                val dayDataDao = database.dayDataDao()
                val firestoreViewModel = FirestoreViewModel()
                val repository = DayDataRepository(dayDataDao, database.userDao())
                val challengeProgressManager = ChallengeProgressManager(navController.context)

                // Busca dados do Firestore e armazena no banco local, ajusta startDate/currentDay após isso.
                repository.fetchAndStoreDayDataFromFirestore(
                    userId = userId,
                    firestoreViewModel = firestoreViewModel,
                    context = navController.context,
                    challengeProgressManager = challengeProgressManager,
                    dayDataDao = dayDataDao
                )

                // Não chamamos mais syncCurrentDayWithDatabase aqui, pois o método acima já ajusta o currentDay.

                // Navega para a tela de DailyTasks após o carregamento.
                // Como o fetch do Firestore é assíncrono, idealmente deveríamos esperar sua conclusão.
                // Uma opção simples (não ideal) é navegar após um pequeno delay ou quando tiver certeza
                // de que o currentDay já foi ajustado. Para simplificar, iremos navegar diretamente,
                // mas note que pode ser necessário melhorar esta lógica.
                // Uma solução mais robusta envolveria retornar um callback ou usar um estado a partir do ViewModel.

                // Aguarde um pouco para garantir o carregamento (Exemplo simples, não ideal):
                kotlinx.coroutines.delay(500) // Espera meio segundo (0.5s) (isto é apenas exemplo)
                println("DEBUG: Redirecting to day ${challengeProgressManager.currentDay}")
                navController.navigate("dailyTasks/${challengeProgressManager.currentDay}")
            }
            is AuthState.Error -> { /* Exibe mensagem de erro */ }
            else -> Unit
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        // Código de UI inalterado
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.app_number_75),
                    style = TextStyle(
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 150.sp,
                        fontFamily = customFont,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                OutlinedTextField(
                    value = email.value,
                    onValueChange = {
                        email.value = it
                        authViewModel.resetAuthState() // Limpa mensagem de erro
                    },
                    label = { Text(stringResource(R.string.email)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(5.dp)
                )

                OutlinedTextField(
                    value = password.value,
                    onValueChange = {
                        password.value = it
                        authViewModel.resetAuthState() // Limpa mensagem de erro
                    },
                    label = { Text(stringResource(R.string.password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(5.dp)
                )

                // Espaço fixo para a mensagem de erro
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (authState is AuthState.Error) {
                        Text(
                            text = (authState as AuthState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        authViewModel.login(email.value, password.value, context)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Text(stringResource(R.string.login))
                }

                OutlinedButton(
                    onClick = { navController.navigate("SignUpScreen") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.sign_up))
                }
            }
        }
    }
}

@Composable
fun SignUpScreen(navController: NavController, authViewModel: AuthViewModel = viewModel()) {
    val registerState by authViewModel.registerState.collectAsState()

    val firstName = remember { mutableStateOf("") }
    val lastName = remember { mutableStateOf("") }
    val email = remember { mutableStateOf("") }
    val address = remember { mutableStateOf("") }
    val city = remember { mutableStateOf("") }
    val height = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }

    // Handle state changes
    LaunchedEffect(registerState) {
        when (registerState) {
            is AuthState.Success -> {
                val userId = (registerState as AuthState.Success).userId
                val sharedPref = navController.context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
                sharedPref.edit().putInt("userId", userId).apply()

                // Inicializa o ChallengeProgressManager
                val challengeProgressManager = ChallengeProgressManager(navController.context)
                // Se o desafio ainda não começou, iniciamos agora
                if (!challengeProgressManager.isChallengeStarted()) {
                    challengeProgressManager.resetProgress()
                    // Agora currentDay = 1 e startDate = System.currentTimeMillis()
                }

                // Redireciona para o dailyTasks/1
                navController.navigate("dailyTasks/1") {
                    popUpTo("loginScreen") { inclusive = true }
                }
            }
            is AuthState.Error -> { /* Mensagem de erro gerenciada abaixo */ }
            else -> Unit
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f) // Ocupa o espaço restante
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Top
            ) {
                OutlinedTextField(
                    value = firstName.value,
                    onValueChange = {
                        firstName.value = it
                        authViewModel.resetRegisterState() // Limpa mensagem de erro
                    },
                    label = { Text(stringResource(R.string.first_name)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = lastName.value,
                    onValueChange = {
                        lastName.value = it
                        authViewModel.resetRegisterState()
                    },
                    label = { Text(stringResource(R.string.last_name)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = email.value,
                    onValueChange = {
                        email.value = it
                        authViewModel.resetRegisterState()
                    },
                    label = { Text(stringResource(R.string.email)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = address.value,
                    onValueChange = {
                        address.value = it
                        authViewModel.resetRegisterState()
                    },
                    label = { Text(stringResource(R.string.address)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = city.value,
                    onValueChange = {
                        city.value = it
                        authViewModel.resetRegisterState()
                    },
                    label = { Text(stringResource(R.string.city)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = height.value,
                    onValueChange = {
                        height.value = it
                        authViewModel.resetRegisterState()
                    },
                    label = { Text(stringResource(R.string.height_cm)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = password.value,
                    onValueChange = {
                        password.value = it
                        authViewModel.resetRegisterState()
                    },
                    label = { Text(stringResource(R.string.password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                // Espaço fixo para a mensagem de erro
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (registerState is AuthState.Error) {
                        Text(
                            text = (registerState as AuthState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        authViewModel.register(
                            firstName.value,
                            lastName.value,
                            email.value,
                            address.value,
                            city.value,
                            height.value,
                            password.value,
                            navController.context
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Text(stringResource(R.string.register))
                }

                OutlinedButton(
                    onClick = { navController.navigate("loginScreen") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.login))
                }
            }
        }
    }
}


