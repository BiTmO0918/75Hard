package com.cmu.a75hard.views

import android.content.Context
import android.net.ConnectivityManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import coil.compose.rememberImagePainter
import com.cmu.a75hard.api.ApiService
import com.cmu.a75hard.api.Meal
import com.cmu.a75hard.api.MealDetails
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.cmu.a75hard.R
import com.cmu.a75hard.api.Category
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailsScreen(navController: NavController, mealId: String) {
    val apiService = ApiService.create()
    val recipeDetails = remember { mutableStateOf<MealDetails?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(mealId) {
        scope.launch {
            try {
                val response = apiService.getRecipeDetails(mealId)
                recipeDetails.value = response.meals.firstOrNull()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    recipeDetails.value?.let { meal ->
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        // Truncar o título se muito longo
                        Text(
                            text = meal.strMeal,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = stringResource(id = R.string.back),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            },
            content = { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Video Preview
                    VideoPreview(
                        videoId = meal.strYoutube.split("=").last(),
                        lifecycleOwner = LocalLifecycleOwner.current
                    )

                    Spacer(modifier = Modifier.height(30.dp))

                    // Ingredients Section
                    Section(title = stringResource(id = R.string.ingredients)) {
                        val ingredients = (1..20).mapNotNull { index ->
                            val ingredientField = meal.javaClass.getDeclaredField("strIngredient$index").apply { isAccessible = true }
                            val measureField = meal.javaClass.getDeclaredField("strMeasure$index").apply { isAccessible = true }
                            val ingredient = ingredientField.get(meal) as? String
                            val measure = measureField.get(meal) as? String
                            if (!ingredient.isNullOrBlank()) "$measure $ingredient" else null
                        }

                        // Mostra todos os ingredientes válidos
                        ingredients.forEach { ingredient ->
                            Text(
                                text = "\u2022 $ingredient",
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(30.dp))

                    // Preparation Section
                    Section(title = stringResource(id = R.string.preparation_method)) {
                        meal.strInstructions.split("\n").forEachIndexed { index, step ->
                            Text(
                                text = "${index + 1}. $step",
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun VideoPreview(videoId: String, lifecycleOwner: LifecycleOwner) {
    AndroidView(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(5.dp)),
        factory = {
            YouTubePlayerView(context = it).apply {
                lifecycleOwner.lifecycle.addObserver(this)

                addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                    override fun onReady(youTubePlayer: YouTubePlayer) {
                        youTubePlayer.cueVideo(videoId = videoId, 0f)
                    }
                })
            }
        }
    )
}

@Composable
fun Section(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeListScreen(
    navController: NavController,
    selectedCategory: MutableState<String>
) {
    val apiService = ApiService.create()
    val recipes = remember { mutableStateOf<List<Meal>>(emptyList()) }
    val categories = remember { mutableStateOf<List<Category>>(emptyList()) }
    val showDialog = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current // Mova isso para fora de remember
    val isConnected = remember { mutableStateOf(false) }

    // Atualizar estado de conectividade
    LaunchedEffect(context) {
        isConnected.value = checkInternetConnection(context)
    }

    if (isConnected.value) {
        // Fetch categories
        LaunchedEffect(Unit) {
            scope.launch {
                try {
                    val response = apiService.getCategories()
                    categories.value = response.categories
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Fetch recipes based on selectedCategory
        LaunchedEffect(selectedCategory.value) {
            scope.launch {
                try {
                    val response = apiService.getRecipesByCategory(selectedCategory.value)
                    recipes.value = response.meals
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.recipes),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    if (isConnected.value) {
                        IconButton(onClick = { showDialog.value = true }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_recipe_category),
                                contentDescription = stringResource(id = R.string.select_categories),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(id = R.string.back),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        content = { padding ->
            if (isConnected.value) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(recipes.value) { recipe ->
                        RecipeCard(navController, recipe)
                    }
                }
            } else {
                // Exibe mensagem de erro de conexão
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    Text(
                        text = stringResource(id = R.string.no_internet_message),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    )

    if (isConnected.value && showDialog.value) {
        CategoryDialog(
            categories = categories.value,
            selectedCategory = selectedCategory.value,
            onCategorySelected = { category ->
                selectedCategory.value = category
                showDialog.value = false
            },
            onDismiss = { showDialog.value = false }
        )
    }
}

@Composable
fun CategoryDialog(
    categories: List<Category>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)) // Fundo translúcido para o diálogo
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .heightIn(max = 400.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.select_categories),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Lista de categorias
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        categories.forEach { category ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onCategorySelected(category.strCategory) },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedCategory == category.strCategory,
                                    onClick = { onCategorySelected(category.strCategory) }
                                )
                                Text(
                                    text = category.strCategory,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(id = R.string.cancel), color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun RecipeCard(navController: NavController, meal: Meal) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiary, // Fundo do card
            contentColor = MaterialTheme.colorScheme.primary // Cor do texto
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp) // Altura fixa do card
            .clickable { navController.navigate("RecipeDetailsScreen/${meal.idMeal}") }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp) // Espaçamento interno
        ) {
            // Imagem da receita
            Image(
                painter = rememberImagePainter(data = meal.strMealThumb),
                contentDescription = stringResource(id = R.string.image_unavailable),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(12.dp)) // Bordas arredondadas na imagem
            )

            // Título da receita, centralizado verticalmente
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 12.dp), // Espaçamento entre imagem e texto
                contentAlignment = Alignment.CenterStart // Centraliza verticalmente
            ) {
                Text(
                    text = meal.strMeal,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface, // Cor do texto contrastante com o fundo
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis // "..." caso o texto ultrapasse 2 linhas
                )
            }
        }
    }
}



fun checkInternetConnection(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkInfo = connectivityManager.activeNetworkInfo
    return networkInfo != null && networkInfo.isConnected
}

