package com.cmu.a75hard.views

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.cmu.a75hard.R
import com.cmu.a75hard.components.ChallengeProgressManager
import com.cmu.a75hard.model.DayData.DayData
import com.cmu.a75hard.model.DayData.DayDataDao
import com.cmu.a75hard.model.user.UserDao
import com.cmu.a75hard.repository.DayDataRepository
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun WeightScreen(
    dayDataDao: DayDataDao,
    userDao: UserDao,
    navController: NavController
) {
    val repository = remember { DayDataRepository(dayDataDao, userDao) }
    val coroutineScope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    var reloadChart by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Obter o userId das SharedPreferences
    val sharedPrefs = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
    val userId = sharedPrefs.getInt("userId", -1)

    val challengeProgressManager = remember { ChallengeProgressManager(context) }
    val currentDay = remember { mutableStateOf(challengeProgressManager.currentDay) }

    LaunchedEffect(Unit) {
        if (userId != -1) {
            currentDay.value = challengeProgressManager.currentDay
        }
    }

    Scaffold(
        bottomBar = {
            Column {
                AddDataButton { showDialog = true }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Título principal
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.weight), fontSize = 24.sp)
                Row {
                    // Botão de Dieta
                    IconButton(onClick = { navController.navigate("recipeListScreen") }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_diet),
                            contentDescription = stringResource(R.string.recipes)
                        )
                    }
                    // Botão de Ranking
                    IconButton(onClick = { navController.navigate("RankingScreen") }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_trophy),
                            contentDescription = stringResource(R.string.ranking)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Gráfico de Histórico de Peso
            Text(stringResource(R.string.history), fontSize = 18.sp)
            Spacer(modifier = Modifier.height(10.dp))
            WeightHistoryChart(repository, userId, reloadChart)

            Spacer(modifier = Modifier.height(30.dp))

            // Gráfico de Mudança Diária
            Text(stringResource(R.string.daily_change), fontSize = 18.sp)
            Spacer(modifier = Modifier.height(10.dp))
            WeightDailyChangeChart(repository, userId, reloadChart)

            if (showDialog) {
                WeightInputDialog(
                    repository = repository,
                    dayNumber = currentDay.value,
                    onDismiss = {
                        showDialog = false
                        reloadChart = !reloadChart
                    },
                    coroutineScope = coroutineScope
                )
            }
        }
    }
}

@Composable
fun AddDataButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF333333))
            .padding(12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.add_data), color = Color.White, fontSize = 16.sp)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color.White, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = onClick) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.add_data),
                        tint = Color.Black
                    )
                }
            }
        }
    }
}

@Composable
fun WeightHistoryChart(repository: DayDataRepository, userId: Int, reload: Boolean) {
    val weights = remember { mutableStateListOf<Entry>() }
    val isDarkTheme = MaterialTheme.colorScheme.background == Color(0xFF121212) // Detecção do tema escuro

    LaunchedEffect(reload) {
        weights.clear()
        if (userId != -1) {
            val weightList = repository.getAllWeightsForUser(userId)
            if (weightList.isNotEmpty()) {
                weights.addAll(weightList.mapIndexed { index, value -> Entry(index.toFloat(), value) })
            }
        }
    }

    if (weights.isNotEmpty()) {
        AndroidView(
            factory = { ctx: Context ->
                LineChart(ctx).apply {
                    val dataSet = LineDataSet(weights, ctx.getString(R.string.weight)).apply {
                        color = if (isDarkTheme) android.graphics.Color.LTGRAY else android.graphics.Color.DKGRAY
                        valueTextColor = if (isDarkTheme) android.graphics.Color.WHITE else android.graphics.Color.BLACK
                        setDrawCircles(true)
                        setDrawValues(true)
                        setDrawFilled(true)
                        mode = LineDataSet.Mode.CUBIC_BEZIER
                        fillColor = if (isDarkTheme) android.graphics.Color.DKGRAY else android.graphics.Color.LTGRAY
                    }

                    data = LineData(dataSet)

                    xAxis.apply {
                        position = XAxis.XAxisPosition.BOTTOM
                        textColor = if (isDarkTheme) android.graphics.Color.WHITE else android.graphics.Color.BLACK
                        setDrawGridLines(false)
                    }

                    axisLeft.textColor = if (isDarkTheme) android.graphics.Color.WHITE else android.graphics.Color.BLACK
                    axisRight.isEnabled = false

                    legend.apply {
                        textColor = if (isDarkTheme) android.graphics.Color.WHITE else android.graphics.Color.BLACK
                    }

                    description.apply {
                        isEnabled = false
                    }

                    setTouchEnabled(false)
                    invalidate()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )
    }
}

@Composable
fun WeightDailyChangeChart(repository: DayDataRepository, userId: Int, reload: Boolean) {
    val changes = remember { mutableStateListOf<BarEntry>() }
    val colors = remember { mutableStateListOf<Int>() }
    val isDarkTheme = MaterialTheme.colorScheme.background == Color(0xFF121212) // Detecção do tema escuro

    LaunchedEffect(reload) {
        changes.clear()
        colors.clear()
        if (userId != -1) {
            val changesList = repository.getDailyWeightChangesForUser(userId)
            if (changesList.isNotEmpty()) {
                changes.addAll(changesList.mapIndexed { index, value -> BarEntry(index.toFloat(), value) })
                colors.addAll(changesList.map { if (it >= 0) android.graphics.Color.GREEN else android.graphics.Color.RED })
            }
        }
    }

    if (changes.isNotEmpty()) {
        AndroidView(
            factory = { ctx: Context ->
                BarChart(ctx).apply {
                    val dataSet = BarDataSet(changes, ctx.getString(R.string.daily_change)).apply {
                        valueTextColor = if (isDarkTheme) android.graphics.Color.WHITE else android.graphics.Color.BLACK
                        this.colors = colors
                    }

                    data = BarData(dataSet)
                    xAxis.apply {
                        position = XAxis.XAxisPosition.BOTTOM
                        textColor = if (isDarkTheme) android.graphics.Color.WHITE else android.graphics.Color.BLACK
                        setDrawGridLines(false)
                    }

                    axisLeft.textColor = if (isDarkTheme) android.graphics.Color.WHITE else android.graphics.Color.BLACK
                    axisRight.isEnabled = false

                    legend.apply {
                        textColor = if (isDarkTheme) android.graphics.Color.WHITE else android.graphics.Color.BLACK
                    }

                    description.apply {
                        isEnabled = false
                    }

                    setTouchEnabled(false)
                    invalidate()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )
    } else {
        Text(
            stringResource(R.string.not_enough_data_for_daily_change),
            modifier = Modifier.padding(16.dp),
            color = if (isDarkTheme) Color.White else Color.Black
        )
    }
}


@Composable
fun WeightInputDialog(
    repository: DayDataRepository,
    dayNumber: Int,
    onDismiss: () -> Unit,
    coroutineScope: CoroutineScope
) {
    var weight by remember { mutableStateOf("") }
    val context = LocalContext.current
    val sharedPrefs = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
    val userId = sharedPrefs.getInt("userId", -1)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.enter_weight_for_day, dayNumber)) },
        text = {
            OutlinedTextField(
                value = weight,
                onValueChange = { weight = it },
                label = { Text(stringResource(R.string.enter_weight)) }
            )
        },
        confirmButton = {
            Button(onClick = {
                val weightValue = weight.toFloatOrNull()
                if (weightValue != null && userId != -1) {
                    coroutineScope.launch {
                        val existingDayData = repository.getDayDataForUser(dayNumber, userId)
                        if (existingDayData != null) {
                            repository.updateWeightForDay(dayNumber, weightValue)
                        } else {
                            repository.insertOrUpdateDayData(
                                DayData(dayNumber = dayNumber, userId = userId, weight = weightValue)
                            )
                        }

                        // Atualiza o campo weightLost do utilizador
                        repository.updateWeightLost(userId)
                        onDismiss()
                    }
                }
            }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
