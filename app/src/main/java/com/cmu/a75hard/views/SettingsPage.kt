package com.cmu.a75hard.views

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

import com.cmu.a75hard.R
import com.cmu.a75hard.components.SettingItem
import com.cmu.a75hard.components.SettingItemWithDropdown
import com.cmu.a75hard.components.SettingItemWithToggle
import androidx.compose.runtime.*
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.WorkManager
import com.cmu.a75hard.MainActivity
import com.cmu.a75hard.components.ChallengeProgressManager
import com.cmu.a75hard.components.SettingItemWithDropdown_
import com.cmu.a75hard.model.user.User
import com.cmu.a75hard.model.user.UserDao
import com.cmu.a75hard.utils.ThemePreferences
import com.cmu.a75hard.viewmodel.AuthViewModel
import com.cmu.a75hard.viewmodel.FirestoreViewModel
import com.cmu.a75hard.workers.ScheduleReminder
import kotlinx.coroutines.launch

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountPage(
    navController: NavController,
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    authViewModel: AuthViewModel = viewModel(),
    themePreferences: ThemePreferences
) {
    val supportPhoneNumber = "123456789"
    var showLogoutDialog by remember { mutableStateOf(false) }
    val activity = (navController.context as? MainActivity)

    val settingsTitle = stringResource(R.string.settings_title)
    val profileLabel = stringResource(R.string.profile_label)
    val notificationsLabel = stringResource(R.string.notifications_label)
    val languageLabel = stringResource(R.string.language_label)
    val feedbackLabel = stringResource(R.string.feedback_label)
    val supportLabel = stringResource(R.string.support_label)
    val themeLabel = stringResource(R.string.theme_label)
    val logoutLabel = stringResource(R.string.logout_label)
    val confirmLogoutStr = stringResource(R.string.confirm_logout)
    val logoutQuestionStr = stringResource(R.string.logout_question)
    val logoutButtonStr = stringResource(R.string.logout_button)
    val cancelButtonStr = stringResource(R.string.cancel_button)
    val onStr = stringResource(R.string.on)
    val offStr = stringResource(R.string.off)
    val englishStr = stringResource(R.string.english)
    val portugueseStr = stringResource(R.string.portuguese)

    val currentLang = activity?.languagePreferences?.getLanguagePreference() ?: "en"
    val selectedLanguage = remember {
        mutableStateOf(if (currentLang == "pt") portugueseStr else englishStr)
    }

    val progressManager = remember(navController.context) { ChallengeProgressManager(navController.context) }

    val isNotificationsOn = remember {
        mutableStateOf(progressManager.isNotificationsEnabled)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = settingsTitle, fontWeight = FontWeight.Bold) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SettingItem(
                iconId = R.drawable.ic_profile,
                label = profileLabel,
                onClick = { navController.navigate("profileScreen") }
            )
            SettingItemWithDropdown(
                iconId = R.drawable.ic_notifications,
                label = notificationsLabel,
                options = listOf(onStr, offStr),
                selectedOption = if (isNotificationsOn.value) onStr else offStr,
                onOptionSelected = { option ->
                    val isOn = option == onStr
                    progressManager.isNotificationsEnabled = isOn
                    isNotificationsOn.value = isOn

                    if (isOn) {
                        ScheduleReminder.scheduleSevenHourReminder(navController.context)
                        Toast.makeText(navController.context, "Notificações ativadas.", Toast.LENGTH_SHORT).show()
                    } else {
                        WorkManager.getInstance(navController.context).cancelUniqueWork("seven_hour_reminder_work")
                        Toast.makeText(navController.context, "Notificações desativadas.", Toast.LENGTH_SHORT).show()
                    }
                }
            )
            SettingItemWithDropdown_(
                iconId = R.drawable.ic_language,
                label = languageLabel,
                options = listOf(englishStr, portugueseStr),
                selectedOption = selectedLanguage.value,
                onOptionSelected = { option ->
                    selectedLanguage.value = option
                    activity?.updateLanguage(if (option == portugueseStr) "pt" else "en")
                }
            )
            SettingItem(
                iconId = R.drawable.ic_feedback,
                label = feedbackLabel,
                onClick = { navController.navigate("feedbackScreen") }
            )
            SettingItem(
                iconId = R.drawable.ic_support,
                label = supportLabel,
                onClick = {
                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:$supportPhoneNumber")
                    }
                    navController.context.startActivity(intent)
                }
            )
            SettingItemWithToggle(
                iconId = R.drawable.ic_theme,
                label = themeLabel,
                isChecked = isDarkTheme,
                onToggleChange = { newTheme ->
                    onThemeChange(newTheme)
                    themePreferences.saveThemePreference(newTheme)
                }
            )

            SettingItem(
                iconId = R.drawable.ic_logout,
                label = logoutLabel,
                isLogout = true,
                onClick = { showLogoutDialog = true }
            )
        }

        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = {
                    Text(
                        text = confirmLogoutStr,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Text(
                        text = logoutQuestionStr,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showLogoutDialog = false

                        // Chamar o método de logout no ViewModel
                        authViewModel.logout(navController.context)

                        // Resetar o progresso do desafio para não herdar dados do utilizador anterior
                        progressManager.resetProgress()

                        // Navegar para a tela de login
                        navController.navigate("loginScreen") {
                            popUpTo("loginScreen") { inclusive = true }
                        }
                    }) {
                        Text(logoutButtonStr, color = MaterialTheme.colorScheme.primary)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text(cancelButtonStr, color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(navController: NavController, firestoreViewModel: FirestoreViewModel) {
    var feedbackText by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") } // Estado para mensagem de sucesso
    var isSending by remember { mutableStateOf(false) } // Estado para indicar envio

    val context = navController.context // Obtenha o contexto do NavController

    // Carregue as strings no contexto composable
    val feedbackTitle = stringResource(R.string.feedback_title)
    val backStr = stringResource(R.string.back)
    val enterFeedbackHere = stringResource(R.string.enter_feedback_here)
    val addFileStr = stringResource(R.string.add_file)
    val sendStr = stringResource(R.string.send)
    val sendIconCd = stringResource(R.string.send_icon_cd)
    val feedbackSentSuccess = stringResource(R.string.feedback_sent_successfully)
    val feedbackEmptyError = stringResource(R.string.feedback_empty_error)
    val errorSendingFeedback = stringResource(R.string.error_sending_feedback)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(feedbackTitle, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = backStr)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Campo de Feedback
                OutlinedTextField(
                    value = feedbackText,
                    onValueChange = { feedbackText = it },
                    placeholder = { Text(enterFeedbackHere) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(5.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.background,
                        unfocusedContainerColor = MaterialTheme.colorScheme.background,
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Botão Add File
                OutlinedButton(
                    onClick = { /* Handle file upload */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(5.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_input_add),
                            contentDescription = addFileStr,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(addFileStr)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Botão Send
                Button(
                    onClick = {
                        if (feedbackText.isNotEmpty()) {
                            isSending = true
                            firestoreViewModel.saveFeedback(userId = 123, feedbackText = feedbackText) { success, error ->
                                isSending = false
                                if (success) {
                                    successMessage = feedbackSentSuccess
                                    feedbackText = "" // Limpa o texto após enviar
                                    Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show()
                                } else {
                                    successMessage = error ?: errorSendingFeedback
                                    Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            successMessage = feedbackEmptyError
                            Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(5.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(sendStr, color = MaterialTheme.colorScheme.onPrimary, fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            painter = painterResource(id = R.drawable.ic_send),
                            contentDescription = sendIconCd,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    )
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, userDao: UserDao, firestoreViewModel: FirestoreViewModel) {
    val context = navController.context
    val sharedPref = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
    val userId = sharedPref.getInt("userId", -1) // Obtém o userId das SharedPreferences

    // Estados para os dados do utilizador
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") } // Estado para exibir mensagens de erro
    val coroutineScope = rememberCoroutineScope()

    // Estado de sincronização com Firestore
    val isSyncingWithFirestore = remember { mutableStateOf(false) }

    // Carregar os dados do utilizador da base local como fonte primária
    LaunchedEffect(userId) {
        if (userId != -1) {
            val localUser = userDao.getUserById(userId)
            localUser?.let {
                firstName = it.firstName
                lastName = it.lastName
                email = it.email
                address = it.address
                city = it.city
                height = it.height.toString()

                // Inicia sincronização com Firestore em segundo plano
                firestoreViewModel.getUserFromFirestore(email) { firestoreUser, error ->
                    if (firestoreUser != null && firestoreUser != localUser) {
                        // Se os dados do Firestore forem diferentes, sincroniza Firestore -> Local
                        coroutineScope.launch {
                            userDao.update(firestoreUser)
                        }
                    }
                    isSyncingWithFirestore.value = false // Sincronização concluída
                }
            }
        }
    }

    val profileTitle = stringResource(R.string.profile_title)
    val backStr = stringResource(R.string.back)
    val firstNameLabel = stringResource(R.string.first_name)
    val lastNameLabel = stringResource(R.string.last_name)
    val emailLabel = stringResource(R.string.email)
    val addressLabel = stringResource(R.string.address)
    val cityLabel = stringResource(R.string.city)
    val heightLabel = stringResource(R.string.height_cm)
    val saveStr = stringResource(R.string.save)
    val saveIconCd = stringResource(R.string.save_icon_cd)
    val successMessage = stringResource(R.string.profile_updated_successfully)
    val heightInvalidMessage = stringResource(R.string.invalid_height)

    fun isHeightValid(height: String): Boolean {
        val heightValue = height.toIntOrNull()
        return heightValue != null && heightValue in 50..300
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(profileTitle, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = backStr)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Top
            ) {
                if (isSyncingWithFirestore.value) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedTextField(
                            value = firstName,
                            onValueChange = { firstName = it },
                            label = { Text(firstNameLabel) },
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            shape = RoundedCornerShape(5.dp)
                        )
                        OutlinedTextField(
                            value = lastName,
                            onValueChange = { lastName = it },
                            label = { Text(lastNameLabel) },
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp),
                            shape = RoundedCornerShape(5.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { /* Campo apenas leitura */ },
                        label = { Text(emailLabel) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(5.dp),
                        readOnly = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text(addressLabel) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(5.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedTextField(
                            value = city,
                            onValueChange = { city = it },
                            label = { Text(cityLabel) },
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            shape = RoundedCornerShape(5.dp)
                        )
                        OutlinedTextField(
                            value = height,
                            onValueChange = { height = it },
                            label = { Text(heightLabel) },
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp),
                            shape = RoundedCornerShape(5.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    if (errorMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = errorMessage,
                                color = MaterialTheme.colorScheme.error,
                                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                if (!isHeightValid(height)) {
                                    errorMessage = heightInvalidMessage
                                    return@launch
                                }

                                val updatedUser = User(
                                    userId = userId,
                                    firstName = firstName,
                                    lastName = lastName,
                                    email = email,
                                    address = address,
                                    city = city,
                                    height = height.toIntOrNull() ?: 0,
                                    password = "" // Ignora a senha
                                )

                                // Atualizar localmente
                                userDao.update(updatedUser)

                                // Atualizar no Firestore
                                firestoreViewModel.updateUserInFirestore(updatedUser) { success, error ->
                                    if (success) {
                                        Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show()
                                    } else {
                                        errorMessage = error ?: "Failed to update profile"
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(5.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(saveStr, color = MaterialTheme.colorScheme.onPrimary, fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                painter = painterResource(id = R.drawable.ic_save),
                                contentDescription = saveIconCd,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    )
}



