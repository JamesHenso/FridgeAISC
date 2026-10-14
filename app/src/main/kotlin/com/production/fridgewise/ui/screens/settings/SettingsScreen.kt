package com.production.fridgewise.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.production.fridgewise.ui.theme.MyAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    var renameOpen by rememberSaveable { mutableStateOf(false) }
    var renameText by rememberSaveable { mutableStateOf(ui.fridgeName) }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Cài đặt") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "back") } }
            )
        }
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Tài khoản", style = MaterialTheme.typography.titleMedium)
            Text("Tủ: ${ui.fridgeName}", style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = { renameText = ui.fridgeName; renameOpen = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Đổi tên tủ")
            }
            Text("Preferences", style = MaterialTheme.typography.titleMedium)
            Text("Ngôn ngữ · Thông báo (deferred — chưa có backend)", style = MaterialTheme.typography.bodySmall)
            Button(onClick = { viewModel.logout(); onLogout() }, modifier = Modifier.fillMaxWidth()) {
                Text("Đăng xuất")
            }
        }
        if (renameOpen) {
            AlertDialog(
                onDismissRequest = { renameOpen = false },
                title = { Text("Đổi tên tủ") },
                text = {
                    TextField(value = renameText, onValueChange = { renameText = it }, singleLine = true)
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.renameFridge(renameText); renameOpen = false }) { Text("Lưu") }
                },
                dismissButton = { TextButton(onClick = { renameOpen = false }) { Text("Hủy") } }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsPreview() {
    MyAppTheme { Text("Cài đặt") }
}
