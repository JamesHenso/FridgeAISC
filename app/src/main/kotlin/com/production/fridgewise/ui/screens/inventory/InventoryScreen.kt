package com.production.fridgewise.ui.screens.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.production.fridgewise.ui.components.EmptyState
import com.production.fridgewise.ui.components.ErrorState
import com.production.fridgewise.ui.components.ExpiryStatus
import com.production.fridgewise.ui.components.FoodItemUi
import com.production.fridgewise.ui.components.LoadingState
import com.production.fridgewise.ui.theme.MyAppTheme
import java.time.LocalDate

private fun visibleFoods(ui: InventoryUiState, today: LocalDate): List<FoodItemUi> {
    val all = ui.freezerFoods + ui.fridgeFoods
    val byTab = if (ui.tab == HomeTab.HAVE) all
    else all.filter { it.expiryStatus != ExpiryStatus.NORMAL }
    val byStorage = if (ui.storage == null) byTab else byTab.filter { it.location == ui.storage }
    val sorted = byStorage.sortedWith(compareBy(nullsLast()) { it.expiryDate })
    return if (ui.sortAsc) sorted else sorted.asReversed()
}

private fun <T : Comparable<T>> nullsLast(): Comparator<T?> =
    Comparator { a, b ->
        when {
            a == null && b == null -> 0
            a == null -> 1
            b == null -> -1
            else -> a.compareTo(b)
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    onNavigateToStorage: (String) -> Unit,
    onNavigateToRecipes: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InventoryViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val today = remember { LocalDate.now() }
    val foods = remember(ui.freezerFoods, ui.fridgeFoods, ui.tab, ui.storage, ui.sortAsc) {
        visibleFoods(ui, today)
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (ui.fridgeName.isEmpty()) "FridgeWise" else ui.fridgeName) },
                navigationIcon = { IconButton(onClick = {}) { Icon(Icons.Default.Search, "search") } },
                actions = { IconButton(onClick = onNavigateToSettings) { Icon(Icons.Default.Settings, "settings") } }
            )
        }
    ) { innerPadding ->
        when {
            ui.isLoading -> LoadingState(Modifier.padding(innerPadding))
            ui.error != null -> ErrorState(ui.error ?: "", onRetry = viewModel::refresh, modifier = Modifier.padding(innerPadding))
            ui.fridgeId == null -> EmptyState("Chưa có tủ lạnh — hãy tạo tủ mới", Modifier.padding(innerPadding))
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "toggle") {
                    HaveNeedToggle(selected = ui.tab, onChange = viewModel::setTab)
                }
                item(key = "stats") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        statCards(ui.stats).chunked(2).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                row.forEach { (number, label, icon) ->
                                    StatCard(number = number, label = label, icon = icon, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
                item(key = "banner") {
                    RecipesBanner(onClick = { ui.fridgeId?.let(onNavigateToRecipes) })
                }
                item(key = "filters") {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StorageDropdown(selected = ui.storage, onSelect = viewModel::setStorage, modifier = Modifier.weight(1f))
                        IconButton(onClick = viewModel::toggleSort) {
                            Icon(Icons.AutoMirrored.Filled.Sort, "sort by expiry")
                        }
                    }
                }
                item(key = "chips") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(null to "All", "fridge" to "Fridge", "freezer" to "Freezer").forEach { (value, label) ->
                            val active = ui.storage == value
                            FilterChip(
                                selected = active,
                                onClick = { viewModel.setStorage(value) },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.inverseSurface,
                                    selectedLabelColor = MaterialTheme.colorScheme.inverseOnSurface
                                )
                            )
                        }
                    }
                }
                if (foods.isEmpty()) {
                    item(key = "empty") { Text("Chưa có thực phẩm", style = MaterialTheme.typography.bodyMedium) }
                } else {
                    items(foods, key = { it.id }) { food ->
                        HomeFoodRow(
                            food = food,
                            onClick = { onNavigateToStorage(food.location ?: "fridge") }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun InventoryPreview() {
    MyAppTheme {
        InventoryScreenPreviewContent()
    }
}

@Composable
private fun InventoryScreenPreviewContent() {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HaveNeedToggle(HomeTab.HAVE, onChange = {})
    }
}
