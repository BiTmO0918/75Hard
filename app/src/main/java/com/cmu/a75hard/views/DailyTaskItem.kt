package com.cmu.a75hard.views

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmu.a75hard.R
import com.cmu.a75hard.model.MyDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun DailyTaskItem(
    iconId: Int,
    title: String,
    description: String,
    dayNumber: Int,
    showPlayButton: Boolean = false,
    showCameraIcon: Boolean = false,
    onPlayClick: () -> Unit = {},
    onCameraClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val dayDataDao = MyDatabase.getDatabase(context).dayDataDao()
    var isCompleted by remember { mutableStateOf(false) }

    // Carregar o status de conclusão da tarefa do banco de dados
    LaunchedEffect(dayNumber) {
        val dayData = dayDataDao.getDayData(dayNumber)
        isCompleted = dayData?.isCompleted ?: false
    }

    // UI para cada item da tarefa diária
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconId),
            contentDescription = "$title Icon",
            tint = Color.Black,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 16.sp)
            Text(text = description, fontSize = 14.sp, color = Color.Gray)
        }

        // Exibir botão de play, ícone de câmera ou checkbox
        when {
            showPlayButton -> {
                IconButton(onClick = onPlayClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_play_circle),
                        contentDescription = "Play",
                        tint = Color.Black
                    )
                }
            }
            showCameraIcon -> {
                IconButton(onClick = onCameraClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_camera),
                        contentDescription = "Camera",
                        tint = Color.Black
                    )
                }
            }
            else -> {
                Checkbox(
                    checked = isCompleted,
                    onCheckedChange = { checked ->
                        isCompleted = checked
                        // Atualizar o status de conclusão no banco de dados
                        CoroutineScope(Dispatchers.IO).launch {
                            dayDataDao.updateDayCompletionStatus(dayNumber, isCompleted)
                        }
                    }
                )
            }
        }
    }
}

