package com.production.fridgewise.ui.screens.storagelist

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.production.fridgewise.ui.components.EmptyState
import com.production.fridgewise.ui.components.ErrorState
import com.production.fridgewise.ui.components.FoodItemCard
import com.production.fridgewise.ui.components.LoadingState
import com.production.fridgewise.ui.theme.MyAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageListScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StorageListViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(ui.title) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "back") } }
            )
        }
    ) { innerPadding ->
        when {
            ui.isLoading -> LoadingState(Modifier.padding(innerPadding))
            ui.error != null -> ErrorState(ui.error ?: "", viewModel::refresh, Modifier.padding(innerPadding))
            ui.foods.isEmpty() -> EmptyState("Ngăn này trống", Modifier.padding(innerPadding))
            else -> LazyColumn(Modifier.fillMaxSize().padding(innerPadding)) {
                items(ui.foods, key = { it.id }) { FoodItemCard(it, onClick = {}) }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StorageListPreview() {
    MyAppTheme { EmptyState("Ngăn này trống") }
}
