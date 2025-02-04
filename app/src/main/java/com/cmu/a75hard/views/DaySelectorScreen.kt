package com.cmu.a75hard.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.cmu.a75hard.R
import com.cmu.a75hard.model.DayData.DayData
import com.cmu.a75hard.model.DayData.DayDataDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DaySelectorScreen(navController: NavController, dayDataDao: DayDataDao, userId: Int) {
    val daysData = remember { mutableStateListOf<DayData>() }
    val coroutineScope = rememberCoroutineScope()

    var lastCompletedDay by remember { mutableStateOf(0) }

    val selectDayStr = stringResource(R.string.select_day)
    val backStr = stringResource(R.string.back)

    // LaunchedEffect para carregar apenas os dados do usuário autenticado
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            daysData.clear()
            // Consulta a base de dados em uma thread de background
            val data = withContext(Dispatchers.IO) {
                dayDataDao.getAllDaysDataForUser(userId)
            }
            daysData.addAll(data)

            // Determina o último dia concluído
            for (day in daysData) {
                if (day.isCompleted) {
                    lastCompletedDay = day.dayNumber
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectDayStr) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = backStr
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        // Grelha de dias
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            contentPadding = paddingValues,
            modifier = Modifier.padding(16.dp)
        ) {
            items(75) { index ->
                val dayNumber = index + 1
                val dayData = daysData.find { it.dayNumber == dayNumber }
                val isCompleted = dayData?.isCompleted == true

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .padding(4.dp)
                        .background(if (isCompleted) Color.Black else Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dayNumber.toString(),
                        color = if (isCompleted) Color.White else Color.Black,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}
