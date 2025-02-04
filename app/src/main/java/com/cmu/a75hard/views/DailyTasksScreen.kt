package com.cmu.a75hard.views

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.cmu.a75hard.R
import com.cmu.a75hard.components.ChallengeProgressManager
import com.cmu.a75hard.model.DayData.DayData
import com.cmu.a75hard.model.DayData.DayDataDao
import com.cmu.a75hard.model.DayData.WorkoutData
import kotlinx.coroutines.launch
import java.io.File
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.cmu.a75hard.model.user.UserDao
import com.cmu.a75hard.repository.DayDataRepository
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyTasksScreen(
    navController: NavController,
    dayDataDao: DayDataDao,
    userDao: UserDao
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
    val userId = sharedPref.getInt("userId", -1) // Recupera o userId armazenado, -1 indica erro

    if (userId == -1) {
        // Caso userId não esteja disponível, redireciona para a tela de login
        navController.navigate("loginScreen")
        return
    }

    val challengeProgressManager = remember { ChallengeProgressManager(context) }
    val dayDataRepository = remember { DayDataRepository(dayDataDao, userDao) }

    var dayData by remember { mutableStateOf<DayData?>(null) }
    var showCameraDialog by remember { mutableStateOf(false) }
    var challengeRestarted by remember { mutableStateOf(false) }
    var challengeCompleted by remember { mutableStateOf(false) }
    var currentPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var showCameraX by remember { mutableStateOf(false) }

    val (cameraLauncher, galleryLauncher) = rememberCameraAndGalleryLaunchers(
        context = context,
        currentPhotoUri = currentPhotoUri,
        onImageCaptured = { uri ->
            scope.launch {
                dayData?.let {
                    val updatedData = it.copy(progressPictureUrl = uri.toString())
                    val finalData = if (isCompleted(updatedData)) {
                        updatedData.copy(isCompleted = true)
                    } else {
                        updatedData
                    }
                    dayDataDao.insertDayData(finalData)
                    dayData = finalData
                }
            }
        },
        onImageSelected = { uri ->
            scope.launch {
                dayData?.let {
                    val updatedData = it.copy(progressPictureUrl = uri.toString())
                    val finalData = if (isCompleted(updatedData)) {
                        updatedData.copy(isCompleted = true)
                    } else {
                        updatedData
                    }
                    dayDataDao.insertDayData(finalData)
                    dayData = finalData
                }
            }
        }
    )


    LaunchedEffect(Unit) {
        if (userId != -1) {
            // Remover a chamada de syncCurrentDayWithDatabase
            // challengeProgressManager.syncCurrentDayWithDatabase(userId, dayDataDao)

            dayData = dayDataRepository.getDayDataForUser(challengeProgressManager.currentDay, userId)
                ?: DayData(dayNumber = challengeProgressManager.currentDay, userId = userId)
        }
    }

    val dayNumber = challengeProgressManager.currentDay

    // Obter todos os strings necessários dentro do composable
    val dayString = stringResource(R.string.day)
    val indoorWorkoutTitle = stringResource(R.string.indoor_workout)
    val outdoorWorkoutTitle = stringResource(R.string.outdoor_workout)
    val dietTitle = stringResource(R.string.diet)
    val readingTitle = stringResource(R.string.reading)
    val waterTitle = stringResource(R.string.water)
    val progressPictureTitle = stringResource(R.string.progress_picture)
    val noAlcoholTitle = stringResource(R.string.no_alcohol)

    val indoorWorkoutDescription = stringResource(R.string.indoor_workout_description)
    val outdoorWorkoutDescription = stringResource(R.string.outdoor_workout_description)
    val dietDescription = stringResource(R.string.diet_description)
    val readingDescription = stringResource(R.string.reading_description)
    val waterDescription = stringResource(R.string.water_description)
    val progressPictureDescription = stringResource(R.string.progress_picture_description)
    val noAlcoholDescription = stringResource(R.string.no_alcohol_description)

    val viewProgressPhotos = stringResource(R.string.view_progress_photos)
    val selectDay = stringResource(R.string.select_day)
    val chooseProgressPhotoOption = stringResource(R.string.choose_progress_photo_option)
    val takeAPhoto = stringResource(R.string.take_a_photo)
    val chooseFromGallery = stringResource(R.string.choose_from_gallery)
    val challengeRestartedTitle = stringResource(R.string.challenge_restarted)
    val challengeRestartedDescription = stringResource(R.string.challenge_restarted_description)
    val understood = stringResource(R.string.understood)
    val congratulations = stringResource(R.string.congratulations)
    val challengeCompletedDescription = stringResource(R.string.challenge_completed_description)
    val ok = stringResource(R.string.ok)
    val restartChallenge = stringResource(R.string.restart_challenge)
    val playString = stringResource(R.string.play)
    val cameraString = stringResource(R.string.camera)
    val taskIconDescriptionFormat = stringResource(R.string.task_icon_description)

    fun updateTask(data: DayData, title: String, completed: Boolean): DayData {
        return when (title) {
            dietTitle -> data.copy(diet = completed)
            readingTitle -> data.copy(reading = completed)
            waterTitle -> data.copy(waterIntake = if (completed) 3.7 else 0.0)
            noAlcoholTitle -> data.copy(noAlcohol = completed)
            indoorWorkoutTitle -> data.copy(
                indoorWorkout = if (completed) data.indoorWorkout
                    ?: WorkoutData(
                        duration = "45 min",
                        maxSpeed = 0.0,
                        pace = "0:00",
                        caloriesBurned = 0,
                        distance = 0.0,
                        averageHeartRate = 0,
                        maxAcceleration = 0.0,
                        steps = 0
                    ) else null
            )
            outdoorWorkoutTitle -> data.copy(
                outdoorWorkout = if (completed) data.outdoorWorkout
                    ?: WorkoutData(
                        duration = "45 min",
                        maxSpeed = 0.0,
                        pace = "0:00",
                        caloriesBurned = 0,
                        distance = 0.0,
                        averageHeartRate = 0,
                        maxAcceleration = 0.0,
                        steps = 0
                    ) else null
            )
            else -> data
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "$dayString $dayNumber") },
                actions = {
                    IconButton(onClick = { navController.navigate("progressList") }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_gallery),
                            contentDescription = viewProgressPhotos
                        )
                    }
                    IconButton(onClick = { navController.navigate("daySelector") }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_calendar),
                            contentDescription = selectDay
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            dayData?.let { data ->
                val tasks = listOf(
                    TaskData(
                        R.drawable.ic_indoor_workout,
                        indoorWorkoutTitle,
                        indoorWorkoutDescription,
                        data.indoorWorkout != null
                    ),
                    TaskData(
                        R.drawable.ic_outdoor_workout,
                        outdoorWorkoutTitle,
                        outdoorWorkoutDescription,
                        data.outdoorWorkout != null,
                        showPlayButton = true
                    ),
                    TaskData(
                        R.drawable.ic_diet,
                        dietTitle,
                        dietDescription,
                        data.diet
                    ),
                    TaskData(
                        R.drawable.ic_reading,
                        readingTitle,
                        readingDescription,
                        data.reading
                    ),
                    TaskData(
                        R.drawable.ic_water,
                        waterTitle,
                        waterDescription,
                        data.waterIntake >= 3.7
                    ),
                    TaskData(
                        R.drawable.ic_gallery,
                        progressPictureTitle,
                        progressPictureDescription,
                        data.progressPictureUrl != null,
                        showCameraIcon = true
                    ),
                    TaskData(
                        R.drawable.ic_no_alcohol,
                        noAlcoholTitle,
                        noAlcoholDescription,
                        data.noAlcohol
                    )
                )

                val dayCompleted = data.isCompleted
                tasks.forEach { task ->
                    DailyTaskItem(
                        iconId = task.iconId,
                        title = task.title,
                        description = task.description,
                        isCompleted = task.isCompleted,
                        showPlayButton = task.showPlayButton,
                        showCameraIcon = task.showCameraIcon,
                        onPlayClick = {
                            if (!dayCompleted && task.title == outdoorWorkoutTitle) {
                                navController.navigate("outdoorWorkout/$userId/$dayNumber")
                            }
                        },
                        onCameraClick = {
                            navController.navigate("cameraXScreen/$dayNumber/$userId")
                        },
                        onToggle = { completed ->
                            if (!dayCompleted) {
                                scope.launch {
                                    val updatedData = updateTask(data, task.title, completed)
                                    val finalData = if (isCompleted(updatedData)) {
                                        updatedData.copy(isCompleted = true)
                                    } else {
                                        updatedData
                                    }
                                    dayDataDao.insertDayData(finalData)
                                    dayData = finalData
                                }
                            }
                        },
                        playString = playString,
                        cameraString = cameraString,
                        taskIconDescriptionFormat = taskIconDescriptionFormat,
                        enabled = !dayCompleted
                    )
                }
            }
        }
    }
    if (showCameraX) {
        dayData?.let { currentDayData ->
            navController.currentBackStackEntry
                ?.savedStateHandle
                ?.getLiveData<String>("capturedImageUri")
                ?.observe(LocalLifecycleOwner.current) { uriString ->
                    uriString?.let { uri ->
                        scope.launch {
                            withContext(Dispatchers.Main) {
                                val updatedData = currentDayData.copy(progressPictureUrl = uri)
                                dayDataDao.insertDayData(updatedData)
                                dayData = updatedData
                            }
                        }
                    }
                }

            navController.navigate("cameraXScreen/${currentDayData.dayNumber}/${currentDayData.userId}")
        }
    }


    if (challengeRestarted) {
        AlertDialog(
            onDismissRequest = { challengeRestarted = false },
            title = { Text(challengeRestartedTitle) },
            text = { Text(challengeRestartedDescription) },
            confirmButton = {
                TextButton(onClick = { challengeRestarted = false }) {
                    Text(understood)
                }
            }
        )
    }

    if (challengeCompleted) {
        AlertDialog(
            onDismissRequest = { challengeCompleted = false },
            title = { Text(congratulations) },
            text = { Text(challengeCompletedDescription) },
            confirmButton = {
                TextButton(onClick = {
                    challengeCompleted = false
                }) {
                    Text(ok)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    challengeCompleted = false
                    challengeProgressManager.resetProgress()
                    scope.launch {
                        dayDataDao.clearAllData()
                        dayData = DayData(dayNumber = 1, userId = userId)
                    }
                }) {
                    Text(restartChallenge)
                }
            }
        )
    }
}

data class TaskData(
    val iconId: Int,
    val title: String,
    val description: String,
    val isCompleted: Boolean,
    val showPlayButton: Boolean = false,
    val showCameraIcon: Boolean = false
)

@Composable
fun DailyTaskItem(
    iconId: Int,
    title: String,
    description: String,
    isCompleted: Boolean,
    showPlayButton: Boolean = false,
    showCameraIcon: Boolean = false,
    onPlayClick: () -> Unit = {},
    onCameraClick: () -> Unit = {},
    onToggle: (Boolean) -> Unit,
    playString: String,
    cameraString: String,
    taskIconDescriptionFormat: String,
    enabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconId),
            contentDescription = String.format(taskIconDescriptionFormat, title),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(text = description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        when {
            showPlayButton -> {
                IconButton(onClick = { if (enabled) onPlayClick() }, enabled = enabled) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_play_circle),
                        contentDescription = playString
                    )
                }
            }
            showCameraIcon -> {
                IconButton(onClick = { if (enabled) onCameraClick() }, enabled = enabled) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_camera),
                        contentDescription = cameraString
                    )
                }
            }
            else -> {
                Checkbox(
                    checked = isCompleted,
                    onCheckedChange = { if (enabled) onToggle(it) },
                    enabled = enabled
                )
            }
        }
    }
}

fun calculateDaysSinceStart(startDateMillis: Long): Long {
    val currentMillis = System.currentTimeMillis()
    val difference = currentMillis - startDateMillis
    return TimeUnit.MILLISECONDS.toDays(difference)
}

private suspend fun checkAllTasksCompleted(dayDataDao: DayDataDao, userId: Int, expectedDayNumber: Int): Boolean {
    for (day in 1 until expectedDayNumber) {
        val dayData = dayDataDao.getDayDataForUser(day, userId)
        val allTasksCompleted = dayData?.let {
            isCompleted(it)
        } ?: false

        if (!allTasksCompleted) {
            return false
        }
    }
    return true
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

fun openCamera(context: Context, cameraLauncher: ActivityResultLauncher<Uri>, onUriCreated: (Uri) -> Unit) {
    val photoFile = createImageFile(context)
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        photoFile
    )
    onUriCreated(uri)
    cameraLauncher.launch(uri)
}

fun openGallery(galleryLauncher: ActivityResultLauncher<String>) {
    galleryLauncher.launch("image/*")
}

private fun createImageFile(context: Context): File {
    val imageFileName = "progress_image_${System.currentTimeMillis()}"
    val storageDir = context.getExternalFilesDir(null)
    return File.createTempFile(imageFileName, ".jpg", storageDir)
}

@Composable
private fun rememberCameraAndGalleryLaunchers(
    context: Context,
    currentPhotoUri: Uri?,
    onImageCaptured: (Uri) -> Unit,
    onImageSelected: (Uri) -> Unit
): Pair<ActivityResultLauncher<Uri>, ActivityResultLauncher<String>> {
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            currentPhotoUri?.let { uri ->
                onImageCaptured(uri)
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            onImageSelected(it)
        }
    }

    return Pair(cameraLauncher, galleryLauncher)
}
