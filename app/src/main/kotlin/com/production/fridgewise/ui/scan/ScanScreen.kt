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
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.production.fridgewise.R
import com.production.fridgewise.domain.model.ScanMode
import java.io.File

private val ScanLime = Color(0xFFB9ED6A)

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
    val reviewing = state.photoUri != null && state.labelCaptureTarget == null
    val product = state.mode == ScanMode.PRODUCT || state.labelCaptureTarget != null
    Box(Modifier.fillMaxSize().background(Color(0xFF18241F))) {
        if (reviewing) AsyncImage(state.photoUri, "Ảnh cần quét", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        else camera()
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (reviewing) 0.05f else 0.22f)))
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth().height(62.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = onClose, Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.14f))) {
                    Icon(Icons.Outlined.Close, "Đóng máy quét", tint = Color.White, modifier = Modifier.size(21.dp))
                }
                Text(if (state.labelCaptureTarget != null) "Quét HSD sản phẩm" else if (product) "Quét sản phẩm" else "Quét hóa đơn", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                IconToggleButton(checked = state.torch, onCheckedChange = { onTorch() }, enabled = ready && !reviewing,
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(if (state.torch) ScanLime else Color.White.copy(alpha = 0.14f))) {
                    Icon(painterResource(R.drawable.ic_bolt), "Đèn flash", Modifier.size(23.dp), tint = if (state.torch) Color(0xFF24321D) else Color.White)
                }
            }
            if (state.photoUri == null) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !product, onClick = { onMode(ScanMode.RECEIPT) }, label = { Text("Hóa đơn") })
                FilterChip(selected = product, onClick = { onMode(ScanMode.PRODUCT) }, label = { Text("Sản phẩm") })
            }
            Spacer(Modifier.height(22.dp))
            Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 22.dp), contentAlignment = Alignment.Center) {
                if (!reviewing) {
                    Box(Modifier.fillMaxSize().border(2.dp, ScanLime, RoundedCornerShape(18.dp)))
                    Surface(shape = CircleShape, color = ScanLime, modifier = Modifier.align(Alignment.TopCenter).offset(y = (-13).dp)) {
                        Row(Modifier.padding(horizontal = 13.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(painterResource(R.drawable.ic_scan), null, Modifier.size(13.dp), tint = Color(0xFF24321D))
                            Text(if (product) "GIỮ NHÃN TRONG KHUNG" else "GIỮ HÓA ĐƠN TRONG KHUNG", color = Color(0xFF24321D), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (permission && ready && state.error == null) ScanLine()
                    if (!permission) Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Cho phép truy cập camera hoặc chọn ảnh từ thư viện.", color = Color.White, textAlign = TextAlign.Center)
                        TextButton(onClick = onPermission) { Text("Mở cài đặt camera", color = ScanLime) }
                    } else if (!ready && state.error == null) CircularProgressIndicator(color = ScanLime)
                }
                state.error?.let { Text(it, color = Color.White, textAlign = TextAlign.Center,
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(Color.Black.copy(alpha = 0.65f)).padding(16.dp)) }
            }
            Surface(shape = CircleShape, color = Color(0xCC253C34), modifier = Modifier.padding(top = 10.dp)) {
                Text(if (reviewing) "Ảnh đã sẵn sàng • Kiểm tra độ rõ" else if (product) "Chụp rõ tên, khối lượng, giá và HSD" else "Đặt hóa đơn phẳng, tránh bóng đổ",
                    color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 15.dp, vertical = 9.dp))
            }
            Box(Modifier.fillMaxWidth().height(102.dp), contentAlignment = Alignment.Center) {
                IconButton(onClick = onGallery, enabled = !state.isCapturing,
                    modifier = Modifier.align(Alignment.CenterStart).size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.14f))) {
                    Icon(painterResource(R.drawable.ic_picture), "Chọn ảnh", Modifier.size(24.dp), tint = Color.White)
                }
                if (reviewing) Button(onClick = onRetake, colors = ButtonDefaults.buttonColors(containerColor = ScanLime, contentColor = Color(0xFF24321D))) {
                    Text("Chụp lại")
                } else Surface(onClick = onCapture, enabled = ready && !state.isCapturing, shape = CircleShape, color = Color.Transparent,
                    modifier = Modifier.size(70.dp).border(3.dp, Color.White, CircleShape),
                    contentColor = Color.White) {
                    Box(Modifier.padding(7.dp).clip(CircleShape).background(if (ready) Color.White else Color.White.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center) {
                        if (state.isCapturing) CircularProgressIndicator(Modifier.size(30.dp), color = Color(0xFF253C34), strokeWidth = 2.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanLine() {
    val transition = rememberInfiniteTransition(label = "receipt-scan")
    val progress by transition.animateFloat(0.08f, 0.92f,
        infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Reverse), label = "scan-line-position")
    Canvas(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 16.dp)) {
        val y = size.height * progress
        drawLine(ScanLime.copy(alpha = 0.15f), Offset(0f, y), Offset(size.width, y), strokeWidth = 10.dp.toPx())
        drawLine(ScanLime, Offset(0f, y), Offset(size.width, y), strokeWidth = 2.dp.toPx())
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
    MaterialTheme {
        ScanScreen(ScanUiState(), permission = true, ready = true,
            onClose = {}, onMode = {}, onTorch = {}, onGallery = {}, onPermission = {},
            onCapture = {}, onRetake = {}, camera = {})
    }
}
