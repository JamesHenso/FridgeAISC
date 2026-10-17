package com.production.fridgewise.ui.recipes

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.production.fridgewise.R
import com.production.fridgewise.domain.model.Recipe
import com.production.fridgewise.domain.model.RecipesUiState

private val RecipeRed = Color(0xFFFF1236)
private val RecipeMuted = Color(0xFF62656D)
private val RecipeBackground = Color(0xFFF7F8FA)

@Composable
fun RecipesRoute(token: String, fridgeId: String?, onBack: () -> Unit, onSignIn: () -> Unit) {
    val model: RecipesViewModel = viewModel(key = "recipes-${token.hashCode()}-$fridgeId",
        factory = viewModelFactory { initializer { RecipesViewModel(token, fridgeId) } })
    val state by model.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(model) { model.refresh() }
    var selectedRecipe by rememberSaveable { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    BackHandler { if (selectedRecipe != null) selectedRecipe = null else onBack() }
    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); model.clearMessage() }
    }
    AnimatedContent(targetState = selectedRecipe, transitionSpec = {
        val forward = targetState != null
        (slideInHorizontally(tween(320)) { if (forward) it else -it / 4 } + fadeIn(tween(220)))
            .togetherWith(slideOutHorizontally(tween(320)) { if (forward) -it / 4 else it } + fadeOut(tween(180)))
    }, label = "recipe-detail-navigation") { id ->
        if (id == null) RecipesScreen(state, onBack, model::search, model::refresh, model::toggleFavorite,
            onSignIn, snackbar, listState, onRecipe = { selectedRecipe = it })
        else RecipeDetailRoute(token, id, fridgeId, id in state.favorites, state.favoritesReady,
            id in state.updatingFavorites, onFavorite = { model.toggleFavorite(id) },
            onBack = { selectedRecipe = null }, onSignIn = onSignIn, snackbar = snackbar)
    }
}

@Composable
fun RecipesScreen(
    state: RecipesUiState,
    onBack: () -> Unit,
    onSearch: (String) -> Unit,
    onRefresh: () -> Unit,
    onFavorite: (String) -> Unit,
    onSignIn: () -> Unit,
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
    listState: LazyListState = rememberLazyListState(),
    onRecipe: (String) -> Unit = {}
) {
    var searching by rememberSaveable { mutableStateOf(false) }
    Scaffold(containerColor = RecipeBackground, contentColor = Color(0xFF0C0D10),
        snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), state = listState,
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)) {
            item {
                Row(Modifier.fillMaxWidth().height(52.dp), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back to inventory", Modifier.size(28.dp))
                    }
                    Row {
                        IconButton(onClick = onRefresh) { Icon(Icons.Outlined.Refresh, "Refresh recipes") }
                        IconButton(onClick = {
                            if (searching) onSearch("")
                            searching = !searching
                        }) { Icon(painterResource(R.drawable.ic_search), "Search recipes", Modifier.size(27.dp)) }
                    }
                }
            }
            item {
                Column(Modifier.padding(start = 5.dp, end = 5.dp, bottom = 8.dp)) {
                    Text("Let's cook it!", fontSize = 30.sp, lineHeight = 40.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Pick the best recipe", fontSize = 14.sp, color = RecipeMuted)
                    AnimatedVisibility(searching || state.query.isNotEmpty(), enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                        OutlinedTextField(state.query, onSearch, Modifier.fillMaxWidth().padding(top = 14.dp),
                            placeholder = { Text("Search recipes") }, singleLine = true,
                            shape = RoundedCornerShape(18.dp),
                            leadingIcon = { Icon(painterResource(R.drawable.ic_search), null, Modifier.size(22.dp)) },
                            trailingIcon = { IconButton(onClick = { onSearch(""); searching = false }) {
                                Icon(Icons.Outlined.Close, "Clear search")
                            } })
                    }
                }
            }
            when {
                state.needsSignIn -> item {
                    RecipeNotice("Phiên đăng nhập đã hết hạn.", "Đăng nhập lại", onSignIn)
                }
                state.isLoading -> item {
                    Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = RecipeRed)
                    }
                }
                state.error != null -> item { RecipeNotice(state.error, "Thử lại", onRefresh) }
                state.recipes.isEmpty() -> item {
                    RecipeNotice(if (state.query.isBlank()) "Chưa có công thức phù hợp với thực phẩm trong tủ." else "Không tìm thấy công thức phù hợp.",
                        "Tìm công thức", { searching = true })
                }
                else -> items(state.recipes, key = { it.id }) { recipe ->
                    RecipeCard(recipe, recipe.id in state.favorites,
                        recipe.id in state.updatingFavorites, state.favoritesReady,
                        onFavorite = { onFavorite(recipe.id) }, onOpen = { onRecipe(recipe.id) }, modifier = Modifier.animateItem())
                }
            }
        }
    }
}

@Composable
private fun RecipeNotice(message: String, action: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 52.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(message, color = RecipeMuted, textAlign = TextAlign.Center)
        TextButton(onClick = onClick) { Text(action, color = RecipeRed) }
    }
}

@Composable
private fun RecipeCard(recipe: Recipe, favorite: Boolean, updating: Boolean, favoritesReady: Boolean,
    onFavorite: () -> Unit, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val heartScale by animateFloatAsState(if (favorite) 1.12f else 1f, spring(dampingRatio = 0.45f), label = "favorite-scale")
    val heartColor by animateColorAsState(if (favorite) RecipeRed else Color.White, label = "favorite-color")
    Surface(onClick = onOpen, modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Color.White) {
        Row(Modifier.padding(7.dp).height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(0.41f).fillMaxHeight().heightIn(min = 136.dp)
                .clip(RoundedCornerShape(12.dp)).background(Color(0xFFE9EDEF))) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(recipe.imageUrl).crossfade(true).build(),
                    contentDescription = recipe.name,
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(R.drawable.auth_grocery_illustration),
                    error = painterResource(R.drawable.auth_grocery_illustration),
                    fallback = painterResource(R.drawable.auth_grocery_illustration),
                    modifier = Modifier.matchParentSize()
                )
                IconToggleButton(checked = favorite, onCheckedChange = { onFavorite() },
                    enabled = favoritesReady && !updating,
                    modifier = Modifier.padding(7.dp).size(40.dp).clip(CircleShape).background(Color(0x99514B43))) {
                    if (updating) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                    else Icon(painterResource(R.drawable.ic_favorite), if (favorite) "Remove favorite" else "Add favorite",
                        Modifier.size(26.dp).graphicsLayer { scaleX = heartScale; scaleY = heartScale }, tint = heartColor)
                }
            }
            Column(Modifier.weight(0.59f).padding(top = 4.dp, bottom = 7.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(recipe.name, fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    RecipeMetric(R.drawable.ic_servings, "Serving:", recipe.servings?.let { "$it người" } ?: "—", Modifier.weight(1f))
                    RecipeMetric(R.drawable.ic_time, "Time:", recipe.minutes?.let { "$it phút" } ?: "—", Modifier.weight(1f))
                    RecipeMetric(R.drawable.ic_difficulty, "Difficulty:", recipe.difficulty?.takeIf { it.isNotBlank() } ?: "—", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun RecipeMetric(@DrawableRes icon: Int, label: String, value: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(painterResource(icon), null, Modifier.size(21.dp), tint = RecipeRed)
        Text(label, color = RecipeMuted, fontSize = 10.sp, lineHeight = 13.sp, textAlign = TextAlign.Center)
        Text(value, fontSize = 12.sp, lineHeight = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Preview(showBackground = true, widthDp = 420, heightDp = 840)
@Composable
private fun RecipesPreview() {
    MaterialTheme {
        RecipesScreen(RecipesUiState(isLoading = false, favoritesReady = true, recipes = listOf(
            Recipe("1", "Beef tacos with green salsa", servings = 4, minutes = 20, difficulty = "Dễ"),
            Recipe("2", "Beef steak with red wine sauce and veggies", servings = 2, minutes = 35, difficulty = "Vừa"),
            Recipe("3", "Creamy mushroom pasta", servings = 2, minutes = 25, difficulty = "Dễ"),
            Recipe("4", "Grilled chicken salad", servings = 2, minutes = 20, difficulty = "Dễ")
        )), {}, {}, {}, {}, {})
    }
}
