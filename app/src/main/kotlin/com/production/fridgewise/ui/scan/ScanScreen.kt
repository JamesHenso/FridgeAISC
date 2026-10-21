package com.production.fridgewise.ui.scan

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.production.fridgewise.R
import com.production.fridgewise.domain.model.ScanMode
import com.production.fridgewise.ui.theme.FridgeWiseTheme
import java.io.File

@Composable
fun ScanRoute(accessToken: String, fridgeId: String?, onClose: () -> Unit, onComplete: () -> Unit) {
    val model: ScanViewModel = viewModel()
    val state by model.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val executor = remember(context) { ContextCompat.getMainExecutor(context) }
    val controller = remember(context) { LifecycleCameraController(context).apply {
        setEnabledUseCases(CameraController.IMAGE_CAPTURE)
        imageCaptureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
    } }
    var permission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var ready by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permission = it }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { model.showPhoto(it.toString()) }
    }
    LaunchedEffect(Unit) { if (!permission) permissionLauncher.launch(Manifest.permission.CAMERA) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer) }
    }
    DisposableEffect(controller, lifecycle, permission, state.photoUri, state.labelCaptureTarget, state.labelPhotoUri) {
        var active = true
        ready = false
        if (permission && (state.photoUri == null || (state.labelCaptureTarget != null && state.labelPhotoUri == null))) {
            val initialized = controller.initializationFuture
            initialized.addListener({
                if (active) {
                    try {
                        initialized.get()
                        controller.bindToLifecycle(lifecycle)
                        ready = true
                    } catch (error: Exception) {
                        model.fail("Không mở được camera. Bạn có thể chọn ảnh từ thư viện.")
                    }
                }
            }, executor)
        }
        onDispose { active = false; controller.unbind() }
    }
    DisposableEffect(state.torch, ready, controller) {
        var active = true
        if (ready) {
            if (state.torch && controller.cameraInfo?.hasFlashUnit() != true) {
                model.fail("Thiết bị không hỗ trợ đèn flash.")
            } else {
                val task = controller.enableTorch(state.torch)
                task.addListener({ if (active) runCatching { task.get() }.onFailure { model.fail("Không bật được đèn flash.") } }, executor)
            }
        }
        onDispose { active = false }
    }
    val view = LocalView.current
    DisposableEffect(view, state.photoUri) {
        val window = context.activity()?.window
        val bars = window?.let { WindowCompat.getInsetsController(it, view) }
        val previousStatus = bars?.isAppearanceLightStatusBars
        val previousNavigation = bars?.isAppearanceLightNavigationBars
        bars?.isAppearanceLightStatusBars = state.photoUri != null
        bars?.isAppearanceLightNavigationBars = state.photoUri != null
        onDispose {
            previousStatus?.let { bars?.isAppearanceLightStatusBars = it }
            previousNavigation?.let { bars?.isAppearanceLightNavigationBars = it }
        }
    }
    val close = { if (state.labelCaptureTarget != null) model.cancelLabelCapture() else { model.retake(); onClose() } }
    state.photoUri?.takeIf { state.labelCaptureTarget == null || state.labelPhotoUri != null }?.let { photo ->
        ReceiptReviewRoute(photo, accessToken, fridgeId, state.mode, reviewOwner = model,
            labelPhotoUri = state.labelPhotoUri, labelTargetId = state.labelCaptureTarget,
            onLabelConsumed = model::consumeLabelPhoto, onScanLabel = model::startLabelCapture,
            onBack = model::retake, onComplete = { model.retake(); onComplete() })
        return
    }
    BackHandler(onBack = close)
    ScanScreen(state, permission, ready, onClose = close, onMode = model::setMode,
        onTorch = { model.setTorch(!state.torch) },
        onGallery = { gallery.launch("image/*") },
        onPermission = {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
        },
        onCapture = {
            if (ready && model.beginCapture()) {
                val generation = model.captureGeneration
                try {
                    val file = File(context.cacheDir, "scan-${System.currentTimeMillis()}.jpg")
                    controller.takePicture(ImageCapture.OutputFileOptions.Builder(file).build(), executor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                                if (!model.completeCapture(generation, Uri.fromFile(file).toString())) file.delete()
                            }
                            override fun onError(error: ImageCaptureException) {
                                file.delete(); model.failCapture(generation, "Không chụp được ảnh. Vui lòng thử lại.")
                            }
                        })
                } catch (error: Exception) { model.fail("Không chụp được ảnh. Vui lòng thử lại.") }
            }
        }, onRetake = model::retake,
        camera = {
            if (permission) AndroidView(factory = { PreviewView(it).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
                this.controller = controller
            } }, modifier = Modifier.fillMaxSize())
        })
}

@Composable
private fun ScanScreen(
    state: ScanUiState, permission: Boolean, ready: Boolean,
    onClose: () -> Unit, onMode: (ScanMode) -> Unit, onTorch: () -> Unit, onGallery: () -> Unit,
    onPermission: () -> Unit, onCapture: () -> Unit, onRetake: () -> Unit,
    camera: @Composable () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val accent = colors.primaryContainer
    val onAccent = colors.onPrimaryContainer
    val reviewing = state.photoUri != null && state.labelCaptureTarget == null
    val product = state.mode == ScanMode.PRODUCT || state.labelCaptureTarget != null
    Box(Modifier.fillMaxSize().background(colors.scrim)) {
        if (reviewing) AsyncImage(state.photoUri, "Ảnh cần quét", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        else camera()
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (reviewing) 0.05f else 0.28f)))
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                Modifier.fillMaxWidth().height(64.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onClose,
                    Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.14f))
                ) {
                    Icon(Icons.Outlined.Close, "Đóng máy quét", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Text(
                    if (state.labelCaptureTarget != null) "Quét HSD sản phẩm" else if (product) "Quét sản phẩm" else "Quét hóa đơn",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                IconToggleButton(
                    checked = state.torch,
                    onCheckedChange = { onTorch() },
                    enabled = ready && !reviewing,
                    modifier = Modifier.size(48.dp).clip(CircleShape)
                        .background(if (state.torch) accent else Color.White.copy(alpha = 0.14f))
                ) {
                    Icon(
                        painterResource(R.drawable.ic_bolt), "Đèn flash",
                        Modifier.size(22.dp), tint = if (state.torch) onAccent else Color.White
                    )
                }
            }
            if (state.photoUri == null) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ScanModeChip("Hóa đơn", !product, accent, onAccent) { onMode(ScanMode.RECEIPT) }
                ScanModeChip("Sản phẩm", product, accent, onAccent) { onMode(ScanMode.PRODUCT) }
            }
            Spacer(Modifier.height(24.dp))
            Box(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                if (!reviewing) {
                    Box(Modifier.fillMaxSize().border(2.dp, accent, MaterialTheme.shapes.extraLarge))
                    Surface(
                        shape = CircleShape,
                        color = accent,
                        contentColor = onAccent,
                        modifier = Modifier.align(Alignment.TopCenter).offset(y = (-13).dp)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(painterResource(R.drawable.ic_scan), null, Modifier.size(14.dp))
                            Text(
                                if (product) "GIỮ NHÃN TRONG KHUNG" else "GIỮ HÓA ĐƠN TRONG KHUNG",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (permission && ready && state.error == null) ScanLine(accent)
                    if (!permission) Column(
                        Modifier.padding(24.dp).clip(MaterialTheme.shapes.large)
                            .background(Color.Black.copy(alpha = 0.55f)).padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Cho phép truy cập camera hoặc chọn ảnh từ thư viện.",
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        TextButton(onClick = onPermission) { Text("Mở cài đặt camera", color = accent) }
                    }
                    else if (!ready && state.error == null) CircularProgressIndicator(color = accent)
                }
                state.error?.let {
                    Text(
                        it,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.clip(MaterialTheme.shapes.large)
                            .background(Color.Black.copy(alpha = 0.7f)).padding(16.dp)
                    )
                }
            }
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.55f),
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Text(
                    if (reviewing) "Ảnh đã sẵn sàng • Kiểm tra độ rõ"
                    else if (product) "Chụp rõ tên, khối lượng, giá và HSD"
                    else "Đặt hóa đơn phẳng, tránh bóng đổ",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
            Box(Modifier.fillMaxWidth().height(104.dp), contentAlignment = Alignment.Center) {
                IconButton(
                    onClick = onGallery,
                    enabled = !state.isCapturing,
                    modifier = Modifier.align(Alignment.CenterStart).size(48.dp).clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.14f))
                ) {
                    Icon(painterResource(R.drawable.ic_picture), "Chọn ảnh", Modifier.size(24.dp), tint = Color.White)
                }
                if (reviewing) {
                    Button(
                        onClick = onRetake,
                        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = onAccent),
                        shape = MaterialTheme.shapes.large
                    ) { Text("Chụp lại") }
                } else {
                    Surface(
                        onClick = onCapture,
                        enabled = ready && !state.isCapturing,
                        shape = CircleShape,
                        color = Color.Transparent,
                        modifier = Modifier.size(72.dp).border(3.dp, Color.White, CircleShape),
                        contentColor = Color.White
                    ) {
                        Box(
                            Modifier.padding(8.dp).clip(CircleShape)
                                .background(if (ready) Color.White else Color.White.copy(alpha = 0.35f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.isCapturing) CircularProgressIndicator(
                                Modifier.size(30.dp),
                                color = colors.scrim,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanModeChip(
    label: String,
    selected: Boolean,
    accent: Color,
    onAccent: Color,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = accent,
            selectedLabelColor = onAccent,
            containerColor = Color.Black.copy(alpha = 0.4f),
            labelColor = Color.White
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = Color.White.copy(alpha = 0.35f),
            selectedBorderColor = Color.Transparent
        )
    )
}

@Composable
private fun ScanLine(accent: Color) {
    val transition = rememberInfiniteTransition(label = "receipt-scan")
    val progress by transition.animateFloat(
        0.08f, 0.92f,
        infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Reverse),
        label = "scan-line-position"
    )
    Canvas(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 16.dp)) {
        val y = size.height * progress
        drawLine(accent.copy(alpha = 0.15f), Offset(0f, y), Offset(size.width, y), strokeWidth = 10.dp.toPx())
        drawLine(accent, Offset(0f, y), Offset(size.width, y), strokeWidth = 2.dp.toPx())
    }
}

private tailrec fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ScanPreview() {
    FridgeWiseTheme {
        ScanScreen(ScanUiState(), permission = true, ready = true,
            onClose = {}, onMode = {}, onTorch = {}, onGallery = {}, onPermission = {},
            onCapture = {}, onRetake = {}, camera = {})
    }
}
