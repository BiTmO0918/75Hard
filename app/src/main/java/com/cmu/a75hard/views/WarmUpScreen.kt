package com.cmu.a75hard.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import com.cmu.a75hard.R
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontWeight

data class Exercise(
    val id: String,
    val name: String,
    val description: String,
    val imageResId: Int?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarmUpScreen(navController: NavController) {
    // Obtém as strings
    val warmUpTitle = stringResource(R.string.warm_up_title)
    val backStr = stringResource(R.string.back)
    val bearCrawlStr = stringResource(R.string.bear_crawl)
    val jumpingJacksStr = stringResource(R.string.jumping_jacks)
    val plankStr = stringResource(R.string.plank)
    val lungeStr = stringResource(R.string.lunge)
    val staticSquatStr = stringResource(R.string.static_squat)
    val duration30SecStr = stringResource(R.string.duration_30_seconds)
    val duration20RepsStr = stringResource(R.string.duration_20_reps)
    val duration1MinStr = stringResource(R.string.duration_1_minute)

    val duration5RepsEachLegStr = stringResource(R.string.duration_5_reps_each_leg)
    val duration15SecStr = stringResource(R.string.duration_15_seconds)

    // Lista de exercícios com strings do resources
    val warmUpExercises = listOf(
        Exercise("1", bearCrawlStr, duration30SecStr, R.drawable.i_bear_crawl),
        Exercise("2", jumpingJacksStr, duration20RepsStr, R.drawable.i_jumping_jacks),
        Exercise("3", plankStr, duration1MinStr, R.drawable.i_plank),
        Exercise("4", lungeStr, duration5RepsEachLegStr, R.drawable.i_lunge),
        Exercise("5", staticSquatStr, duration15SecStr, R.drawable.i_static_quat)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(warmUpTitle) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = backStr
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues)) {
            items(warmUpExercises) { exercise ->
                ExerciseItem(exercise = exercise)
            }
        }
    }
}

@Composable
fun ExerciseItem(exercise: Exercise) {
    val imageUnavailableStr = stringResource(R.string.image_unavailable)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        exercise.imageResId?.let { imageResId ->
            Image(
                painter = painterResource(id = imageResId),
                contentDescription = exercise.name,
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
            )
        } ?: run {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = imageUnavailableStr,
                    color = MaterialTheme.colorScheme.onTertiary,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = exercise.name,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = exercise.description,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}
