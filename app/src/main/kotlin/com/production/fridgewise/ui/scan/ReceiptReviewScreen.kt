package com.production.fridgewise.ui.scan

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil3.compose.AsyncImage
import com.production.fridgewise.R
import com.production.fridgewise.domain.model.ReceiptItem
import com.production.fridgewise.domain.model.ScanMode
import com.production.fridgewise.ui.theme.FridgeWiseTheme
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun ReceiptReviewRoute(photoUri: String, token: String, fridgeId: String?, mode: ScanMode,
    reviewOwner: ViewModelStoreOwner, labelPhotoUri: String?, labelTargetId: String?,
    onLabelConsumed: () -> Unit, onScanLabel: (String) -> Unit,
    onBack: () -> Unit, onComplete: () -> Unit) {
    val resolver = LocalContext.current.applicationContext.contentResolver
    val uri = remember(photoUri) { Uri.parse(photoUri) }
    val model: ReceiptReviewViewModel = viewModel(viewModelStoreOwner = reviewOwner,
        factory = viewModelFactory { initializer { ReceiptReviewViewModel(token, fridgeId) } })
    val state by model.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val load = {
        model.load(mode, "image/jpeg") { jpegStream { resolver.openInputStream(uri) ?: error("Không mở được ảnh.") } }
    }
    LaunchedEffect(model) { load() }
    LaunchedEffect(labelPhotoUri, labelTargetId) {
        if (labelPhotoUri != null && labelTargetId != null) {
            val labelUri = Uri.parse(labelPhotoUri)
            model.scanLabel(labelTargetId, "image/jpeg") {
                jpegStream { resolver.openInputStream(labelUri) ?: error("Không mở được ảnh nhãn.") }
            }
            onLabelConsumed()
        }
    }
    LaunchedEffect(state.completed) { if (state.completed) onComplete() }
    val back: () -> Unit = { if (!state.saving && !state.committing) { model.cancelPending(); onBack() } }
    BackHandler(onBack = back)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        AnimatedContent(targetState = when {
            state.loading -> "loading"
            state.error != null -> "error"
            state.committing || state.saved || state.commitError != null -> "putting"
            else -> "review"
        }, transitionSpec = {
            (fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 15 }).togetherWith(fadeOut(tween(180)))
        },
            modifier = Modifier.fillMaxSize().padding(padding), label = "receipt-processing") { page ->
            when (page) {
                "loading" -> ReceiptLoading(photoUri, state.stage, back)
                "error" -> Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        photoUri, null,
                        Modifier.size(160.dp).clip(MaterialTheme.shapes.large),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        state.error.orEmpty(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Button(onClick = { load() }, modifier = Modifier.padding(top = 16.dp)) { Text("Thử lại") }
                    TextButton(onClick = back) { Text("Chụp lại hóa đơn") }
                }
                "putting" -> PuttingAwayScreen(state.items, state.saved, state.commitError,
                    onRetry = model::finish, onBack = back)
                else -> ReceiptReviewScreen(state, back, model::edit, model::finish, onScanLabel) {
                    scope.launch { snackbar.showSnackbar("Coming soon") }
                }
            }
        }
    }
    state.items.firstOrNull { it.id == state.editingId }?.let { original ->
        val item = state.labelSuggestion ?: original
        ReceiptEditDialog(item, state.saving, state.editError, onDismiss = { model.edit(null) }, onSave = model::save)
    }
}

/** Azure Document Intelligence only reads JPEG/PNG and rejects anything over 4 MB, but a picked image
 *  can be WebP/HEIC (some providers report those as image/jpeg) and camera shots are often larger.
 *  Decode whatever the URI holds and re-encode it as a JPEG we know Azure accepts. */
private fun jpegStream(open: () -> InputStream): InputStream {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    open().use { BitmapFactory.decodeStream(it, null, bounds) }
    require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Không đọc được ảnh. Vui lòng chọn ảnh JPG hoặc PNG." }
    val longest = maxOf(bounds.outWidth, bounds.outHeight)
    var sample = 1
    while (longest / (sample * 2) >= 1200) sample *= 2
    val bitmap = open().use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
        ?: error("Không đọc được ảnh. Vui lòng chọn ảnh JPG hoặc PNG.")
    return java.io.ByteArrayOutputStream()
        .also { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it); bitmap.recycle() }
        .toByteArray().inputStream()
}

@Composable
private fun ReceiptLoading(photoUri: String, stage: String, onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "receipt-loading")
    val tilt by transition.animateFloat(-2f, 2f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "receipt-tilt")
    val line by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse), label = "receipt-reading")
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Quay lại máy quét")
        }
        Spacer(Modifier.weight(1f))
        Box(
            Modifier.widthIn(max = 260.dp).fillMaxWidth().aspectRatio(0.78f)
                .graphicsLayer { rotationZ = tilt }
                .clip(MaterialTheme.shapes.extraLarge)
                .background(colors.surfaceContainerLowest)
                .border(1.dp, colors.outlineVariant, MaterialTheme.shapes.extraLarge)
        ) {
            AsyncImage(
                photoUri, "Hóa đơn đang xử lý",
                Modifier.fillMaxSize().padding(12.dp),
                contentScale = ContentScale.Fit
            )
            BoxWithConstraints(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().offset(y = maxHeight * line).height(3.dp).background(colors.primary))
            }
        }
        Spacer(Modifier.height(32.dp))
        CircularProgressIndicator(Modifier.size(30.dp), color = colors.primary, strokeWidth = 3.dp)
        Text(
            stage,
            style = MaterialTheme.typography.titleLarge,
            color = colors.onSurface,
            modifier = Modifier.padding(top = 18.dp)
        )
        Text(
            "Đang nhận diện thực phẩm trong ảnh",
            color = colors.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 6.dp)
        )
        Spacer(Modifier.weight(1f))
    }
}

/** Full-screen put-away moment shown while the draft is committed. It also owns the
 *  confirmation beat, so a fast network still gets a readable animation. */
@Composable
private fun PuttingAwayScreen(items: List<ReceiptItem>, saved: Boolean, error: String?,
    onRetry: () -> Unit, onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val ambient = rememberInfiniteTransition(label = "put-away-ambient")
    val drift by ambient.animateFloat(0f, 1f, infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "put-away-drift")
    val glow by ambient.animateFloat(0.4f, 1f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "put-away-glow")
    val progress = remember { Animatable(0f) }
    LaunchedEffect(items, error) {
        if (error == null) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(1250, easing = FastOutSlowInEasing))
        }
    }
    val door by animateFloatAsState(if (saved) 1f else 0f, tween(650, easing = FastOutSlowInEasing), label = "put-away-door")
    val value = progress.value
    val landed = (value * items.size).roundToInt().coerceIn(0, items.size)
    val stage = when {
        error != null -> "Chưa cất được thực phẩm"
        saved -> "Đã cất gọn vào tủ!"
        value < 0.35f -> "Đang phân loại thực phẩm…"
        value < 0.92f -> "Đang xếp vào tủ…"
        else -> "Đang hoàn tất…"
    }
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(colors.background, colors.primaryContainer.copy(alpha = 0.55f), colors.background)
            )
        )
    ) {
        AmbientBlobs(drift, glow)
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                Modifier.padding(top = 14.dp),
                shape = CircleShape,
                color = colors.surfaceContainerLowest.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, colors.outlineVariant)
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Icon(painterResource(R.drawable.ic_fridge), null, Modifier.size(16.dp), tint = colors.primary)
                    Text(
                        "BƯỚC 3/3  ·  CẤT VÀO TỦ",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.primary
                    )
                }
            }
            Spacer(Modifier.weight(0.22f))
            FridgeScene(items, value, saved, door, glow, Modifier.fillMaxWidth())
            Spacer(Modifier.height(24.dp))
            Text(
                stage,
                style = MaterialTheme.typography.headlineSmall,
                color = colors.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                when {
                    error != null -> "Dữ liệu quét vẫn được giữ nguyên. Thử lại nhé."
                    saved -> "Tủ lạnh đã sẵn sàng cho bữa ăn tiếp theo"
                    else -> "Đang đưa ${items.size} thực phẩm vào tủ lạnh"
                },
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp)
            )
            Spacer(Modifier.height(20.dp))
            if (error == null) {
                LinearProgressIndicator(
                    progress = { if (saved) 1f else value },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = colors.primary,
                    trackColor = colors.surfaceContainerHigh,
                    drawStopIndicator = {}
                )
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        if (saved) "Hoàn tất" else "Đang xử lý",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                    Text(
                        "$landed/${items.size} món",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.primary
                    )
                }
            } else {
                PutAwayError(error, onRetry, onBack)
            }
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun AmbientBlobs(drift: Float, glow: Float) {
    val colors = MaterialTheme.colorScheme
    Canvas(Modifier.fillMaxSize()) {
        val lime = colors.primaryContainer
        val mint = colors.primary
        val top = Offset(size.width * (0.18f + drift * 0.08f), size.height * 0.14f)
        val topRadius = size.minDimension * (0.42f + glow * 0.08f)
        drawCircle(
            Brush.radialGradient(listOf(lime.copy(alpha = 0.35f), Color.Transparent), center = top, radius = topRadius),
            radius = topRadius, center = top
        )
        val side = Offset(size.width * (0.88f - drift * 0.1f), size.height * (0.58f + drift * 0.06f))
        val sideRadius = size.minDimension * 0.5f
        drawCircle(
            Brush.radialGradient(listOf(mint.copy(alpha = 0.14f), Color.Transparent), center = side, radius = sideRadius),
            radius = sideRadius, center = side
        )
    }
}

@Composable
private fun FridgeScene(items: List<ReceiptItem>, progress: Float, saved: Boolean, door: Float, glow: Float,
    modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val shown = remember(items) { items.take(9) }
    val fridgeWidth = 236.dp
    val fridgeHeight = 306.dp
    val chip = 40.dp
    val shape = MaterialTheme.shapes.extraLarge
    BoxWithConstraints(modifier.height(fridgeHeight + 62.dp)) {
        val left = (maxWidth - fridgeWidth) / 2
        Box(
            Modifier.offset(x = left - 18.dp, y = 14.dp).size(fridgeWidth + 36.dp, fridgeHeight)
                .graphicsLayer { alpha = 0.35f + glow * 0.35f }
                .clip(RoundedCornerShape(42.dp))
                .background(
                    Brush.radialGradient(
                        listOf(colors.primaryContainer.copy(alpha = 0.7f), Color.Transparent)
                    )
                )
        )
        FridgeInterior(fridgeWidth, fridgeHeight, Modifier.offset(x = left))
        shown.forEachIndexed { index, item ->
            val column = index % 3
            val row = index / 3
            val stagger = 0.07f
            val span = (1f - stagger * (shown.size - 1).coerceAtLeast(1)).coerceAtLeast(0.35f)
            val eased = FastOutSlowInEasing.transform(((progress - index * stagger) / span).coerceIn(0f, 1f))
            val startX = 0.5f + (column - 1) * 0.22f
            val startY = 1.16f + row * 0.045f
            val targetX = 0.5f + (column - 1) * 0.24f
            val targetY = 0.20f + row * 0.24f
            val controlX = (startX + targetX) / 2f + if (column == 2) 0.16f else -0.14f
            val controlY = minOf(startY, targetY) - 0.28f
            val x = quadratic(startX, controlX, targetX, eased)
            val y = quadratic(startY, controlY, targetY, eased)
            Box(
                Modifier.offset(x = left + fridgeWidth * x - chip * 0.5f, y = fridgeHeight * y - chip * 0.5f)
                    .size(chip).graphicsLayer {
                        alpha = (eased * 4f).coerceIn(0f, 1f)
                        scaleX = 0.55f + eased * 0.45f
                        scaleY = scaleX
                        rotationZ = (1f - eased) * if (column % 2 == 0) -14f else 14f
                    }
            ) { FoodChip(item, chip) }
        }
        Box(
            Modifier.offset(x = left).size(fridgeWidth, fridgeHeight)
                .graphicsLayer {
                    translationX = -size.width * (1f - door)
                    alpha = door
                }
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        listOf(colors.surfaceContainerLowest, colors.primaryContainer.copy(alpha = 0.5f))
                    )
                )
                .border(1.dp, colors.outlineVariant, shape)
        ) {
            Box(
                Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(horizontal = 14.dp)
                    .offset(y = fridgeHeight * 0.25f).height(1.dp).background(colors.outlineVariant)
            )
            Box(
                Modifier.align(Alignment.CenterEnd).offset(x = (-16).dp).width(7.dp).height(fridgeHeight * 0.26f)
                    .clip(CircleShape).background(colors.primary.copy(alpha = 0.35f))
            )
            Box(
                Modifier.align(Alignment.TopCenter).fillMaxWidth().height(fridgeHeight * 0.48f)
                    .background(
                        Brush.verticalGradient(
                            listOf(colors.surfaceContainerLowest.copy(alpha = 0.5f), Color.Transparent)
                        )
                    )
            )
        }
        AnimatedVisibility(
            visible = saved,
            modifier = Modifier.offset(x = left + fridgeWidth - 34.dp, y = (-4).dp),
            enter = fadeIn(tween(220)) + scaleIn(
                initialScale = 0.35f,
                animationSpec = spring(dampingRatio = 0.5f, stiffness = 420f)
            )
        ) {
            Box(
                Modifier.size(52.dp).clip(CircleShape).background(colors.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painterResource(R.drawable.ic_check), "Đã cất thực phẩm vào tủ",
                    Modifier.size(26.dp), tint = colors.onPrimary
                )
            }
        }
    }
}

@Composable
private fun FridgeInterior(width: androidx.compose.ui.unit.Dp, height: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.extraLarge
    Box(
        modifier.size(width, height).clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(colors.primaryContainer.copy(alpha = 0.5f), colors.primaryContainer.copy(alpha = 0.85f))
                )
            )
            .border(1.dp, colors.outlineVariant, shape)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            listOf(0.32f, 0.57f, 0.82f).forEach { fraction ->
                val y = size.height * fraction
                drawLine(colors.outline.copy(alpha = 0.4f), Offset(0f, y), Offset(size.width, y), strokeWidth = 2.dp.toPx())
                drawLine(colors.surfaceContainerLowest.copy(alpha = 0.75f), Offset(0f, y + 2.dp.toPx()),
                    Offset(size.width, y + 2.dp.toPx()), strokeWidth = 1.dp.toPx())
            }
        }
        Box(Modifier.fillMaxWidth().fillMaxHeight(0.22f).background(colors.surfaceContainerLowest.copy(alpha = 0.3f)))
        Box(Modifier.fillMaxWidth().offset(y = height * 0.22f).height(2.dp).background(colors.outline.copy(alpha = 0.4f)))
        Box(
            Modifier.align(Alignment.TopCenter).padding(top = 12.dp).width(width * 0.3f).height(4.dp)
                .clip(CircleShape).background(colors.surfaceContainerLowest.copy(alpha = 0.85f))
        )
    }
}

@Composable
private fun FoodChip(item: ReceiptItem, size: androidx.compose.ui.unit.Dp) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier.size(size).clip(CircleShape).background(colors.surfaceContainerLowest)
            .border(1.dp, colors.outlineVariant, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (item.imageUrl.isNullOrBlank()) {
            Text(
                item.name.trim().take(1).uppercase(),
                style = MaterialTheme.typography.titleSmall,
                color = colors.primary
            )
        } else {
            AsyncImage(
                item.imageUrl, item.name, Modifier.fillMaxSize().padding(4.dp),
                contentScale = ContentScale.Fit,
                error = painterResource(R.drawable.ic_nguyenlieu),
                fallback = painterResource(R.drawable.ic_nguyenlieu)
            )
        }
    }
}

@Composable
private fun PutAwayError(message: String, onRetry: () -> Unit, onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = colors.surfaceContainerLowest,
        border = BorderStroke(1.dp, colors.errorContainer)
    ) {
        Column(
            Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                message,
                color = colors.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Quay lại") }
                Button(
                    onClick = onRetry,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) { Text("Thử lại", style = MaterialTheme.typography.labelLarge) }
            }
        }
    }
}

private fun quadratic(start: Float, control: Float, end: Float, t: Float): Float {
    val inverse = 1f - t
    return inverse * inverse * start + 2f * inverse * t * control + t * t * end
}

@Composable
private fun ReceiptReviewScreen(state: ReceiptReviewState, onBack: () -> Unit, onEdit: (String?) -> Unit,
    onConfirm: () -> Unit, onScanLabel: (String) -> Unit, comingSoon: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedIconButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                colors = androidx.compose.material3.IconButtonDefaults.outlinedIconButtonColors(
                    containerColor = colors.surfaceContainerLowest,
                    contentColor = colors.onSurfaceVariant
                ),
                border = BorderStroke(1.dp, colors.outlineVariant)
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Quay lại")
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Kiểm tra", style = MaterialTheme.typography.headlineMedium, color = colors.onSurface)
                Text(
                    if (state.mode == ScanMode.PRODUCT) "Kiểm tra thông tin trên nhãn" else "Chỉnh sửa nếu hóa đơn đọc chưa đúng",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(3) { index ->
                Box(
                    Modifier.weight(1f).height(7.dp).clip(CircleShape)
                        .background(if (index < 2) colors.primary else colors.surfaceContainerHigh)
                )
            }
        }
        LazyColumn(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 14.dp)
        ) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = colors.surfaceContainerLowest,
                    border = BorderStroke(1.dp, colors.outlineVariant)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(painterResource(R.drawable.ic_note), null, Modifier.size(22.dp), tint = colors.primary)
                        Text(
                            "Đã tìm thấy ${state.items.size} sản phẩm",
                            color = colors.primary,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }
            if (state.merchant != null || state.receiptTotalVnd != null) item {
                Text(
                    listOfNotNull(state.merchant, state.receiptSubtotalVnd?.let { "Tiền hàng: ${formatVnd(it)}" },
                        state.receiptDiscountVnd?.let { "Giảm giá: ${formatVnd(it)}" },
                        state.receiptTotalVnd?.let { "Thanh toán: ${formatVnd(it)}" }).joinToString(" • "),
                    color = colors.primary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (state.reconciliationDeltaVnd != null && state.reconciliationDeltaVnd != 0L) item {
                Text(
                    "Tổng các dòng lệch ${formatVnd(kotlin.math.abs(state.reconciliationDeltaVnd))} so với tổng tiền hàng; hãy kiểm tra giá trên hóa đơn.",
                    color = colors.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            state.editError?.let { message -> item { Text(message, color = colors.error) } }
            if (state.labelScanning) item {
                Text("Đang đọc nhãn sản phẩm…", color = colors.primary, style = MaterialTheme.typography.bodySmall)
            }
            if (state.items.isEmpty()) item {
                Text(
                    "Chưa nhận diện được thực phẩm. Hãy chụp rõ tên và số lượng trên hóa đơn.",
                    Modifier.padding(20.dp),
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            items(state.items, key = { requireNotNull(it.id) }) { item ->
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = colors.surfaceContainerLowest,
                    border = BorderStroke(1.dp, colors.outlineVariant)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            Modifier.fillMaxWidth().padding(bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                item.imageUrl, "Nguyên liệu ${item.name}",
                                Modifier.size(40.dp),
                                contentScale = ContentScale.Fit,
                                error = painterResource(R.drawable.ic_nguyenlieu),
                                fallback = painterResource(R.drawable.ic_nguyenlieu)
                            )
                            Text(
                                item.name,
                                Modifier.weight(1f).padding(horizontal = 12.dp),
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.onSurface
                            )
                            IconButton(onClick = { onEdit(item.id) }) {
                                Icon(
                                    painterResource(R.drawable.ic_edit), "Chỉnh sửa ${item.name}",
                                    Modifier.size(22.dp), tint = colors.onSurfaceVariant
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ReceiptField("Số lượng", item.quantityLabel, Modifier.weight(1f))
                            ReceiptField("Ngày mua", displayDate(item.purchasedAt), Modifier.weight(1f))
                            ReceiptField("Hạn dùng", displayDate(item.expiryDate), Modifier.weight(1f))
                        }
                        item.priceVnd?.let {
                            Text(
                                "Thành tiền: ${formatVnd(it)}",
                                color = colors.primary,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        if (item.matchStatus == "needs_review") Text(
                            "Tên cần kiểm tra: ${item.rawName ?: item.name}",
                            color = colors.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        if (item.expiryDate == null) Text(
                            "HSD chưa xác định",
                            color = colors.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        else if (item.expiryDate < SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
                            Text(
                                "HSD đã qua – kiểm tra trước khi nhập kho",
                                color = colors.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        if (state.mode == ScanMode.RECEIPT) IconButton(
                            onClick = { item.id?.let(onScanLabel) },
                            enabled = !state.labelScanning && !state.saving && item.id != null,
                            modifier = Modifier.align(Alignment.End).size(48.dp)
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_scan), "Quét hạn sử dụng ${item.name}",
                                Modifier.size(20.dp), tint = colors.primary
                            )
                        }
                    }
                }
            }
        }
        OutlinedButton(
            onClick = comingSoon,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(1.dp, colors.outlineVariant)
        ) {
            Icon(Icons.Outlined.Add, null, tint = colors.primary)
            Spacer(Modifier.width(8.dp))
            Text("Thêm thực phẩm bị thiếu", color = colors.primary)
        }
        val allMapped = state.items.all { !it.ingredientId.isNullOrBlank() }
        if (state.items.isNotEmpty() && !allMapped) {
            val unresolvedCount = state.items.count { it.ingredientId.isNullOrBlank() }
            Text(
                "Còn $unresolvedCount món chưa xác định được nguyên liệu. Chạm biểu tượng bút chì trên món đó để chọn trong danh mục rồi lưu.",
                color = colors.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
            )
        }
        Button(
            onClick = onConfirm,
            enabled = state.items.isNotEmpty() && allMapped && !state.saving && !state.committing && !state.saved,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 20.dp).height(52.dp),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
        ) {
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null)
            Spacer(Modifier.width(8.dp))
            Text("Xác nhận ${state.items.size} thực phẩm", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun ReceiptField(label: String, value: String, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier.clip(MaterialTheme.shapes.small)
            .background(colors.surfaceContainerLow)
            .padding(8.dp)
    ) {
        Text(label, color = colors.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurface,
            maxLines = 1,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

internal fun displayDate(date: String?): String = date?.takeIf { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) }
    ?.let { "${it.substring(8, 10)}/${it.substring(5, 7)}/${it.take(4)}" } ?: "Chưa rõ"
private fun formatVnd(value: Long): String = "${"%,d".format(Locale.US, value).replace(',', '.')}đ"

/** Ô ngày chỉ đọc: bấm vào mở lịch, giá trị vẫn lưu dạng yyyy-MM-dd cho backend. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(label: String, value: String, enabled: Boolean, onChange: (String) -> Unit) {
    var picking by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = displayFullDate(value),
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            enabled = enabled,
            singleLine = true,
            trailingIcon = { Icon(Icons.Outlined.DateRange, null) },
            modifier = Modifier.fillMaxWidth()
        )
        Box(Modifier.matchParentSize().clickable(enabled = enabled) { picking = true })
    }
    if (picking) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = parseReceiptDate(value))
        DatePickerDialog(onDismissRequest = { picking = false }, confirmButton = {
            TextButton(onClick = {
                onChange(picker.selectedDateMillis?.let(::formatReceiptDate).orEmpty())
                picking = false
            }) { Text("Chọn") }
        }, dismissButton = {
            Row {
                TextButton(onClick = { onChange(""); picking = false }) { Text("Xoá") }
                TextButton(onClick = { picking = false }) { Text("Hủy") }
            }
        }) { DatePicker(state = picker) }
    }
}
private fun displayFullDate(date: String?): String = date?.takeIf { it.isNotBlank() && validReceiptDate(it) }
    ?.let { "${it.substring(8, 10)}/${it.substring(5, 7)}/${it.take(4)}" } ?: "Chưa chọn"
internal fun parseReceiptDate(value: String): Long? = value.takeIf { it.isNotBlank() && validReceiptDate(it) }
    ?.let { utcDateFormat().parse(it)?.time }
internal fun formatReceiptDate(millis: Long): String = utcDateFormat().format(Date(millis))
private fun utcDateFormat() = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
    isLenient = false
    timeZone = TimeZone.getTimeZone("UTC")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReceiptEditDialog(item: ReceiptItem, saving: Boolean, serverError: String?, onDismiss: () -> Unit, onSave: (ReceiptItem) -> Unit) {
    var name by rememberSaveable(item.id, item.expiryDate) { mutableStateOf(item.name) }
    var chosenCandidate by remember(item.id) { mutableStateOf<String?>(null) }
    var candidatesExpanded by remember { mutableStateOf(false) }
    var quantity by rememberSaveable(item.id, item.quantity) { mutableStateOf(item.quantity?.toString().orEmpty()) }
    var unit by rememberSaveable(item.id, item.unit) { mutableStateOf(item.unit.orEmpty()) }
    var purchased by rememberSaveable(item.id, item.purchasedAt) { mutableStateOf(item.purchasedAt.orEmpty()) }
    var expiry by rememberSaveable(item.id, item.expiryDate) { mutableStateOf(item.expiryDate.orEmpty()) }
    var price by rememberSaveable(item.id, item.priceVnd) { mutableStateOf(item.priceVnd?.toString().orEmpty()) }
    var unitPrice by rememberSaveable(item.id, item.unitPriceVnd) { mutableStateOf(item.unitPriceVnd?.toString().orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(item.name) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it; chosenCandidate = null }, label = { Text("Tên sản phẩm") }, enabled = !saving, singleLine = true)
                if (item.candidates.isNotEmpty()) Box {
                    TextButton(onClick = { candidatesExpanded = true }) { Text("Chọn từ danh mục gợi ý") }
                    DropdownMenu(expanded = candidatesExpanded, onDismissRequest = { candidatesExpanded = false }) {
                        item.candidates.forEach { candidate ->
                            DropdownMenuItem(text = { Text(candidate.name) }, onClick = {
                                name = candidate.name; chosenCandidate = candidate.id; candidatesExpanded = false
                            })
                        }
                    }
                }
                OutlinedTextField(quantity, { quantity = it }, label = { Text("Số lượng") }, enabled = !saving,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                OutlinedTextField(unit, { unit = it }, label = { Text("Đơn vị") }, enabled = !saving, singleLine = true)
                DateField("Ngày mua", purchased, !saving) { purchased = it }
                DateField("Hạn dùng", expiry, !saving) { expiry = it }
                OutlinedTextField(unitPrice, { unitPrice = it }, label = { Text("Đơn giá (đ)") }, enabled = !saving,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(price, { price = it }, label = { Text("Thành tiền (đ)") }, enabled = !saving,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                (error ?: serverError)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }, confirmButton = {
            TextButton(enabled = !saving, onClick = {
                val number = quantity.trim().replace(',', '.').toDoubleOrNull()
                error = when {
                    name.isBlank() -> "Tên sản phẩm không được để trống."
                    item.ingredientId.isNullOrBlank() && chosenCandidate.isNullOrBlank() -> "Hãy chọn nguyên liệu từ danh mục để nhập kho."
                    quantity.isNotBlank() && (number == null || !number.isFinite() || number <= 0) -> "Số lượng phải lớn hơn 0."
                    price.isNotBlank() && (price.toLongOrNull() == null || price.toLong() < 0) -> "Thành tiền phải là số không âm."
                    unitPrice.isNotBlank() && (unitPrice.toLongOrNull() == null || unitPrice.toLong() < 0) -> "Đơn giá phải là số không âm."
                    !validReceiptDate(purchased) || !validReceiptDate(expiry) -> "Ngày không hợp lệ."
                    purchased.isNotBlank() && expiry.isNotBlank() && expiry < purchased -> "HSD trước ngày mua; kiểm tra xem nhãn có đúng sản phẩm không."
                    else -> null
                }
                if (error == null) onSave(item.copy(name = name.trim(), ingredientId = chosenCandidate ?: if (name.trim() == item.name) item.ingredientId else null,
                    quantity = number, unit = unit.trim().ifBlank { null }, purchasedAt = purchased.ifBlank { null },
                    expiryDate = expiry.ifBlank { null }, priceVnd = price.toLongOrNull(),
                    unitPriceVnd = unitPrice.toLongOrNull(), matchStatus = "confirmed"))
            }) { Text(if (saving) "Đang lưu…" else "Lưu") }
        }, dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Hủy") } })
}
internal fun validReceiptDate(value: String): Boolean = value.isBlank() ||
    (value.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) && runCatching {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(value) != null
    }.getOrDefault(false))

@Preview(showBackground = true, widthDp = 400, heightDp = 840)
@Composable
private fun ReceiptReviewPreview() {
    FridgeWiseTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ReceiptReviewScreen(ReceiptReviewState(loading = false, items = listOf(
                ReceiptItem("Cà chua bi", "i1", 500.0, "g", "2026-10-01", "2026-10-06", "1"),
                ReceiptItem("Thịt heo ba rọi", "i2", 350.0, "g", "2026-10-01", "2026-10-03", "2"),
                ReceiptItem("Cải thìa", "i3", 2.0, "bó", "2026-10-01", "2026-10-02", "3")
            )), {}, {}, {}, {}) {}
        }
    }
}
