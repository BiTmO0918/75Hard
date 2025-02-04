package com.cmu.a75hard.views

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberImagePainter
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.cmu.a75hard.R
import com.cmu.a75hard.model.user.User
import com.cmu.a75hard.utils.isInternetAvailable
import com.cmu.a75hard.viewmodel.FirestoreViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingScreen(navController: NavController, firestoreViewModel: FirestoreViewModel) {
    val context = LocalContext.current
    val isConnected = remember { mutableStateOf(isInternetAvailable(context)) }
    val userList = remember { mutableStateListOf<User>() }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Obtenção das strings no início do Composable
    val noInternetMessage = stringResource(R.string.no_internet_message)
    val rankingScreenTitle = stringResource(R.string.ranking_screen_title)
    val backStr = stringResource(R.string.back)
    val positionLabel = stringResource(R.string.position_label)
    val nameLabel = stringResource(R.string.name_label)
    val pointsLabel = stringResource(R.string.points_label)

    LaunchedEffect(Unit) {
        if (isConnected.value) {
            firestoreViewModel.getAllUsersFromFirestore { users, error ->
                if (users != null) {
                    userList.clear()
                    userList.addAll(users.sortedByDescending { it.weightLost })
                    loading = false
                } else {
                    errorMessage = error
                    loading = false
                }
            }
        } else {
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = rankingScreenTitle, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = backStr,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        content = { padding ->
            if (!isConnected.value) {
                // Tela sem conexão à internet
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    Text(
                        text = noInternetMessage,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else if (loading) {
                // Indicador de carregamento
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    CircularProgressIndicator()
                }
            } else if (errorMessage != null) {
                // Mensagem de erro ao carregar dados
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    Text(
                        text = errorMessage ?: stringResource(R.string.unknown_error),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                // Exibição da tabela
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(8.dp), // Cantos arredondados
                        border = BorderStroke(1.dp, Color.LightGray), // Borda fina cinza clara
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            // Cabeçalho
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = positionLabel,
                                    modifier = Modifier.weight(1f),
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = nameLabel,
                                    modifier = Modifier.weight(1f),
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = pointsLabel,
                                    modifier = Modifier.weight(1f),
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Conteúdo da tabela
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                itemsIndexed(userList) { index, user ->
                                    val textColor = when (index) {
                                        0 -> Color(0xFFFFD700) // Dourado
                                        1 -> Color(0xFFC0C0C0) // Prateado
                                        2 -> Color(0xFFCD7F32) // Bronze
                                        else -> MaterialTheme.colorScheme.onBackground
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            modifier = Modifier.weight(1f),
                                            color = textColor,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "${user.firstName} ${user.lastName}",
                                            modifier = Modifier.weight(1f),
                                            color = textColor,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "${user.weightLost}",
                                            modifier = Modifier.weight(1f),
                                            color = textColor,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}





