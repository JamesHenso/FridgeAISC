package com.production.fridgewise.ui.inventory

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil3.compose.AsyncImage
import com.production.fridgewise.R
import com.production.fridgewise.domain.model.Food
import com.production.fridgewise.domain.model.FoodUrgency
import com.production.fridgewise.domain.model.InventoryFilter
import com.production.fridgewise.domain.model.InventoryStats
import com.production.fridgewise.domain.model.InventoryUiState
import com.production.fridgewise.domain.model.inventoryToday
import com.production.fridgewise.ui.components.RefreshableContent
import com.production.fridgewise.ui.recipes.FavoriteRoute
import com.production.fridgewise.ui.recipes.RecipesRoute
import com.production.fridgewise.ui.scan.ScanRoute
import com.production.fridgewise.ui.theme.FridgeWiseTheme
import kotlinx.coroutines.launch

@Composable
fun InventoryRoute(accessToken: String, onSignIn: () -> Unit, userId: String? = null) {
    val model: InventoryViewModel = viewModel(key = "inventory-${accessToken.hashCode()}", factory = viewModelFactory {
        initializer { InventoryViewModel(accessToken) }
    })
    val state by model.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(userId, state.isLoading, state.error) {
        if (!userId.isNullOrBlank()) {
            com.production.fridgewise.notifications.ExpiryNotifications.account(context, userId)
            if (!state.isLoading && state.error == null && state.query.isBlank() && state.location == null) {
                try {
                    com.production.fridgewise.notifications.ExpiryNotifications.sync(context, userId, accessToken)
                } catch (e: kotlinx.coroutines.CancellationException) { throw e
                } catch (_: Exception) { /* Keep last successful snapshot; settings shows its date. */ }
            }
        }
    }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var page by rememberSaveable { mutableStateOf("inventory") }
    var settingsReturnPage by rememberSaveable { mutableStateOf("inventory") }
    var recipesFridgeId by rememberSaveable { mutableStateOf<String?>(null) }
    var recipesFocusId by rememberSaveable { mutableStateOf<String?>(null) }
    var recipesFocusName by rememberSaveable { mutableStateOf<String?>(null) }
    var scanFridgeId by rememberSaveable { mutableStateOf<String?>(null) }
    val inventoryScroll = rememberLazyListState()
    AnimatedContent(
        targetState = page,
        transitionSpec = {
            val forward = targetState != "inventory"
            (slideInHorizontally(tween(360)) { if (forward) it else -it / 4 } + fadeIn(tween(280)))
                .togetherWith(slideOutHorizontally(tween(360)) { if (forward) -it / 4 else it } + fadeOut(tween(220)))
        }, label = "inventory-recipes-transition"
    ) { destination ->
        if (destination == "scan") {
            ScanRoute(accessToken, scanFridgeId, onClose = { page = "inventory" }, onComplete = {
                page = "inventory"
                model.refresh()
                scope.launch { snackbar.showSnackbar("Đã thêm thực phẩm vào Inventory") }
            })
        } else if (destination == "recipes") {
            RecipesRoute(accessToken, recipesFridgeId, onBack = { page = "inventory" }, onSignIn = onSignIn,
                focusIngredientId = recipesFocusId, focusIngredientName = recipesFocusName)
        } else if (destination == "favorite") {
            FavoriteRoute(accessToken, state.selectedFridgeId, onSignIn = onSignIn, onFeature = { feature ->
                when (feature) {
                    "Inventory" -> page = "inventory"
                    "Recipes" -> { recipesFridgeId = state.selectedFridgeId; recipesFocusId = null; recipesFocusName = null; page = "recipes" }
                    "Report" -> page = "report"
                    "Scan" -> { scanFridgeId = state.selectedFridgeId; page = "scan" }
                    "Settings" -> { settingsReturnPage = "favorite"; page = "settings" }
                }
            })
        } else if (destination == "settings") {
            ReminderSettings(userId.orEmpty(), accessToken, onDismiss = { page = settingsReturnPage })
        } else if (destination == "report") {
            WeeklyReportScreen(
                state = state,
                onRefresh = model::refresh,
                onSignIn = onSignIn,
                onFeature = { feature ->
                    when (feature) {
                        "Inventory" -> page = "inventory"
                        "Recipes" -> {
                            recipesFridgeId = state.selectedFridgeId
                            recipesFocusId = null
                            recipesFocusName = null
                            page = "recipes"
                        }
                        "Scan" -> { scanFridgeId = state.selectedFridgeId; page = "scan" }
                        "Favorite" -> page = "favorite"
                        "Settings" -> { settingsReturnPage = "report"; page = "settings" }
                        else -> scope.launch { snackbar.showSnackbar("$feature — coming soon") }
                    }
                }
            )
        } else {
            InventoryScreen(state, model::search, model::selectFridge,
                model::refresh, onSignIn, snackbar, inventoryScroll,
                onFeature = { feature ->
                    if (feature == "Recipes") {
                        recipesFridgeId = state.selectedFridgeId
                        recipesFocusId = null
                        recipesFocusName = null
                        page = "recipes"
                    } else if (feature == "Scan") {
                        scanFridgeId = state.selectedFridgeId
                        page = "scan"
                    } else if (feature == "Report") {
                        model.search("")
                        model.selectLocation(null)
                        page = "report"
                    } else if (feature == "Settings") {
                        settingsReturnPage = "inventory"; page = "settings"
                    } else if (feature == "Favorite") {
                        page = "favorite"
                    } else if (feature == "Inventory") {
                        page = "inventory"
                    } else scope.launch { snackbar.showSnackbar("$feature — coming soon") }
                },
                onFilter = model::selectFilter,
                onDeleteFood = { food ->
                    model.remove(food) { message -> scope.launch { snackbar.showSnackbar(message) } }
                },
                onUseFood = { food ->
                    if (food.ingredientId.isNullOrBlank()) {
                        scope.launch { snackbar.showSnackbar("Món này chưa được ghép với nguyên liệu trong danh mục.") }
                    } else {
                        recipesFridgeId = state.selectedFridgeId
                        recipesFocusId = food.ingredientId
                        recipesFocusName = food.name
                        page = "recipes"
                    }
                })
        }
    }
}

@Composable
fun InventoryScreen(
    state: InventoryUiState,
    onSearch: (String) -> Unit,
    onFridge: (String) -> Unit,
    onRefresh: () -> Unit,
    onSignIn: () -> Unit,
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
    listState: LazyListState = rememberLazyListState(),
    onFeature: (String) -> Unit,
    onUseFood: (Food) -> Unit = {},
    onFilter: (InventoryFilter) -> Unit = {},
    onDeleteFood: (Food) -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    val today = inventoryToday()
    val visibleFoods = state.foods.filter { state.filter.matches(it, today) }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var showFridges by remember { mutableStateOf(false) }
    Scaffold(
        containerColor = colors.background,
        contentColor = colors.onBackground,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = { InventoryBottomBar(onFeature, selectedTab = "Inventory") }
    ) { padding ->
        RefreshableContent(state.isLoading, onRefresh, Modifier.padding(padding)) { pullRefreshing ->
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    InventoryHeader(
                        searching = showSearch,
                        onSearchToggle = {
                            if (showSearch) onSearch("")
                            showSearch = !showSearch
                        },
                        onSettings = { onFeature("Settings") }
                    )
                }
                item {
                    AnimatedVisibility(
                        visible = showSearch,
                        enter = expandVertically(animationSpec = tween(220)) + fadeIn(tween(180)),
                        exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(tween(120))
                    ) {
                        OutlinedTextField(
                            value = state.query,
                            onValueChange = onSearch,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = {
                                Text("Search your items", style = MaterialTheme.typography.bodyMedium)
                            },
                            leadingIcon = { Icon(Icons.Outlined.Search, null, tint = colors.onSurfaceVariant) },
                            trailingIcon = {
                                IconButton(onClick = { onSearch(""); showSearch = false }) {
                                    Icon(Icons.Outlined.Close, "Clear search")
                                }
                            },
                            shape = MaterialTheme.shapes.large
                        )
                    }
                }
                if (state.fridges.size > 1) item {
                    Box {
                        Surface(
                            onClick = { showFridges = true },
                            shape = CircleShape,
                            color = colors.secondaryContainer,
                            contentColor = colors.onSecondaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 16.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    state.fridges.firstOrNull { it.id == state.selectedFridgeId }?.name.orEmpty(),
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Icon(Icons.Outlined.ExpandMore, null, Modifier.size(18.dp))
                            }
                        }
                        DropdownMenu(showFridges, onDismissRequest = { showFridges = false }) {
                            state.fridges.forEach { fridge ->
                                DropdownMenuItem(
                                    text = { Text(fridge.name) },
                                    onClick = {
                                        showFridges = false
                                        onFridge(fridge.id)
                                    }
                                )
                            }
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard(state.stats.inStock, "In stock", R.drawable.in_stock, Modifier.weight(1f))
                        StatCard(state.stats.useToday, "Use today", R.drawable.use_today, Modifier.weight(1f))
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard(state.stats.useSoon, "Use soon", R.drawable.use_soon, Modifier.weight(1f))
                        StatCard(state.stats.cleanOut, "Clean out", R.drawable.clean_out, Modifier.weight(1f))
                    }
                }
                item {
                    InventoryFilterBar(
                        selected = state.filter,
                        onSelect = onFilter,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
                when {
                    state.isLoading && !pullRefreshing -> item {
                        Box(Modifier.fillMaxWidth().heightIn(min = 160.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = colors.primary)
                        }
                    }
                    state.error != null -> item {
                        InventoryNotice(
                            title = if (state.needsSignIn) "Your session has expired" else "Couldn't load your inventory",
                            message = if (state.needsSignIn) "Please sign in again to keep your fridge up to date." else state.error,
                            action = if (state.needsSignIn) "Sign in" else "Try again",
                            onAction = if (state.needsSignIn) onSignIn else onRefresh,
                            error = true
                        )
                    }
                    state.foods.isEmpty() -> item {
                        InventoryNotice(
                            title = "Add your items",
                            message = "Scan a receipt or add food manually to start tracking what is in your fridge.",
                            action = if (state.query.isNotBlank()) "Clear search" else null,
                            onAction = { onSearch("") },
                            illustration = true
                        )
                    }
                    visibleFoods.isEmpty() -> item {
                        InventoryNotice(
                            title = "Nothing in this group",
                            message = "Không có thực phẩm trong nhóm này.",
                            action = null,
                            onAction = {}
                        )
                    }
                    else -> items(visibleFoods, key = { it.id }) { food ->
                        val dismissState = rememberSwipeToDismissBoxState(confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) onDeleteFood(food)
                            value == SwipeToDismissBoxValue.EndToStart
                        })
                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            backgroundContent = {
                                Box(
                                    Modifier.fillMaxSize()
                                        .clip(MaterialTheme.shapes.large)
                                        .background(colors.errorContainer),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        "Xoá ${food.name}",
                                        tint = colors.onErrorContainer,
                                        modifier = Modifier.padding(end = 24.dp).size(24.dp)
                                    )
                                }
                            },
                            modifier = Modifier.animateItem()
                        ) {
                            FoodRow(food, onUse = { onUseFood(food) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryHeader(searching: Boolean, onSearchToggle: () -> Unit, onSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onSearchToggle) {
            Icon(
                if (searching) Icons.Outlined.Close else Icons.Outlined.Search,
                if (searching) "Close search" else "Search inventory"
            )
        }
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Fridge", color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Text("Wise", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Outlined.Settings, "Settings")
        }
    }
}

@Composable
private fun InventoryFilterBar(
    selected: InventoryFilter,
    onSelect: (InventoryFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    BoxWithConstraints(
        modifier.clip(CircleShape).background(colors.surfaceContainerHigh).padding(4.dp)
    ) {
        val tabWidth = maxWidth / InventoryFilter.entries.size
        val indicatorOffset by animateDpAsState(
            targetValue = tabWidth * selected.ordinal,
            animationSpec = tween(280, easing = FastOutSlowInEasing),
            label = "inventory-filter-position"
        )
        Box(
            Modifier.offset(x = indicatorOffset).width(tabWidth).height(48.dp)
                .clip(CircleShape).background(colors.primary)
        )
        Row(Modifier.fillMaxWidth().selectableGroup()) {
            InventoryFilter.entries.forEach { filter ->
                val isSelected = selected == filter
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) colors.onPrimary else colors.onSurfaceVariant,
                    animationSpec = tween(200),
                    label = "inventory-filter-text"
                )
                Box(
                    Modifier.weight(1f).height(48.dp).clip(CircleShape)
                        .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(filter) }),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        filter.label,
                        color = textColor,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(count: Int, label: String, image: Int, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.height(104.dp),
        shape = MaterialTheme.shapes.large,
        color = colors.surfaceContainerLowest,
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Box {
            Image(
                painterResource(image), null,
                Modifier.align(Alignment.BottomEnd)
                    .offset(x = 18.dp, y = 18.dp)
                    .requiredSize(98.dp),
                contentScale = ContentScale.Fit,
                alpha = 0.9f
            )
            Column(
                Modifier.padding(start = 16.dp, top = 14.dp, end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    count.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.onSurface
                )
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FoodRow(food: Food, onUse: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val status = food.status()
    val statusColor = when (status.urgency) {
        FoodUrgency.EXPIRED -> colors.error
        FoodUrgency.SOON, FoodUrgency.TODAY -> colors.tertiary
        else -> colors.onSurfaceVariant
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = colors.surfaceContainerLowest,
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier.size(48.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(colors.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    food.imageUrl, food.name,
                    Modifier.fillMaxSize().padding(4.dp),
                    error = painterResource(R.drawable.ic_nguyenlieu),
                    fallback = painterResource(R.drawable.ic_nguyenlieu)
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    food.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    status.text,
                    color = statusColor,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (status.urgency == FoodUrgency.NORMAL) FontWeight.Normal else FontWeight.SemiBold
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(food.quantityLabel, style = MaterialTheme.typography.labelLarge, color = colors.onSurface)
                TextButton(
                    onClick = onUse,
                    enabled = status.urgency != FoodUrgency.EXPIRED,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(
                        if (status.urgency == FoodUrgency.EXPIRED) "Đã quá hạn" else "Dùng món này",
                        color = if (status.urgency == FoodUrgency.EXPIRED) colors.onSurfaceVariant else colors.primary,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun InventoryNotice(
    title: String,
    message: String?,
    action: String?,
    onAction: () -> Unit,
    error: Boolean = false,
    illustration: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = MaterialTheme.shapes.extraLarge,
        color = colors.surfaceContainerLowest,
        border = BorderStroke(1.dp, if (error) colors.errorContainer else colors.outlineVariant)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (illustration) {
                Image(
                    painterResource(R.drawable.auth_grocery_illustration),
                    null,
                    Modifier.size(160.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Box(
                    Modifier.size(48.dp).clip(CircleShape)
                        .background(if (error) colors.errorContainer else colors.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (error) Icons.Outlined.WarningAmber else Icons.Outlined.Search,
                        null,
                        tint = if (error) colors.onErrorContainer else colors.onPrimaryContainer
                    )
                }
            }
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
                textAlign = TextAlign.Center
            )
            if (message != null) {
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (error) colors.error else colors.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            if (action != null) {
                if (error) {
                    Button(
                        onClick = onAction,
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                    ) { Text(action) }
                } else {
                    TextButton(onClick = onAction) { Text(action, color = colors.primary) }
                }
            }
        }
    }
}

@Composable
internal fun InventoryBottomBar(onFeature: (String) -> Unit, selectedTab: String) {
    val colors = MaterialTheme.colorScheme
    NavigationBar(
        containerColor = colors.surfaceContainer,
        contentColor = colors.onSurfaceVariant,
        tonalElevation = 3.dp
    ) {
        BottomItem("Recipes", R.drawable.ic_recipe, selectedTab == "Recipes") { onFeature("Recipes") }
        BottomItem("Favorite", R.drawable.ic_favorite, selectedTab == "Favorite") { onFeature("Favorite") }
        BottomItem("Scan", R.drawable.ic_scan, selected = false, raised = true) { onFeature("Scan") }
        BottomItem("Inventory", R.drawable.ic_inventory, selectedTab == "Inventory") { onFeature("Inventory") }
        BottomItem("Report", R.drawable.ic_report, selectedTab == "Report") { onFeature("Report") }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.BottomItem(
    label: String,
    @DrawableRes icon: Int,
    selected: Boolean,
    raised: Boolean = false,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = {
            if (raised) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(colors.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painterResource(icon), label,
                        Modifier.size(24.dp),
                        tint = colors.onPrimary
                    )
                }
            } else {
                Icon(painterResource(icon), label, Modifier.size(24.dp))
            }
        },
        label = {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = colors.onSecondaryContainer,
            selectedTextColor = colors.onSurface,
            indicatorColor = colors.secondaryContainer,
            unselectedIconColor = colors.onSurfaceVariant,
            unselectedTextColor = colors.onSurfaceVariant
        )
    )
}

@Preview(showBackground = true, widthDp = 400, heightDp = 840)
@Composable
private fun InventoryPreview() {
    FridgeWiseTheme {
        InventoryScreen(InventoryUiState(
            isLoading = false,
            stats = InventoryStats(5, 0, 1, 1),
            foods = listOf(
                Food("1", "Burgers", 2.0, "pcs", "fridge", expiryDate = "2026-10-13"),
                Food("2", "Beef rib-eye steak", 300.0, "g", "fridge", expiryDate = "2026-10-17"),
                Food("3", "Duck frozen", 2.0, "kg", "freezer", purchasedAt = "2026-10-04"),
                Food("4", "Tomatoes", 0.5, "kg", "fridge", purchasedAt = "2026-10-12"),
                Food("5", "Gouda cheese", 150.0, "g", "fridge", purchasedAt = "2026-10-13")
            )
        ), {}, {}, {}, {}, onFeature = {})
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 840)
@Composable
private fun EmptyInventoryPreview() {
    FridgeWiseTheme {
        InventoryScreen(InventoryUiState(isLoading = false), {}, {}, {}, {}, onFeature = {})
    }
}
