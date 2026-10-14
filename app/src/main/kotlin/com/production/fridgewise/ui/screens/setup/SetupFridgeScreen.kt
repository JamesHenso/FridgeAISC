package com.production.fridgewise.ui.screens.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.production.fridgewise.ui.theme.MyAppTheme

@Composable
fun SetupFridgeScreen(
    onSetupComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SetupFridgeViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Đặt tên tủ lạnh", style = MaterialTheme.typography.headlineSmall)
            TextField(value = ui.name, onValueChange = viewModel::onNameChange,
                placeholder = { Text("VD: Tủ nhà") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(onClick = { viewModel.createFridge(onSetupComplete) }, modifier = Modifier.fillMaxWidth()) {
                Text("Tiếp tục")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SetupPreview() {
    MyAppTheme { Text("Đặt tên tủ lạnh") }
}
