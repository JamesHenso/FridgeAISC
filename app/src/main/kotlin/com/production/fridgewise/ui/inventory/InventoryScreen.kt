package com.production.fridgewise.ui.inventory

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import com.production.fridgewise.ui.recipes.RecipesRoute
import com.production.fridgewise.ui.scan.ScanRoute
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.annotation.DrawableRes
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.production.fridgewise.R
import com.production.fridgewise.domain.model.*
import kotlinx.coroutines.launch

private val InventoryGreen = Color(0xFF05C487)
private val InventoryBackground = Color(0xFFF0F7FD)
private val InventoryMuted = Color(0xFF80828B)
private val InventoryInk = Color(0xFF101319)

@Composable
fun InventoryRoute(accessToken: String, onSignIn: () -> Unit) {
    val model: InventoryViewModel = viewModel(key = "inventory-${accessToken.hashCode()}", factory = viewModelFactory {
        initializer { InventoryViewModel(accessToken) }
    })
    val state by model.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var page by rememberSaveable { mutableStateOf("inventory") }
    var recipesFridgeId by rememberSaveable { mutableStateOf<String?>(null) }
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
            RecipesRoute(accessToken, recipesFridgeId, onBack = { page = "inventory" }, onSignIn = onSignIn)
        } else {
            InventoryScreen(state, model::selectLocation, model::search, model::selectFridge,
                model::refresh, onSignIn, snackbar, inventoryScroll) { feature ->
                if (feature == "Recipes") {
                    recipesFridgeId = state.selectedFridgeId
                    page = "recipes"
                } else if (feature == "Scan") {
                    scanFridgeId = state.selectedFridgeId
                    page = "scan"
                } else scope.launch { snackbar.showSnackbar("$feature — coming soon") }
            }
        }
    }
}

@Composable
fun InventoryScreen(
    state: InventoryUiState,
    onLocation: (String?) -> Unit,
    onSearch: (String) -> Unit,
    onFridge: (String) -> Unit,
    onRefresh: () -> Unit,
    onSignIn: () -> Unit,
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
    listState: LazyListState = rememberLazyListState(),
    onFeature: (String) -> Unit
) {
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var showFridges by remember { mutableStateOf(false) }
    Scaffold(
        containerColor = InventoryBackground,
        contentColor = InventoryInk,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = { InventoryBottomBar(onFeature) }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Box(Modifier.fillMaxWidth().height(64.dp)) {
                    IconButton(onClick = {
                        if (showSearch) onSearch("")
                        showSearch = !showSearch
                    }, modifier = Modifier.align(Alignment.CenterStart)) {
                        Icon(Icons.Outlined.Search, "Search inventory", Modifier.size(28.dp))
                    }
                    Text("FridgeWise", fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
                    IconButton(onClick = { onFeature("Notifications") }, modifier = Modifier.align(Alignment.CenterEnd)) {
                        Icon(Icons.Outlined.NotificationsNone, "Notifications", Modifier.size(28.dp))
                    }
                }
            }
            if (showSearch) item {
                OutlinedTextField(state.query, onSearch, Modifier.fillMaxWidth(),
                    singleLine = true, placeholder = { Text("Search your items") },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    trailingIcon = { IconButton(onClick = { onSearch(""); showSearch = false }) {
                        Icon(Icons.Outlined.Close, "Clear search")
                    } }, shape = RoundedCornerShape(18.dp))
            }
            if (state.fridges.size > 1) item {
                Box {
                    TextButton(onClick = { showFridges = true }) {
                        Text(state.fridges.firstOrNull { it.id == state.selectedFridgeId }?.name.orEmpty(), color = InventoryGreen)
                        Icon(Icons.Outlined.ExpandMore, null, tint = InventoryGreen)
                    }
                    DropdownMenu(showFridges, onDismissRequest = { showFridges = false }) {
                        state.fridges.forEach { fridge ->
                            DropdownMenuItem(text = { Text(fridge.name) }, onClick = {
                                showFridges = false; onFridge(fridge.id)
                            })
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    StatCard(state.stats.inStock, "In stock", R.drawable.in_stock, Modifier.weight(1f))
                    StatCard(state.stats.useToday, "Use today", R.drawable.use_today, Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    StatCard(state.stats.useSoon, "Use soon", R.drawable.use_soon, Modifier.weight(1f))
                    StatCard(state.stats.cleanOut, "Clean out", R.drawable.clean_out, Modifier.weight(1f))
                }
            }
            item {
                Surface(onClick = { onFeature("Recipes") }, enabled = !state.isLoading && state.error == null,
                    shape = RoundedCornerShape(15.dp),
                    color = Color.White, modifier = Modifier.fillMaxWidth().padding(top = 5.dp)) {
                    Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Recipes", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text("Cook with what you already have", fontSize = 14.sp, color = InventoryMuted)
                        }
                        Icon(Icons.Outlined.ChevronRight, null, Modifier.size(28.dp))
                    }
                }
            }
            item {
                Column(Modifier.padding(top = 4.dp, bottom = 7.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        listOf(null to "All", "freezer" to "Freezer", "fridge" to "Fridge").forEach { (value, label) ->
                            val selected = value == state.location
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                TextButton(onClick = { onLocation(value) }, modifier = Modifier.fillMaxWidth()) {
                                    Text(label, fontSize = 16.sp, color = if (selected) InventoryGreen else InventoryMuted,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                                }
                                Box(Modifier.width(55.dp).height(3.dp).clip(CircleShape)
                                    .background(if (selected) InventoryGreen else Color.Transparent))
                            }
                        }
                        IconButton(onClick = onRefresh) { Icon(Icons.Outlined.Refresh, "Refresh inventory", tint = InventoryMuted) }
                    }
                    HorizontalDivider(color = Color(0xFFDFEAF2))
                }
            }
            when {
                state.isLoading -> item {
                    Box(Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = InventoryGreen)
                    }
                }
                state.error != null -> item {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (state.needsSignIn) "Your session has expired. Please sign in again." else state.error,
                            color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = if (state.needsSignIn) onSignIn else onRefresh) {
                            Text(if (state.needsSignIn) "Sign in" else "Try again")
                        }
                    }
                }
                state.foods.isEmpty() -> item {
                    Column(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(painterResource(R.drawable.auth_grocery_illustration), null,
                            Modifier.size(180.dp), contentScale = ContentScale.Fit)
                        Text("Add your items", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                        if (state.query.isNotBlank() || state.location != null) {
                            TextButton(onClick = { onSearch(""); onLocation(null) }) { Text("Clear filters") }
                        }
                    }
                }
                else -> items(state.foods, key = { it.id }) { FoodRow(it) }
            }
        }
    }
}

@Composable
private fun StatCard(count: Int, label: String, image: Int, modifier: Modifier) {
    Box(modifier.height(96.dp).clip(RoundedCornerShape(15.dp)).background(Color.White)) {
        Image(
            painterResource(image), null,
            Modifier.align(Alignment.BottomEnd)
                .offset(x = 20.dp, y = 20.dp)
                .wrapContentSize(Alignment.BottomEnd, unbounded = true)
                .requiredSize(105.dp),
            contentScale = ContentScale.Fit
        )
        Column(Modifier.padding(start = 18.dp, top = 10.dp)) {
            Text(count.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp)
            Text(label, fontSize = 14.sp, color = Color.Black)
        }
    }
}

@Composable
private fun FoodRow(food: Food) {
    val status = food.status()
    val statusColor = when (status.urgency) {
        FoodUrgency.EXPIRED -> Color(0xFFFF1655)
        FoodUrgency.SOON, FoodUrgency.TODAY -> Color(0xFFFF8500)
        else -> InventoryMuted
    }
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(Color.White).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(InventoryBackground), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Restaurant, null, Modifier.size(27.dp), tint = InventoryGreen)
        }
        Column(Modifier.weight(1f)) {
            Text(food.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(status.text, color = statusColor, fontSize = 14.sp,
                fontWeight = if (status.urgency == FoodUrgency.NORMAL) FontWeight.Normal else FontWeight.SemiBold)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(food.quantityLabel, fontSize = 15.sp)
            Text(when (food.location) { "freezer" -> "Freezer"; "fridge" -> "Fridge"; else -> "—" },
                color = InventoryMuted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun InventoryBottomBar(onFeature: (String) -> Unit) {
    // Keep system insets outside the shared icon/label layout.
    Box(Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding()) {
        Box(Modifier.fillMaxWidth().height(102.dp).background(InventoryBackground)) {
            Surface(
                modifier = Modifier.fillMaxSize().padding(top = 22.dp),
                color = Color.White,
                shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
            ) {}
            Row(Modifier.fillMaxSize().padding(start = 8.dp, end = 8.dp, bottom = 12.dp)) {
                BottomItem("Reels", R.drawable.ic_video, Modifier.weight(1f)) { onFeature("Reels") }
                BottomItem("Favorite", R.drawable.ic_favorite, Modifier.weight(1f)) { onFeature("Favorite") }
                BottomItem("Scan", R.drawable.ic_scan, Modifier.weight(1f), raised = true) { onFeature("Scan") }
                BottomItem("Inventory", R.drawable.ic_inventory, Modifier.weight(1f), selected = true) {}
                BottomItem("Setting", R.drawable.ic_settings, Modifier.weight(1f)) { onFeature("Settings") }
            }
        }
    }
}

@Composable
private fun BottomItem(
    label: String,
    @DrawableRes icon: Int,
    modifier: Modifier,
    selected: Boolean = false,
    raised: Boolean = false,
    onClick: () -> Unit
) {
    val color = if (selected) InventoryGreen else InventoryMuted
    Column(
        modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = if (raised) Alignment.TopCenter else Alignment.BottomCenter) {
            if (raised) {
                Surface(onClick = onClick, shape = CircleShape, shadowElevation = 8.dp,
                    color = InventoryGreen, modifier = Modifier.size(64.dp)) {
                    Box(Modifier.background(Brush.linearGradient(listOf(Color(0xFF08CE98), Color(0xFF00B47F)))),
                        contentAlignment = Alignment.Center) {
                        Icon(painterResource(icon), label, Modifier.size(34.dp), tint = Color.White)
                    }
                }
            } else {
                IconButton(onClick = onClick, modifier = Modifier.size(48.dp).clip(CircleShape)) {
                    Icon(painterResource(icon), label, Modifier.size(28.dp), tint = color)
                }
            }
        }
        // Identical text metrics and shared row height keep all five labels aligned.
        Text(label, fontSize = 11.sp, lineHeight = 16.sp, color = color, maxLines = 1)
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun InventoryPreview() {
    MaterialTheme {
        InventoryScreen(InventoryUiState(isLoading = false, stats = InventoryStats(5, 0, 1, 1), foods = listOf(
            Food("1", "Burgers", 2.0, "pcs", "fridge", expiryDate = "2026-10-13"),
            Food("2", "Beef rib-eye steak", 300.0, "g", "fridge", expiryDate = "2026-10-17"),
            Food("3", "Duck frozen", 2.0, "kg", "freezer", purchasedAt = "2026-10-04"),
            Food("4", "Tomatoes", 0.5, "kg", "fridge", purchasedAt = "2026-10-12"),
            Food("5", "Gouda cheese", 150.0, "g", "fridge", purchasedAt = "2026-10-13")
        )), {}, {}, {}, {}, {}) {}
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun EmptyInventoryPreview() {
    MaterialTheme { InventoryScreen(InventoryUiState(isLoading = false), {}, {}, {}, {}, {}) {} }
}
