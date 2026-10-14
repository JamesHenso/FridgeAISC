package com.production.fridgewise.ui.screens.recipes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.production.fridgewise.ui.components.EmptyState
import com.production.fridgewise.ui.components.ErrorState
import com.production.fridgewise.ui.components.LoadingState
import com.production.fridgewise.ui.theme.MyAppTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecipeDetailViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    var sheet by rememberSaveable { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    fun closeSheet() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { sheet = null }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(ui.info?.name ?: "Chi tiết món") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "back") } }
            )
        }
    ) { innerPadding ->
        when {
            ui.isLoading -> LoadingState(Modifier.padding(innerPadding))
            ui.error != null -> ErrorState(ui.error ?: "", viewModel::load, Modifier.padding(innerPadding))
            ui.info == null -> EmptyState("Không có dữ liệu", Modifier.padding(innerPadding))
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "meta") {
                    Text(
                        listOfNotNull(
                            ui.detail?.servings?.let { "Khẩu phần $it" },
                            ui.detail?.time?.let { "${it}p" },
                            ui.detail?.difficulty
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                item(key = "actions") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { sheet = "have" }) { Text("Có sẵn (${ui.matched.size})") }
                        Button(onClick = { sheet = "need" }) { Text("Cần mua (${ui.missing.size})") }
                    }
                }
                item(key = "ing-h") { Text("Nguyên liệu", style = MaterialTheme.typography.titleMedium) }
                items(ui.ingredients, key = { it.ingredientId ?: it.name }) {
                    Text("• ${it.name} ${it.quantity.orEmpty()} ${it.unit.orEmpty()}".trim())
                }
                item(key = "steps") {
                    Text("Cách làm", style = MaterialTheme.typography.titleMedium)
                    Text(ui.detail?.instructions.orEmpty())
                }
            }
        }
        if (sheet != null) {
            ModalBottomSheet(onDismissRequest = { sheet = null }, sheetState = sheetState) {
                val list = if (sheet == "have") ui.matched else ui.missing
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (sheet == "have") "Nguyên liệu có sẵn" else "Nguyên liệu cần mua",
                        style = MaterialTheme.typography.titleMedium)
                    if (list.isEmpty()) Text("Trống")
                    list.forEach { Text("• ${it.name}") }
                    Button(onClick = ::closeSheet, modifier = Modifier.fillMaxWidth()) { Text("Đóng") }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RecipeDetailPreview() {
    MyAppTheme { EmptyState("Không có dữ liệu") }
}
