package com.production.fridgewise.ui.scan

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

private val ReviewGreen = Color(0xFF10BD98)
private val ReviewDarkGreen = Color(0xFF087A62)
private val ReviewBackground = Color(0xFFF3F6F3)
private val ReviewMuted = Color(0xFF858B91)

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
        val mime = resolver.getType(uri) ?: if (uri.path?.endsWith(".png", ignoreCase = true) == true) "image/png" else "image/jpeg"
        model.load(mode, mime) { resolver.openInputStream(uri) ?: error("Không mở được ảnh.") }
    }
    LaunchedEffect(model) { load() }
    LaunchedEffect(labelPhotoUri, labelTargetId) {
        if (labelPhotoUri != null && labelTargetId != null) {
            val labelUri = Uri.parse(labelPhotoUri)
            val mime = resolver.getType(labelUri) ?: if (labelUri.path?.endsWith(".png", true) == true) "image/png" else "image/jpeg"
            model.scanLabel(labelTargetId, mime) { resolver.openInputStream(labelUri) ?: error("Không mở được ảnh nhãn.") }
            onLabelConsumed()
        }
    }
    LaunchedEffect(state.completed) { if (state.completed) onComplete() }
    val back: () -> Unit = { if (!state.saving && !state.committing) { model.cancelPending(); onBack() } }
    BackHandler(onBack = back)
    Scaffold(containerColor = ReviewBackground, contentColor = Color(0xFF191C28),
        snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        AnimatedContent(targetState = when { state.loading -> "loading"; state.error != null -> "error"; else -> "review" },
            transitionSpec = { (fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 15 }).togetherWith(fadeOut(tween(180))) },
            modifier = Modifier.fillMaxSize().padding(padding), label = "receipt-processing") { page ->
            when (page) {
                "loading" -> ReceiptLoading(photoUri, state.stage, back)
                "error" -> Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    AsyncImage(photoUri, null, Modifier.size(160.dp).clip(RoundedCornerShape(18.dp)), contentScale = ContentScale.Fit)
                    Spacer(Modifier.height(20.dp))
                    Text(state.error.orEmpty(), textAlign = TextAlign.Center)
                    Button(onClick = { load() }, modifier = Modifier.padding(top = 16.dp)) { Text("Thử lại") }
                    TextButton(onClick = back) { Text("Chụp lại hóa đơn") }
                }
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

@Composable
private fun ReceiptLoading(photoUri: String, stage: String, onBack: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "receipt-loading")
    val tilt by transition.animateFloat(-2f, 2f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "receipt-tilt")
    val line by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse), label = "receipt-reading")
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Quay lại máy quét")
        }
        Spacer(Modifier.weight(1f))
        Box(Modifier.widthIn(max = 260.dp).fillMaxWidth().aspectRatio(0.78f)
            .graphicsLayer { rotationZ = tilt }.clip(RoundedCornerShape(22.dp)).background(Color.White)) {
            AsyncImage(photoUri, "Hóa đơn đang xử lý", Modifier.fillMaxSize().padding(12.dp), contentScale = ContentScale.Fit)
            BoxWithConstraints(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().offset(y = maxHeight * line).height(3.dp).background(ReviewGreen))
            }
        }
        Spacer(Modifier.height(32.dp))
        CircularProgressIndicator(Modifier.size(30.dp), color = ReviewGreen, strokeWidth = 3.dp)
        Text(stage, fontSize = 21.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 18.dp))
        Text("Đang nhận diện thực phẩm trong ảnh", color = ReviewMuted, modifier = Modifier.padding(top = 7.dp))
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun ReceiptReviewScreen(state: ReceiptReviewState, onBack: () -> Unit, onEdit: (String?) -> Unit,
    onConfirm: () -> Unit, onScanLabel: (String) -> Unit, comingSoon: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 26.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedIconButton(onClick = onBack, modifier = Modifier.size(48.dp), shape = CircleShape,
                colors = IconButtonDefaults.outlinedIconButtonColors(containerColor = Color.White)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Quay lại", tint = ReviewMuted)
            }
            Column {
                Text("Kiểm tra", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(if (state.mode == ScanMode.PRODUCT) "Kiểm tra thông tin trên nhãn" else "Chỉnh sửa nếu hóa đơn đọc chưa đúng", fontSize = 13.sp, color = ReviewMuted)
            }
        }
        Row(Modifier.fillMaxWidth().padding(bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(3) { index -> Box(Modifier.weight(1f).height(7.dp).clip(CircleShape)
                .background(if (index < 2) ReviewGreen else Color(0xFFDDE3E0))) }
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 14.dp)) {
            item {
                Surface(shape = RoundedCornerShape(16.dp), color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E8E4))) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(painterResource(R.drawable.ic_note), null, Modifier.size(22.dp), tint = ReviewDarkGreen)
                        Text("Đã tìm thấy ${state.items.size} sản phẩm", color = ReviewDarkGreen, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                }
            }
            if (state.merchant != null || state.receiptTotalVnd != null) item {
                Text(listOfNotNull(state.merchant, state.receiptSubtotalVnd?.let { "Tiền hàng: ${formatVnd(it)}" },
                    state.receiptDiscountVnd?.let { "Giảm giá: ${formatVnd(it)}" },
                    state.receiptTotalVnd?.let { "Thanh toán: ${formatVnd(it)}" }).joinToString(" • "),
                    color = ReviewDarkGreen, fontSize = 13.sp)
            }
            if (state.reconciliationDeltaVnd != null && state.reconciliationDeltaVnd != 0L) item {
                Text("Tổng các dòng lệch ${formatVnd(kotlin.math.abs(state.reconciliationDeltaVnd))} so với tổng tiền hàng; hãy kiểm tra giá trên hóa đơn.",
                    color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
            state.editError?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
            if (state.labelScanning) item { Text("Đang đọc nhãn sản phẩm…", color = ReviewDarkGreen) }
            if (state.items.isEmpty()) item {
                Text("Chưa nhận diện được thực phẩm. Hãy chụp rõ tên và số lượng trên hóa đơn.",
                    Modifier.padding(20.dp), color = ReviewMuted, textAlign = TextAlign.Center)
            }
            items(state.items, key = { requireNotNull(it.id) }) { item ->
                Surface(shape = RoundedCornerShape(22.dp), color = Color.White, shadowElevation = 1.dp) {
                    Column(Modifier.padding(14.dp)) {
                        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Image(painterResource(R.drawable.ic_nguyenlieu), "Nguyên liệu ${item.name}",
                                Modifier.size(36.dp), contentScale = ContentScale.Fit)
                            Text(item.name, Modifier.weight(1f).padding(horizontal = 12.dp), fontSize = 17.sp, fontWeight = FontWeight.Medium)
                            IconButton(onClick = { onEdit(item.id) }) {
                                Icon(painterResource(R.drawable.ic_edit), "Chỉnh sửa ${item.name}", Modifier.size(22.dp), tint = ReviewMuted)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            ReceiptField("Số lượng", item.quantityLabel, Modifier.weight(1f))
                            ReceiptField("Ngày mua", displayDate(item.purchasedAt), Modifier.weight(1f))
                            ReceiptField("Hạn dùng", displayDate(item.expiryDate), Modifier.weight(1f))
                        }
                        item.priceVnd?.let { Text("Thành tiền: ${formatVnd(it)}", color = ReviewDarkGreen, modifier = Modifier.padding(top = 8.dp)) }
                        if (item.matchStatus == "needs_review") Text("Tên cần kiểm tra: ${item.rawName ?: item.name}",
                            color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                        if (item.expiryDate == null) Text("HSD chưa xác định", color = ReviewMuted, fontSize = 12.sp,
                            modifier = Modifier.padding(top = 6.dp))
                        else if (item.expiryDate < SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date()))
                            Text("HSD đã qua – kiểm tra trước khi nhập kho", color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                        if (state.mode == ScanMode.RECEIPT) IconButton(onClick = { item.id?.let(onScanLabel) },
                            enabled = !state.labelScanning && !state.saving && item.id != null,
                            modifier = Modifier.align(Alignment.End).size(40.dp)) {
                            Icon(painterResource(R.drawable.ic_scan), "Quét hạn sử dụng ${item.name}",
                                Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
        OutlinedButton(onClick = comingSoon, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC4D1CB))) {
            Icon(Icons.Outlined.Add, null, tint = ReviewGreen)
            Spacer(Modifier.width(8.dp))
            Text("Thêm thực phẩm bị thiếu", color = ReviewDarkGreen)
        }
        val allMapped = state.items.all { !it.ingredientId.isNullOrBlank() }
        if (state.items.isNotEmpty() && !allMapped) {
            val unresolvedCount = state.items.count { it.ingredientId.isNullOrBlank() }
            Text("Còn $unresolvedCount món chưa xác định được nguyên liệu. Chạm biểu tượng bút chì trên món đó để chọn trong danh mục rồi lưu.",
                color = MaterialTheme.colorScheme.error, fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
        }
        Button(onClick = onConfirm, enabled = state.items.isNotEmpty() && allMapped && !state.saving, modifier = Modifier.fillMaxWidth().padding(top = 9.dp, bottom = 18.dp).height(52.dp),
            shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = ReviewGreen)) {
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null)
            Spacer(Modifier.width(9.dp))
            Text("Xác nhận ${state.items.size} thực phẩm", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ReceiptField(label: String, value: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(ReviewBackground).padding(10.dp)) {
        Text(label, color = ReviewMuted, fontSize = 11.sp)
        Text(value, fontSize = 15.sp, modifier = Modifier.padding(top = 4.dp))
    }
}
private fun displayDate(date: String?): String = date?.takeIf { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) }
    ?.let { "${it.substring(8, 10)}/${it.substring(5, 7)}" } ?: "Chưa rõ"
private fun formatVnd(value: Long): String = "${"%,d".format(Locale.US, value).replace(',', '.')}đ"

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
    AlertDialog(onDismissRequest = { if (!saving) onDismiss() }, title = { Text(item.name) },
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
                OutlinedTextField(purchased, { purchased = it }, label = { Text("Ngày mua (YYYY-MM-DD)") }, enabled = !saving, singleLine = true)
                OutlinedTextField(expiry, { expiry = it }, label = { Text("Hạn dùng (YYYY-MM-DD)") }, enabled = !saving, singleLine = true)
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
                    !validReceiptDate(purchased) || !validReceiptDate(expiry) -> "Ngày phải đúng định dạng YYYY-MM-DD."
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
    MaterialTheme {
        Surface(color = ReviewBackground) {
            ReceiptReviewScreen(ReceiptReviewState(loading = false, items = listOf(
                ReceiptItem("Cà chua bi", "i1", 500.0, "g", "2026-10-01", "2026-10-06", "1"),
                ReceiptItem("Thịt heo ba rọi", "i2", 350.0, "g", "2026-10-01", "2026-10-03", "2"),
                ReceiptItem("Cải thìa", "i3", 2.0, "bó", "2026-10-01", "2026-10-02", "3")
            )), {}, {}, {}, {}) {}
        }
    }
}
