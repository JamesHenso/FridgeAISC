package com.production.fridgewise.ui.scan

import androidx.compose.animation.SizeTransform
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.production.fridgewise.R

private val PlacementGreen = Color(0xFF10BD98)
private val PlacementMuted = Color(0xFF858B91)

@Composable
internal fun ReceiptPlacementScreen(
    state: ReceiptReviewState,
    onBack: () -> Unit,
    onPlace: (String, String) -> Unit,
    onUndo: () -> Unit,
    onFinish: () -> Unit
) {
    val next = state.placement.next(state.items)
    val complete = state.placement.isComplete(state.items)
    val locked = state.committing || state.locationsSaved || state.completed
    var freezerBounds by remember { mutableStateOf(Rect.Zero) }
    var fridgeBounds by remember { mutableStateOf(Rect.Zero) }
    var cardBounds by remember { mutableStateOf(Rect.Zero) }
    var dragOffset by remember(next?.id) { mutableStateOf(Offset.Zero) }
    var dragPosition by remember(next?.id) { mutableStateOf<Offset?>(null) }
    val currentOnPlace by rememberUpdatedState(onPlace)
    fun locationAt(point: Offset?): String? = when {
        point == null -> null
        freezerBounds.contains(point) -> "freezer"
        fridgeBounds.contains(point) -> "fridge"
        else -> null
    }
    val hovered = locationAt(dragPosition)
    val scale by animateFloatAsState(if (dragPosition != null) 1.04f else 1f, label = "drag-scale")

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedIconButton(onClick = onBack, enabled = !state.committing && !state.completed, shape = CircleShape, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, if (state.locationsSaved) "Quay lại Inventory" else "Quay lại kiểm tra")
            }
            Column(Modifier.padding(start = 14.dp)) {
                Text("Xác nhận", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("Chọn nơi bảo quản thực phẩm", fontSize = 13.sp, color = PlacementMuted)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(3) {
                Surface(Modifier.weight(1f).height(7.dp), shape = CircleShape, color = PlacementGreen) {}
            }
        }
        Text("Đã xếp ${state.placement.locations.size}/${state.items.size} thực phẩm",
            Modifier.padding(top = 18.dp, bottom = 6.dp), color = Color(0xFF087A62), fontWeight = FontWeight.SemiBold)
        Text(if (complete) "Mọi thứ đã sẵn sàng!" else "Kéo thực phẩm vào một ngăn bên dưới",
            fontSize = 13.sp, color = PlacementMuted, modifier = Modifier.padding(bottom = 20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("freezer", "fridge").forEach { location ->
                val names = state.items.filter { state.placement.locations[it.id] == location }.map { it.name }
                StorageDropZone(
                    label = if (location == "freezer") "Freezer" else "Fridge",
                    icon = if (location == "freezer") R.drawable.ic_snow else R.drawable.ic_fridge,
                    color = if (location == "freezer") Color(0xFF429CE3) else PlacementGreen,
                    names = names, highlighted = hovered == location,
                    enabled = !locked && next != null,
                    onClick = { next?.id?.let { onPlace(it, location) } },
                    modifier = Modifier.weight(1f).onGloballyPositioned {
                        if (location == "freezer") freezerBounds = it.boundsInRoot() else fridgeBounds = it.boundsInRoot()
                    }
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        AnimatedContent(targetState = next, contentKey = { it?.id },
            transitionSpec = {
                ((fadeIn(tween(230)) + slideInHorizontally(tween(230)) { it / 3 })
                    .togetherWith(fadeOut(tween(120)))).using(SizeTransform(clip = false))
            },
            modifier = Modifier.fillMaxWidth().zIndex(1f), label = "next-ingredient") { item ->
            if (item != null) {
                Surface(shape = RoundedCornerShape(24.dp), color = Color.White,
                    shadowElevation = if (dragPosition != null) 12.dp else 2.dp,
                    modifier = Modifier.fillMaxWidth()
                        .onGloballyPositioned { cardBounds = it.boundsInRoot() }
                        .graphicsLayer {
                            translationX = dragOffset.x; translationY = dragOffset.y
                            scaleX = scale; scaleY = scale
                            rotationZ = (dragOffset.x / 35f).coerceIn(-6f, 6f)
                        }
                        .semantics {
                            customActions = listOf("Freezer", "Fridge").map { title ->
                                CustomAccessibilityAction("Đặt vào $title") {
                                    if (!locked && item.id != null) { currentOnPlace(item.id, title.lowercase()); true } else false
                                }
                            }
                        }
                        .pointerInput(item.id, locked, freezerBounds, fridgeBounds) {
                            if (!locked) detectDragGestures(
                                onDragStart = { point -> dragPosition = cardBounds.topLeft + point },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragOffset += amount
                                    dragPosition = dragPosition?.plus(amount)
                                },
                                onDragCancel = { dragOffset = Offset.Zero; dragPosition = null },
                                onDragEnd = {
                                    val location = locationAt(dragPosition)
                                    dragOffset = Offset.Zero; dragPosition = null
                                    if (location != null && item.id != null) currentOnPlace(item.id, location)
                                }
                            )
                        }) {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(painterResource(R.drawable.thit_heo), null, Modifier.size(58.dp))
                        Text(item.name, fontWeight = FontWeight.SemiBold, fontSize = 20.sp,
                            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                        Text(item.quantityLabel, color = PlacementMuted, modifier = Modifier.padding(top = 4.dp))
                        Text("Kéo thả hoặc chạm vào ngăn để chọn", fontSize = 11.sp, color = PlacementMuted,
                            modifier = Modifier.padding(top = 12.dp))
                    }
                }
            } else {
                Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(shape = CircleShape, color = PlacementGreen.copy(alpha = 0.12f)) {
                        Icon(painterResource(R.drawable.ic_check), null, Modifier.padding(16.dp).size(36.dp), tint = PlacementGreen)
                    }
                    Text("Đã phân loại tất cả thực phẩm", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 14.dp))
                }
            }
        }
        TextButton(onClick = onUndo, enabled = state.placement.locations.isNotEmpty() && !locked,
            modifier = Modifier.padding(top = 8.dp)) { Text("Hoàn tác lần xếp cuối") }
        state.commitError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 10.dp))
            if (state.locationsSaved && !state.committing) {
                TextButton(onClick = onBack) { Text("Về Inventory để kiểm tra") }
            }
        }
        if (complete) {
            Button(onClick = onFinish, enabled = !state.committing && !state.completed,
                shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = PlacementGreen),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp).height(54.dp)) {
                if (state.committing) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                else Icon(painterResource(R.drawable.ic_check), null, Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(if (state.committing) "Đang thêm…" else if (state.commitError != null) "Thử lại" else "Thêm vào inventory",
                    fontWeight = FontWeight.SemiBold)
            }
        } else Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StorageDropZone(label: String, icon: Int, color: Color, names: List<String>, highlighted: Boolean,
    enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val background by animateColorAsState(if (highlighted) color.copy(alpha = 0.22f) else color.copy(alpha = 0.07f), label = "drop-highlight")
    Surface(onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 208.dp),
        shape = RoundedCornerShape(24.dp), color = background,
        border = BorderStroke(if (highlighted) 2.dp else 1.dp, color.copy(alpha = if (highlighted) 1f else 0.3f))) {
        Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(painterResource(icon), null, Modifier.size(42.dp), tint = color)
            Text(label, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
            Text("${names.size} thực phẩm", color = color, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, bottom = 10.dp))
            if (names.isEmpty()) Text(if (highlighted) "Thả vào đây" else "Chưa có thực phẩm", color = PlacementMuted, fontSize = 11.sp)
            names.takeLast(2).forEach { Text(it, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            if (names.size > 2) Text("+${names.size - 2} thực phẩm khác", fontSize = 11.sp, color = PlacementMuted)
        }
    }
}
