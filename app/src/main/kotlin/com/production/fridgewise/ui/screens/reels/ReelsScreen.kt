package com.production.fridgewise.ui.screens.reels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.production.fridgewise.ui.theme.MyAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelsScreen(
    onNavigateToRecipeDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReelsViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    Scaffold(modifier = modifier, topBar = { TopAppBar(title = { Text("Reels") }) }) { innerPadding ->
        when {
            ui.isLoading -> LoadingState(Modifier.padding(innerPadding))
            ui.error != null -> ErrorState(ui.error ?: "", viewModel::load, Modifier.padding(innerPadding))
            ui.reels.isEmpty() -> EmptyState("Chưa có video", Modifier.padding(innerPadding))
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(ui.reels, key = { it.id }) { reel ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(reel.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                listOfNotNull(
                                    reel.servings?.let { "Khẩu phần $it" },
                                    reel.time?.let { "${it}p" },
                                    reel.difficulty,
                                    reel.likeCount?.let { "♥ $it" }
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { viewModel.likeReel(reel.id) }) {
                                    Text(if (reel.isLiked) "Đã thích" else "Thích")
                                }
                                Button(onClick = { onNavigateToRecipeDetail(reel.id) }) { Text("Nấu ngay") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReelsPreview() {
    MyAppTheme { EmptyState("Chưa có video") }
}
