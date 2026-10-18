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
import coil3.compose.AsyncImage
import com.production.fridgewise.R
import com.production.fridgewise.domain.model.*
import java.util.Locale
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
        } else if (destination == "report") {
            WeeklyReportScreen(
                state = state,
                onRefresh = model::refresh,
                onSignIn = onSignIn,
                onFeature = { feature ->
                    when (feature) {
                        "Inventory" -> page = "inventory"
                        "Scan" -> { scanFridgeId = state.selectedFridgeId; page = "scan" }
                        "Settings" -> scope.launch { snackbar.showSnackbar("Settings — coming soon") }
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
                    } else if (feature == "Inventory") {
                        page = "inventory"
                    } else scope.launch { snackbar.showSnackbar("$feature — coming soon") }
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
    onUseFood: (Food) -> Unit = {}
) {
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var showFridges by remember { mutableStateOf(false) }
    Scaffold(
        containerColor = InventoryBackground,
        contentColor = InventoryInk,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = { InventoryBottomBar(onFeature, selectedTab = "Inventory") }
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
                    IconButton(onClick = { onFeature("Settings") }, modifier = Modifier.align(Alignment.CenterEnd)) {
                        Icon(Icons.Outlined.Settings, "Settings", Modifier.size(27.dp))
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
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = onRefresh) { Icon(Icons.Outlined.Refresh, "Refresh inventory", tint = InventoryMuted) }
                }
                HorizontalDivider(color = Color(0xFFDFEAF2))
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
                        if (state.query.isNotBlank()) {
                            TextButton(onClick = { onSearch("") }) { Text("Clear search") }
                        }
                    }
                }
                else -> items(state.foods, key = { it.id }) { food -> FoodRow(food, onUse = { onUseFood(food) }) }
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
private fun FoodRow(food: Food, onUse: () -> Unit) {
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
            TextButton(onClick = onUse, enabled = status.urgency != FoodUrgency.EXPIRED,
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)) {
                Text(if (status.urgency == FoodUrgency.EXPIRED) "Đã quá hạn" else "Dùng món này",
                    color = if (status.urgency == FoodUrgency.EXPIRED) InventoryMuted else InventoryGreen,
                    fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun InventoryBottomBar(onFeature: (String) -> Unit, selectedTab: String) {
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
                BottomItem("Inventory", R.drawable.ic_inventory, Modifier.weight(1f), selected = selectedTab == "Inventory") { onFeature("Inventory") }
                BottomItem("Báo cáo", R.drawable.ic_report, Modifier.weight(1f), selected = selectedTab == "Report") { onFeature("Report") }
            }
        }
    }
}

@Composable
private fun WeeklyReportScreen(
    state: InventoryUiState,
    onRefresh: () -> Unit,
    onSignIn: () -> Unit,
    onFeature: (String) -> Unit
) {
    val expiredFoods = remember(state.foods) { state.foods.filter { it.status().urgency == FoodUrgency.EXPIRED } }
    val knownExpiredSpend = remember(expiredFoods) { expiredFoods.mapNotNull { it.estimatedValueVnd }.sum() }
    val missingPriceCount = remember(expiredFoods) { expiredFoods.count { it.estimatedValueVnd == null } }
    Scaffold(
        containerColor = InventoryBackground,
        contentColor = InventoryInk,
        bottomBar = { InventoryBottomBar(onFeature, selectedTab = "Report") }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth().height(58.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Báo cáo", fontSize = 27.sp, fontWeight = FontWeight.Bold, color = InventoryInk)
                        Text("Thực phẩm đã hết hạn", fontSize = 14.sp, color = InventoryMuted)
                    }
                    IconButton(onClick = onRefresh) { Icon(Icons.Outlined.Refresh, "Làm mới báo cáo", tint = InventoryMuted) }
                    IconButton(onClick = { onFeature("Settings") }) { Icon(Icons.Outlined.Settings, "Cài đặt", tint = InventoryMuted) }
                }
            }
            if (state.isLoading) {
                item { Box(Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = InventoryGreen)
                } }
            } else if (state.error != null) {
                item {
                    Surface(shape = RoundedCornerShape(20.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (state.needsSignIn) "Phiên đăng nhập đã hết hạn" else "Không tải được dữ liệu kho",
                                fontWeight = FontWeight.SemiBold, color = InventoryInk)
                            Text(state.error, color = InventoryMuted, modifier = Modifier.padding(top = 6.dp))
                            TextButton(onClick = if (state.needsSignIn) onSignIn else onRefresh) {
                                Text(if (state.needsSignIn) "Đăng nhập" else "Thử lại")
                            }
                        }
                    }
                }
            } else {
                item {
                    Surface(shape = RoundedCornerShape(22.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Box(Modifier.size(60.dp).clip(CircleShape).background(Color(0xFFE5F7EC)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.DeleteOutline, null, Modifier.size(30.dp), tint = Color(0xFF08734D))
                            }
                            Column(Modifier.weight(1f)) {
                                Text("Thực phẩm hết hạn", color = InventoryMuted, fontSize = 14.sp)
                                Text("${expiredFoods.size}", fontSize = 32.sp, lineHeight = 38.sp,
                                    fontWeight = FontWeight.Bold, color = InventoryInk)
                            }
                        }
                    }
                }
                item {
                    Surface(shape = RoundedCornerShape(22.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Box(Modifier.size(60.dp).clip(CircleShape).background(Color(0xFFFFF2D9)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.Payments, null, Modifier.size(29.dp), tint = Color(0xFFAA6A00))
                            }
                            Column(Modifier.weight(1f)) {
                                Text("Chi tiêu cho thực phẩm hết hạn", color = InventoryMuted, fontSize = 14.sp)
                                Text(formatVnd(knownExpiredSpend), fontSize = 25.sp, lineHeight = 31.sp,
                                    fontWeight = FontWeight.Bold, color = InventoryInk)
                                Text(
                                    if (missingPriceCount == 0) "Ước tính theo số lượng còn trong kho"
                                    else "$missingPriceCount món chưa có giá; tổng chỉ gồm món đã ghi giá",
                                    color = InventoryMuted, fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Danh sách hết hạn", Modifier.weight(1f), fontSize = 19.sp, fontWeight = FontWeight.Bold, color = InventoryInk)
                        Text("${expiredFoods.size}", color = InventoryMuted, fontSize = 13.sp)
                    }
                }
                if (expiredFoods.isEmpty()) {
                    item {
                        Surface(shape = RoundedCornerShape(20.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.fillMaxWidth().padding(vertical = 30.dp, horizontal = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Outlined.Eco, null, Modifier.size(38.dp), tint = InventoryGreen)
                                Text("Chưa có thực phẩm hết hạn", Modifier.padding(top = 10.dp), fontWeight = FontWeight.SemiBold, color = InventoryInk)
                            }
                        }
                    }
                } else {
                    items(expiredFoods, key = { "report-${it.id}" }) { food ->
                        ExpiredFoodReportRow(food)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpiredFoodReportRow(food: Food) {
    Surface(shape = RoundedCornerShape(18.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFF2F7F3)), contentAlignment = Alignment.Center) {
                if (!food.imageUrl.isNullOrBlank()) {
                    AsyncImage(food.imageUrl, food.name, Modifier.fillMaxSize().padding(4.dp))
                } else {
                    Icon(Icons.Outlined.Restaurant, null, Modifier.size(27.dp), tint = Color(0xFF16805B))
                }
            }
            Column(Modifier.weight(1f)) {
                Text(food.name, fontWeight = FontWeight.SemiBold, color = InventoryInk, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("HSD ${formatDate(food.expiryDate)}", color = InventoryMuted, fontSize = 12.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(food.quantityLabel, fontWeight = FontWeight.SemiBold, color = InventoryInk, fontSize = 14.sp)
                Text(food.estimatedValueVnd?.let(::formatVnd) ?: "Chưa có giá", color = InventoryMuted, fontSize = 12.sp)
            }
        }
    }
}

private fun formatVnd(amount: Long): String =
    String.format(Locale("vi", "VN"), "%,d đ", amount).replace(',', '.')

private fun formatDate(value: String?): String {
    if (value.isNullOrBlank()) return "chưa rõ hạn"
    return runCatching {
        val input = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
        val output = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("vi-VN"))
        output.format(input.parse(value.take(10))!!)
    }.getOrDefault(value.take(10))
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
        )), {}, {}, {}, {}, onFeature = {})
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun EmptyInventoryPreview() {
    MaterialTheme { InventoryScreen(InventoryUiState(isLoading = false), {}, {}, {}, {}, onFeature = {}) }
}
