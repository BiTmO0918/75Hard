package com.cmu.a75hard.components

import android.content.Context
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.cmu.a75hard.R
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.cmu.a75hard.components.ChallengeProgressManager

@Composable
fun BottomNavigationBar(navController: NavController, currentRoute: String?, context: Context) {
    // ProgressManager para acessar o progresso atual
    val progressManager = remember { ChallengeProgressManager(context) }
    var currentDay by remember { mutableStateOf(progressManager.currentDay) }

    // Sempre que o BottomNavigationBar for recomposto, atualize o currentDay
    LaunchedEffect(Unit) {
        currentDay = progressManager.currentDay
    }

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.background
    ) {
        NavigationBarItem(
            icon = { Icon(painter = painterResource(id = R.drawable.ic_today), contentDescription = stringResource(R.string.today)) },
            label = { Text(stringResource(R.string.today)) },
            selected = currentRoute?.startsWith("dailyTasks") == true,
            onClick = {
                // Atualiza o dia antes de navegar
                currentDay = progressManager.currentDay
                navController.navigate("dailyTasks/$currentDay") {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onSecondary,
                selectedTextColor = MaterialTheme.colorScheme.onSecondary,
                indicatorColor = MaterialTheme.colorScheme.surface
            )
        )
        NavigationBarItem(
            icon = { Icon(painter = painterResource(id = R.drawable.ic_weight), contentDescription = stringResource(R.string.weight)) },
            label = { Text(stringResource(R.string.weight)) },
            selected = currentRoute?.startsWith("weight") == true,
            onClick = {
                navController.navigate("weight") {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onSecondary,
                selectedTextColor = MaterialTheme.colorScheme.onSecondary,
                indicatorColor = MaterialTheme.colorScheme.surface
            )
        )
        NavigationBarItem(
            icon = { Icon(painter = painterResource(id = R.drawable.ic_settings), contentDescription = stringResource(R.string.settings_title)) },
            label = { Text(stringResource(R.string.settings_title)) },
            selected = currentRoute == "accountPage",
            onClick = {
                if (currentRoute != "accountPage") {
                    navController.navigate("accountPage") {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onSecondary,
                selectedTextColor = MaterialTheme.colorScheme.onSecondary,
                indicatorColor = MaterialTheme.colorScheme.surface
            )
        )
    }
}
