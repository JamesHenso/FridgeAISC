package com.production.fridgewise.ui.screens.family

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.production.fridgewise.ui.components.EmptyState
import com.production.fridgewise.ui.theme.MyAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyScreen(modifier: Modifier = Modifier, viewModel: FamilyViewModel = hiltViewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    var code by rememberSaveable { mutableStateOf("") }
    var confirmJoin by rememberSaveable { mutableStateOf(false) }
    Scaffold(modifier = modifier, topBar = { TopAppBar(title = { Text("Gia đình") }) }) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Mã mời", style = MaterialTheme.typography.labelMedium)
                    Text(ui.inviteCode ?: "—", style = MaterialTheme.typography.titleMedium)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(value = code, onValueChange = { code = it }, placeholder = { Text("Nhập mã") }, modifier = Modifier.weight(1f), singleLine = true)
                Button(onClick = { confirmJoin = true }, enabled = code.isNotBlank()) { Text("Tham gia") }
            }
            if (ui.members.isEmpty()) EmptyState("Chưa có thành viên")
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ui.members, key = { it.user_id }) {
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(it.name ?: it.email.orEmpty(), style = MaterialTheme.typography.titleSmall)
                                Text(it.email.orEmpty(), style = MaterialTheme.typography.bodySmall)
                            }
                            Text(it.role, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            OutlinedButton(onClick = { viewModel.refresh() }, modifier = Modifier.fillMaxWidth()) { Text("Tải lại") }
        }
        if (confirmJoin) {
            AlertDialog(
                onDismissRequest = { confirmJoin = false },
                title = { Text("Tham gia tủ?") },
                text = { Text("Dùng mã $code để tham gia.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.joinWithCode(code); code = ""; confirmJoin = false
                    }) { Text("Đồng ý") }
                },
                dismissButton = { TextButton(onClick = { confirmJoin = false }) { Text("Hủy") } }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FamilyPreview() {
    MyAppTheme { EmptyState("Chưa có thành viên") }
}
