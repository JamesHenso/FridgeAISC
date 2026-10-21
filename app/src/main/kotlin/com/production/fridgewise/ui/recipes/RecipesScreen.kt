package com.production.fridgewise.ui.recipes

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.production.fridgewise.R
import com.production.fridgewise.domain.model.Recipe
import com.production.fridgewise.domain.model.RecipesUiState
import com.production.fridgewise.ui.components.RefreshableContent
import com.production.fridgewise.ui.theme.FridgeWiseTheme

@Composable
fun RecipesRoute(token: String, fridgeId: String?, onBack: () -> Unit, onSignIn: () -> Unit,
    focusIngredientId: String? = null, focusIngredientName: String? = null) {
    val model: RecipesViewModel = viewModel(key = "recipes-${token.hashCode()}-$fridgeId-$focusIngredientId",
        factory = viewModelFactory { initializer { RecipesViewModel(token, fridgeId, focusIngredientId) } })
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
            onSignIn, snackbar, listState, onRecipe = { selectedRecipe = it }, onMethod = model::selectMethod,
            focusIngredientName = focusIngredientName)
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
    onRecipe: (String) -> Unit = {},
    onMethod: (String?) -> Unit = {},
    focusIngredientName: String? = null
) {
    val colors = MaterialTheme.colorScheme
    var searching by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        containerColor = colors.background,
        contentColor = colors.onBackground,
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        RefreshableContent(state.isLoading, onRefresh, Modifier.padding(padding)) { pullRefreshing ->
            LazyColumn(
                Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back to inventory")
                        }
                        if (focusIngredientName == null) {
                            IconButton(onClick = {
                                if (searching) onSearch("")
                                searching = !searching
                            }) {
                                Icon(Icons.Outlined.Search, "Search recipes")
                            }
                        }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            if (focusIngredientName == null) "Let's cook it!" else "Món với $focusIngredientName",
                            style = MaterialTheme.typography.headlineMedium,
                            color = colors.onSurface
                        )
                        Text(
                            "Pick the best recipe",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
                item {
                    AnimatedVisibility(
                        visible = searching || state.query.isNotEmpty(),
                        enter = expandVertically(animationSpec = tween(220)) + fadeIn(tween(180)),
                        exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(tween(120))
                    ) {
                        OutlinedTextField(
                            state.query, onSearch,
                            Modifier.fillMaxWidth(),
                            placeholder = { Text("Search recipes") },
                            singleLine = true,
                            shape = MaterialTheme.shapes.large,
                            leadingIcon = { Icon(Icons.Outlined.Search, null, Modifier.size(22.dp)) },
                            trailingIcon = {
                                IconButton(onClick = { onSearch(""); searching = false }) {
                                    Icon(Icons.Outlined.Close, "Clear search")
                                }
                            }
                        )
                    }
                }
                if (state.cookingMethods.isNotEmpty()) item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = state.selectedMethod == null,
                                onClick = { onMethod(null) },
                                label = { Text("Tất cả") }
                            )
                        }
                        items(state.cookingMethods) { method ->
                            FilterChip(
                                selected = state.selectedMethod == method,
                                onClick = { onMethod(method) },
                                label = { Text(method) }
                            )
                        }
                    }
                }
                when {
                    state.needsSignIn -> item {
                        RecipeNotice("Phiên đăng nhập đã hết hạn.", "Đăng nhập lại", onSignIn)
                    }
                    state.isLoading && !pullRefreshing -> item {
                        Box(Modifier.fillMaxWidth().heightIn(min = 240.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = colors.primary)
                        }
                    }
                    state.error != null -> item { RecipeNotice(state.error, "Thử lại", onRefresh) }
                    state.recipes.isEmpty() -> item {
                        RecipeNotice(
                            if (state.query.isBlank()) "Chưa có công thức phù hợp với thực phẩm trong tủ." else "Không tìm thấy công thức phù hợp.",
                            "Tìm công thức", { searching = true }
                        )
                    }
                    else -> items(state.recipes, key = { it.id }) { recipe ->
                        RecipeCard(
                            recipe, recipe.id in state.favorites,
                            recipe.id in state.updatingFavorites, state.favoritesReady,
                            onFavorite = { onFavorite(recipe.id) },
                            onOpen = { onRecipe(recipe.id) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipeNotice(message: String, action: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = colors.surfaceContainerLowest,
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Column(
            Modifier.padding(horizontal = 24.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(colors.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.RestaurantMenu, null, tint = colors.onPrimaryContainer)
            }
            Text(
                message,
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            TextButton(onClick = onClick) { Text(action, color = colors.primary) }
        }
    }
}

@Composable
internal fun RecipeCard(recipe: Recipe, favorite: Boolean, updating: Boolean, favoritesReady: Boolean,
    onFavorite: () -> Unit, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val heartScale by animateFloatAsState(if (favorite) 1.12f else 1f, spring(dampingRatio = 0.45f), label = "favorite-scale")
    val heartColor by animateColorAsState(if (favorite) colors.error else colors.onSurface, label = "favorite-color")
    Surface(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = colors.surfaceContainerLowest,
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Row(
            Modifier.padding(8.dp).height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier.weight(0.41f).fillMaxHeight().heightIn(min = 144.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(colors.surfaceContainerHigh)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(recipe.imageUrl).crossfade(true).build(),
                    contentDescription = recipe.name,
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(R.drawable.auth_grocery_illustration),
                    error = painterResource(R.drawable.auth_grocery_illustration),
                    fallback = painterResource(R.drawable.auth_grocery_illustration),
                    modifier = Modifier.matchParentSize()
                )
                IconToggleButton(
                    checked = favorite,
                    onCheckedChange = { onFavorite() },
                    enabled = favoritesReady && !updating,
                    modifier = Modifier.padding(6.dp).size(48.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceContainerLowest.copy(alpha = 0.9f))
                ) {
                    if (updating) CircularProgressIndicator(Modifier.size(20.dp), color = colors.primary, strokeWidth = 2.dp)
                    else Icon(
                        painterResource(R.drawable.ic_favorite),
                        if (favorite) "Remove favorite" else "Add favorite",
                        Modifier.size(24.dp).graphicsLayer { scaleX = heartScale; scaleY = heartScale },
                        tint = heartColor
                    )
                }
            }
            Column(
                Modifier.weight(0.59f).padding(top = 4.dp, bottom = 8.dp, end = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    recipe.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                    maxLines = 3
                )
                Spacer(Modifier.weight(1f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    RecipeMetric(R.drawable.ic_servings, "Serving", recipe.servings?.let { "$it người" } ?: "—", Modifier.weight(1f))
                    RecipeMetric(R.drawable.ic_time, "Time", recipe.minutes?.let { "$it phút" } ?: "—", Modifier.weight(1f))
                    RecipeMetric(R.drawable.ic_difficulty, "Level", recipe.difficulty?.takeIf { it.isNotBlank() } ?: "—", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun RecipeMetric(@DrawableRes icon: Int, label: String, value: String, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(painterResource(icon), null, Modifier.size(20.dp), tint = colors.primary)
        Text(
            label,
            color = colors.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center
        )
        Text(
            value,
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true, widthDp = 420, heightDp = 840)
@Composable
private fun RecipesPreview() {
    FridgeWiseTheme {
        RecipesScreen(RecipesUiState(isLoading = false, favoritesReady = true, recipes = listOf(
            Recipe("1", "Beef tacos with green salsa", servings = 4, minutes = 20, difficulty = "Dễ"),
            Recipe("2", "Beef steak with red wine sauce and veggies", servings = 2, minutes = 35, difficulty = "Vừa"),
            Recipe("3", "Creamy mushroom pasta", servings = 2, minutes = 25, difficulty = "Dễ"),
            Recipe("4", "Grilled chicken salad", servings = 2, minutes = 20, difficulty = "Dễ")
        )), {}, {}, {}, {}, {})
    }
}
