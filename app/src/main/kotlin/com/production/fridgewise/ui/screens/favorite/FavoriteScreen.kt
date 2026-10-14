package com.production.fridgewise.ui.screens.favorite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.production.fridgewise.ui.components.EmptyState
import com.production.fridgewise.ui.components.ErrorState
import com.production.fridgewise.ui.components.LoadingState
import com.production.fridgewise.ui.components.RecipeCard
import com.production.fridgewise.ui.theme.MyAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteScreen(
    onRecipeClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FavoriteViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    Scaffold(modifier = modifier, topBar = { TopAppBar(title = { Text("Yêu thích") }) }) { innerPadding ->
        when {
            ui.isLoading -> LoadingState(Modifier.padding(innerPadding))
            ui.error != null -> ErrorState(ui.error ?: "", viewModel::refresh, Modifier.padding(innerPadding))
            ui.favorites.isEmpty() -> EmptyState("Chưa có món yêu thích", Modifier.padding(innerPadding))
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ui.favorites, key = { it.id }) {
                    RecipeCard(it, onClick = { onRecipeClick(it.id) }, onFavoriteClick = { viewModel.removeFavorite(it.id) })
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FavoritePreview() {
    MyAppTheme { EmptyState("Chưa có món yêu thích") }
}
