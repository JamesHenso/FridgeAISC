package com.production.fridgewise.ui.screens.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.production.fridgewise.ui.components.EmptyState
import com.production.fridgewise.ui.components.ErrorState
import com.production.fridgewise.ui.components.FoodItemCard
import com.production.fridgewise.ui.components.LoadingState
import com.production.fridgewise.ui.theme.MyAppTheme

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
    InventoryContent(ui = ui, onRefresh = viewModel::refresh,
        onNavigateToStorage = onNavigateToStorage,
        onNavigateToRecipes = onNavigateToRecipes,
        onNavigateToSettings = onNavigateToSettings, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryContent(
    ui: InventoryUiState,
    onRefresh: () -> Unit,
    onNavigateToStorage: (String) -> Unit,
    onNavigateToRecipes: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(ui.fridgeName.ifEmpty { "FridgeWise" }) },
                navigationIcon = { IconButton(onClick = {}) { Icon(Icons.Default.Search, "search") } },
                actions = { IconButton(onClick = onNavigateToSettings) { Icon(Icons.Default.Settings, "settings") } }
            )
        }
    ) { innerPadding ->
        when {
            ui.isLoading -> LoadingState(Modifier.padding(innerPadding))
            ui.error != null -> ErrorState(ui.error ?: "", onRetry = onRefresh, modifier = Modifier.padding(innerPadding))
            ui.fridgeId == null -> EmptyState("Chưa có tủ lạnh — hãy tạo tủ mới", Modifier.padding(innerPadding))
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "stats") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "In Stock" to (ui.stats?.inStock ?: 0).toString(),
                            "Use Today" to (ui.stats?.useToday ?: 0).toString(),
                            "Use Soon" to (ui.stats?.useSoon ?: 0).toString(),
                            "Clean Out" to (ui.stats?.cleanOut ?: 0).toString()
                        ).forEach { (label, value) ->
                            Card(Modifier.weight(1f)) {
                                Column(Modifier.padding(8.dp)) {
                                    Text(value, style = MaterialTheme.typography.titleMedium)
                                    Text(label, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
                item(key = "freezer-h") {
                    Text("Freezer — Ngăn đông", style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.clickable { onNavigateToStorage("freezer") })
                }
                items(ui.freezerFoods.take(3), key = { it.id }) {
                    FoodItemCard(it, onClick = { onNavigateToStorage("freezer") })
                }
                item(key = "fridge-h") {
                    Text("Fridge — Ngăn mát", style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.clickable { onNavigateToStorage("fridge") })
                }
                items(ui.fridgeFoods.take(3), key = { it.id }) {
                    FoodItemCard(it, onClick = { onNavigateToStorage("fridge") })
                }
                item(key = "cook") {
                    Button(onClick = { ui.fridgeId?.let(onNavigateToRecipes) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Nấu ăn")
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
        InventoryContent(
            ui = InventoryUiState(fridgeId = "f1", fridgeName = "Tủ nhà"), onRefresh = {},
            onNavigateToStorage = {}, onNavigateToRecipes = {}, onNavigateToSettings = {}
        )
    }
}
