package com.production.fridgewise.ui.recipes

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.production.fridgewise.R
import com.production.fridgewise.ui.components.RefreshableContent
import com.production.fridgewise.ui.inventory.InventoryBottomBar

@Composable
fun FavoriteRoute(token: String, fridgeId: String?, onFeature: (String) -> Unit, onSignIn: () -> Unit) {
    val model: FavoriteViewModel = viewModel(key = "favorite-${token.hashCode()}",
        factory = viewModelFactory { initializer { FavoriteViewModel(token) } })
    val state by model.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var selectedRecipe by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(model) { model.refresh() }
    BackHandler { if (selectedRecipe != null) selectedRecipe = null else onFeature("Inventory") }
    AnimatedContent(targetState = selectedRecipe, transitionSpec = {
        val forward = targetState != null
        (slideInHorizontally(tween(320)) { if (forward) it else -it / 4 } + fadeIn(tween(220)))
            .togetherWith(slideOutHorizontally(tween(320)) { if (forward) -it / 4 else it } + fadeOut(tween(180)))
    }, label = "favorite-detail") { id ->
        if (id == null) FavoriteScreen(state, model::refresh, model::remove, onFeature, onSignIn,
            onRecipe = { selectedRecipe = it }, snackbar = snackbar)
        else RecipeDetailRoute(token, id, fridgeId, favorite = state.recipes.any { it.id == id },
            favoriteReady = !state.loading, favoriteUpdating = id in state.removing,
            onFavorite = { model.remove(id) }, onBack = { selectedRecipe = null },
            onSignIn = onSignIn, snackbar = snackbar)
    }
}

@Composable
private fun FavoriteScreen(
    state: FavoriteUiState,
    onRefresh: () -> Unit,
    onRemove: (String) -> Unit,
    onFeature: (String) -> Unit,
    onSignIn: () -> Unit,
    onRecipe: (String) -> Unit,
    snackbar: SnackbarHostState
) {
    val colors = MaterialTheme.colorScheme
    Scaffold(
        containerColor = colors.background,
        contentColor = colors.onBackground,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = { InventoryBottomBar(onFeature, "Favorite") }
    ) { padding ->
        RefreshableContent(state.loading, onRefresh, Modifier.padding(padding)) { pullRefreshing ->
            BoxWithConstraints(Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = if (maxWidth > 600.dp) 40.dp else 20.dp,
                        end = if (maxWidth > 600.dp) 40.dp else 20.dp,
                        top = 8.dp,
                        bottom = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 56.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Favorite",
                                Modifier.weight(1f),
                                style = MaterialTheme.typography.headlineMedium,
                                color = colors.onSurface
                            )
                            IconButton(onClick = { onFeature("Settings") }) {
                                Icon(Icons.Outlined.Settings, "Cài đặt")
                            }
                        }
                    }
                    item {
                        Text(
                            "Công thức bạn muốn nấu lại",
                            color = colors.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.extraLarge,
                            color = colors.tertiaryContainer
                        ) {
                            Row(
                                Modifier.padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    Modifier.size(56.dp).clip(CircleShape).background(colors.tertiary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painterResource(R.drawable.ic_favorite), null,
                                        Modifier.size(28.dp), tint = colors.onTertiary
                                    )
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        "Món ăn đã lưu",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.onTertiaryContainer.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        "${state.recipes.size} công thức",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = colors.onTertiaryContainer
                                    )
                                }
                            }
                        }
                    }
                    when {
                        state.loading && !pullRefreshing && state.recipes.isEmpty() -> item {
                            Box(Modifier.fillMaxWidth().heightIn(min = 240.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = colors.primary)
                            }
                        }
                        state.needsSignIn -> item { FavoriteNotice("Phiên đăng nhập đã hết hạn.", "Đăng nhập lại", onSignIn) }
                        state.error != null && state.recipes.isEmpty() -> item {
                            FavoriteNotice(state.error, "Thử lại", onRefresh)
                        }
                        state.recipes.isEmpty() -> item {
                            AnimatedVisibility(true, enter = fadeIn(tween(420)) + scaleIn(initialScale = 0.94f)) {
                                FavoriteNotice(
                                    "Chưa có món nào được lưu. Chạm trái tim ở Recipes để giữ món bạn thích.",
                                    "Khám phá Recipes", { onFeature("Recipes") }
                                )
                            }
                        }
                        else -> {
                            item {
                                Text(
                                    "Danh sách yêu thích",
                                    Modifier.padding(top = 8.dp),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = colors.onSurface
                                )
                            }
                            items(state.recipes, key = { it.id }) { recipe ->
                                RecipeCard(
                                    recipe, favorite = true, updating = recipe.id in state.removing,
                                    favoritesReady = true, onFavorite = { onRemove(recipe.id) },
                                    onOpen = { onRecipe(recipe.id) }, modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }
                    if (state.error != null && state.recipes.isNotEmpty()) item {
                        Text(state.error, color = colors.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteNotice(message: String, action: String, onClick: () -> Unit) {
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
                Modifier.size(52.dp).clip(CircleShape).background(colors.tertiaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(R.drawable.ic_favorite), null, Modifier.size(26.dp), tint = colors.onTertiaryContainer)
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
