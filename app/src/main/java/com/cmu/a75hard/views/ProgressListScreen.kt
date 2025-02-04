package com.cmu.a75hard.views

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.cmu.a75hard.R
import com.cmu.a75hard.model.DayData.DayDataDao
import com.cmu.a75hard.repository.DayDataRepository
import kotlinx.coroutines.launch

@Composable
fun ProgressListScreen(navController: NavController, dayDataDao: DayDataDao) {

    val context = navController.context
    val sharedPrefs = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
    val userId = sharedPrefs.getInt("userId", -1)

    // Crie uma instância do repositório:
    val repository = remember {
        // Obtenha userDao se precisar. Supondo que tenha acesso ao MyDatabase como antes:
        val database = com.cmu.a75hard.model.MyDatabase.getDatabase(context)
        DayDataRepository(dayDataDao, database.userDao())
    }

    val scope = rememberCoroutineScope()
    var progressImages by remember { mutableStateOf<List<DayDataDao.ProgressImage>>(emptyList()) }

    val backStr = stringResource(R.string.back)
    val yourProgressStr = stringResource(R.string.your_progress)
    val progressPictureForDayStr = stringResource(R.string.progress_picture_for_day)
    val dayFormatStr = stringResource(R.string.day_format)

    // Carrega os dados de URLs de progresso da BD
    LaunchedEffect(Unit) {
        scope.launch {
            if (userId != -1) {
                progressImages = repository.getProgressUrlsForUser(userId)
            } else {
                progressImages = emptyList()
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = backStr
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = yourProgressStr, style = MaterialTheme.typography.titleLarge)
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(top = 16.dp)
        ) {
            items(progressImages) { progressImage ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AsyncImage(
                        model = progressImage.progressPictureUrl,
                        contentDescription = String.format(progressPictureForDayStr, progressImage.dayNumber),
                        placeholder = painterResource(R.drawable.ic_gallery),
                        error = painterResource(R.drawable.ic_gallery),
                        modifier = Modifier.size(100.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = String.format(dayFormatStr, progressImage.dayNumber),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}
