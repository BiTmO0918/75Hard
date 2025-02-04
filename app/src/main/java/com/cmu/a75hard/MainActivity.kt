package com.cmu.a75hard

import android.Manifest
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cmu.a75hard.components.BottomNavigationBar
import com.cmu.a75hard.components.ChallengeProgressManager
import com.cmu.a75hard.model.DayData.DayDataDao
import com.cmu.a75hard.model.MyDatabase
import com.cmu.a75hard.model.user.UserDao
import com.cmu.a75hard.repository.DayDataRepository
import com.cmu.a75hard.ui.theme._75HardTheme
import com.cmu.a75hard.utils.LanguagePreferences
import com.cmu.a75hard.utils.LocaleManager
import com.cmu.a75hard.utils.ThemePreferences
import com.cmu.a75hard.viewmodel.AuthViewModel
import com.cmu.a75hard.viewmodel.FirestoreViewModel
import com.cmu.a75hard.views.*
import com.cmu.a75hard.workers.ScheduleReminder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var dayDataDao: DayDataDao
    private lateinit var userDao: UserDao
    private lateinit var themePreferences: ThemePreferences
    lateinit var languagePreferences: LanguagePreferences
    private var currentLanguage by mutableStateOf("en")

    override fun attachBaseContext(newBase: Context) {
        val tempPrefs = LanguagePreferences(newBase)

        // Verifica se é a primeira execução
        val isFirstLaunch = tempPrefs.isFirstLaunch()

        // Define o idioma inicial
        val lang = if (isFirstLaunch) {
            val systemLanguage = Locale.getDefault().language
            tempPrefs.saveLanguagePreference(systemLanguage) // Salva o idioma inicial
            tempPrefs.setFirstLaunch(false) // Marca que a primeira execução já ocorreu
            systemLanguage
        } else {
            tempPrefs.getLanguagePreference() // Usa o idioma salvo nas preferências
        }

        // Atualiza o contexto com o idioma configurado
        val context = LocaleManager.updateBaseContextLocale(newBase, lang)
        super.attachBaseContext(context)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializações das preferências
        themePreferences = ThemePreferences(this)
        languagePreferences = LanguagePreferences(this)

        // Inicializa a base de dados e DAOs
        val database = MyDatabase.getDatabase(applicationContext)
        dayDataDao = database.dayDataDao()
        userDao = database.userDao()

        val firestoreViewModel = FirestoreViewModel() // Inicializa o FirestoreViewModel

        val sharedPrefs = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val userId = sharedPrefs.getInt("userId", -1) // Obter o userId armazenado

        val challengeProgressManager = ChallengeProgressManager(applicationContext)
        if (userId != -1) {
            // Recalcula o currentDay baseado no tempo, já que o desafio está em andamento
            challengeProgressManager.updateCurrentDayFromTime()
        }

        if (userId != -1) {
            val dayDataRepository = DayDataRepository(dayDataDao, userDao)
            CoroutineScope(Dispatchers.IO).launch {
                dayDataRepository.syncDayDataToFirestore(userId, firestoreViewModel, this@MainActivity)
            }
        }


        // Verifica se é a primeira execução
        val isFirstLaunch = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getBoolean("is_first_launch", true)

        // Define o tema inicial
        val isDarkThemeSaved = themePreferences.getThemePreference()

        // Detectar tema do dispositivo se for a primeira vez
        val isDarkThemeSystem = resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        var isDarkTheme by mutableStateOf(
            if (isFirstLaunch) isDarkThemeSystem else isDarkThemeSaved
        )

        if (isFirstLaunch) {
            // Marca como inicialização completa
            getSharedPreferences("app_prefs", Context.MODE_PRIVATE).edit()
                .putBoolean("is_first_launch", false).apply()
            // Salvar o estado inicial como o tema do sistema
            themePreferences.saveThemePreference(isDarkThemeSystem)
        }

        currentLanguage = languagePreferences.getLanguagePreference()

        // Habilita o modo de borda a borda
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Verifica se o modo de economia de bateria está ativado
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val isBatterySaverEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            powerManager.isPowerSaveMode
        } else {
            false
        }

        setContent {
            // Criação dos ViewModels
            val firestoreViewModel = provideFirestoreViewModel()
            val authViewModel = provideAuthViewModel(userDao, firestoreViewModel)
            val navController = rememberNavController()
            val progressManager = ChallengeProgressManager(applicationContext) // Inicialize o ProgressManager
            val currentDay = progressManager.currentDay

            // Determina a tela inicial com base no estado de autenticação
            val startDestination = if (authViewModel.isUserLoggedIn(applicationContext)) {
                "dailyTasks/$currentDay"
            } else {
                "loginScreen"
            }

            _75HardTheme(darkTheme = isDarkTheme) {
                Scaffold(
                    bottomBar = {
                        val currentRoute by navController.currentBackStackEntryAsState()
                        val currentDestination = currentRoute?.destination?.route

                        if (currentDestination in listOf("accountPage", "dailyTasks/{dayNumber}", "weight")) {
                            BottomNavigationBar(
                                navController = navController,
                                currentRoute = currentDestination,
                                context = this@MainActivity
                            )
                        }
                    }
                ) { paddingValues ->
                    Column(modifier = Modifier.padding(paddingValues)) {
                        NavHost(
                            navController = navController,
                            startDestination = startDestination
                        ) {
                            composable("dailyTasks") {
                                DailyTasksScreen(navController, dayDataDao, userDao)
                            }
                            composable("progressList") {
                                ProgressListScreen(navController, dayDataDao)
                            }
                            composable("accountPage") {
                                AccountPage(
                                    navController = navController,
                                    isDarkTheme = isDarkTheme,
                                    onThemeChange = { newTheme ->
                                        if (!isBatterySaverEnabled) {
                                            isDarkTheme = newTheme
                                            themePreferences.saveThemePreference(newTheme)
                                        }
                                    },
                                    authViewModel = authViewModel,
                                    themePreferences = themePreferences
                                )
                            }
                            composable("daySelector") {
                                DaySelectorScreen(
                                    navController = navController,
                                    dayDataDao = dayDataDao,
                                    userId = userId
                                )
                            }
                            composable("dailyTasks/{dayNumber}") { backStackEntry ->
                                val dayNumber = backStackEntry.arguments?.getString("dayNumber")?.toInt() ?: 1
                                DailyTasksScreen(
                                    navController = navController,
                                    dayDataDao = dayDataDao, // Já existente
                                    userDao = userDao        // Adicione o UserDao aqui
                                )
                            }
                            composable("outdoorWorkout/{userId}/{dayNumber}") { backStackEntry ->
                                val userId = backStackEntry.arguments?.getString("userId")?.toInt() ?: -1
                                val dayNumber = backStackEntry.arguments?.getString("dayNumber")?.toInt() ?: 1
                                OutdoorWorkoutScreen(navController, dayDataDao, userId, dayNumber)
                            }

                            composable("warmUp") {
                                WarmUpScreen(navController)
                            }
                            composable("loginScreen") {
                                LoginScreen(navController, authViewModel)
                            }
                            composable("SignUpScreen") {
                                SignUpScreen(navController, authViewModel)
                            }
                            composable("feedbackScreen") {
                                FeedbackScreen(navController, firestoreViewModel)
                            }
                            composable("profileScreen") {
                                ProfileScreen(
                                    navController = navController,
                                    userDao = userDao,
                                    firestoreViewModel = firestoreViewModel
                                )
                            }
                            composable("recipeListScreen") {
                                RecipeListScreen(navController, remember { mutableStateOf("Beef") })
                            }
                            composable("RecipeDetailsScreen/{mealId}") { backStackEntry ->
                                val mealId = backStackEntry.arguments?.getString("mealId")
                                if (mealId != null) {
                                    RecipeDetailsScreen(navController, mealId)
                                }
                            }
                            composable("RankingScreen") {
                                RankingScreen(navController, firestoreViewModel)
                            }
                            composable("weight") {
                                WeightScreen(
                                    dayDataDao = dayDataDao,
                                    userDao = userDao,
                                    navController = navController
                                )
                            }
                            composable("cameraXScreen/{dayNumber}/{userId}") { backStackEntry ->
                                val dayNumber = backStackEntry.arguments?.getString("dayNumber")?.toInt() ?: 1
                                val userId = backStackEntry.arguments?.getString("userId")?.toInt() ?: -1

                                CameraXScreen(
                                    navController = navController,
                                    dayDataDao = dayDataDao,
                                    dayNumber = dayNumber,
                                    userId = userId,
                                    onClose = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun updateLanguage(languageCode: String) {
        languagePreferences.saveLanguagePreference(languageCode)
        recreate()
    }

    private fun provideFirestoreViewModel(): FirestoreViewModel {
        return ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(FirestoreViewModel::class.java)) {
                        @Suppress("UNCHECKED_CAST")
                        return FirestoreViewModel() as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        )[FirestoreViewModel::class.java]
    }

    private fun provideAuthViewModel(userDao: UserDao, firestoreViewModel: FirestoreViewModel): AuthViewModel {
        return ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                        @Suppress("UNCHECKED_CAST")
                        return AuthViewModel(userDao, firestoreViewModel) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        )[AuthViewModel::class.java]
    }

    private fun requestNotificationPermissionAndScheduleReminder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val requestPermissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    ScheduleReminder.scheduleDailyReminder(this)
                }
            }
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            ScheduleReminder.scheduleDailyReminder(this)
        }
    }
}
