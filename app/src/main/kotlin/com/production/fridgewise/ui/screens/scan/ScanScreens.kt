package com.production.fridgewise.ui.screens.scan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.production.fridgewise.ui.theme.MyAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    onNextStep: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScanViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(ui.scanId, ui.step) {
        if (ui.step == ScanStep.CONFIRM) ui.scanId?.let(onNextStep)
    }
    val context = androidx.compose.ui.platform.LocalContext.current
    var photoUri by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<android.net.Uri?>(null) }
    var previewUri by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<android.net.Uri?>(null) }

    fun newPhotoUri(): android.net.Uri {
        val dir = java.io.File(context.cacheDir, "scan").apply { mkdirs() }
        val file = java.io.File(dir, "receipt_${System.currentTimeMillis()}.jpg")
        return androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
    fun send(uri: android.net.Uri) {
        previewUri = uri
        context.contentResolver.openInputStream(uri)?.use { viewModel.scanPhoto(it.readBytes()) }
    }

    val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.TakePicture()
    ) { ok -> if (ok) photoUri?.let(::send) }
    val galleryLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let(::send) }
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) cameraLauncher.launch(newPhotoUri().also { photoUri = it })
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Quét hóa đơn") },
                navigationIcon = { IconButton(onClick = onCancel) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "back") } }
            )
        }
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (previewUri != null) {
                coil3.compose.AsyncImage(
                    model = previewUri,
                    contentDescription = "hóa đơn",
                    modifier = Modifier.fillMaxWidth().height(220.dp)
                )
            }
            if (ui.isLoading) {
                com.production.fridgewise.ui.components.LoadingState()
            } else {
                Button(
                    onClick = {
                        photoUri = newPhotoUri()
                        when {
                            androidx.core.content.ContextCompat.checkSelfPermission(
                                context, android.Manifest.permission.CAMERA
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED ->
                                cameraLauncher.launch(photoUri!!)
                            else -> permissionLauncher.launch(android.Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Chụp ảnh") }
                OutlinedButton(
                    onClick = {
                        galleryLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Chọn ảnh") }
            }
            if (ui.error != null) Text(ui.error ?: "", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmItemsScreen(
    onNextStep: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScanViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(ui.step) {
        if (ui.step == ScanStep.ASSIGN) ui.scanId?.let(onNextStep)
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Xác nhận thực phẩm") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "back") } }
            )
        }
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(ui.items, key = { it.id ?: it.name }) { item ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(item.name, style = MaterialTheme.typography.titleSmall)
                            Text("${item.qtyDisplay ?: ""} ${item.unitDisplay.orEmpty()} · Mua ${item.purchasedAt.orEmpty()} · HSD ${item.expiryDate.orEmpty()}".trim(),
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            OutlinedButton(
                onClick = { viewModel.addManualItem(ScannedItemUi(name = "Món mới")) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Thêm thực phẩm") }
            Button(
                onClick = { viewModel.confirmItems() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Xác nhận") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignStorageScreen(
    onComplete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScanViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Phân loại") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "back") } }
            )
        }
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Chạm để gán ngăn (drag-and-drop để sau).", style = MaterialTheme.typography.bodyMedium)
            ui.items.filter { it.location == null }.forEach { item ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(item.name, modifier = Modifier.weight(1f))
                        OutlinedButton(onClick = { viewModel.assignLocation(item.id, "freezer") }) { Text("Đông") }
                        OutlinedButton(onClick = { viewModel.assignLocation(item.id, "fridge") }) { Text("Mát") }
                    }
                }
            }
            Text("Tủ đông (${ui.items.count { it.location == "freezer" }}) · Tủ lạnh (${ui.items.count { it.location == "fridge" }})")
            Button(
                onClick = { viewModel.commitScan(onComplete) },
                enabled = ui.items.isNotEmpty() && ui.items.all { it.location != null },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Hoàn tất") }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ScanPreview() {
    MyAppTheme { Text("Scan wizard") }
}
