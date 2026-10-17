package com.production.fridgewise.ui.recipes

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil3.compose.AsyncImage
import com.production.fridgewise.R
import com.production.fridgewise.domain.model.RecipeDetail
import com.production.fridgewise.domain.model.RecipeIngredient
import kotlinx.coroutines.launch

private val DetailGreen = Color(0xFF0BB887)
private val DetailMuted = Color(0xFF77858E)
private val DetailBackground = Color(0xFFF0F8F4)

@Composable
internal fun RecipeDetailRoute(token: String, recipeId: String, fridgeId: String?, favorite: Boolean,
    favoriteReady: Boolean, favoriteUpdating: Boolean, onFavorite: () -> Unit,
    onBack: () -> Unit, onSignIn: () -> Unit, snackbar: SnackbarHostState) {
    val model: RecipeDetailViewModel = viewModel(key = "recipe-detail-${token.hashCode()}-$fridgeId-$recipeId",
        factory = viewModelFactory { initializer { RecipeDetailViewModel(token, recipeId, fridgeId) } })
    val state by model.uiState.collectAsState()
    LaunchedEffect(model) { model.refresh() }
    Scaffold(containerColor = DetailBackground, contentColor = Color(0xFF101D29),
        snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        if (state.detail != null) {
            RecipeDetailContent(state.detail!!, favorite, favoriteReady && !favoriteUpdating,
                favoriteUpdating, onFavorite, onBack, snackbar, Modifier.padding(padding))
        } else Column(Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Quay lại Recipes") }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (state.loading) CircularProgressIndicator(color = DetailGreen)
                else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (state.needsSignIn) "Phiên đăng nhập đã hết hạn." else state.error.orEmpty(), textAlign = TextAlign.Center)
                    TextButton(onClick = if (state.needsSignIn) onSignIn else model::refresh) {
                        Text(if (state.needsSignIn) "Đăng nhập lại" else "Thử lại")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecipeDetailContent(detail: RecipeDetail, favorite: Boolean, favoriteEnabled: Boolean,
    favoriteUpdating: Boolean, onFavorite: () -> Unit, onBack: () -> Unit,
    snackbar: SnackbarHostState, modifier: Modifier) {
    var sheet by rememberSaveable(detail.recipe.id) { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledIconButton(onClick = onBack, colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White)) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Quay lại Recipes")
                }
                Text(detail.recipe.name, Modifier.weight(1f), fontSize = 21.sp, lineHeight = 27.sp, fontWeight = FontWeight.Bold)
                FilledIconToggleButton(checked = favorite, onCheckedChange = { onFavorite() }, enabled = favoriteEnabled,
                    colors = IconButtonDefaults.filledIconToggleButtonColors(containerColor = Color.White,
                        checkedContainerColor = Color.White, checkedContentColor = Color(0xFFFF1236))) {
                    if (favoriteUpdating) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Icon(painterResource(R.drawable.ic_favorite), if (favorite) "Bỏ yêu thích" else "Thêm yêu thích", Modifier.size(24.dp))
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xFFE1EBE8)).padding(3.dp)) {
                listOf("Have", "Need").forEach { label ->
                    Surface(onClick = { sheet = label }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(15.dp),
                        color = if ((sheet ?: "Have") == label) Color.White else Color.Transparent) {
                        Text(label, Modifier.padding(vertical = 12.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        item {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(20.dp)).background(Color(0xFFDDE8E2)),
                contentAlignment = Alignment.Center) {
                AsyncImage(detail.recipe.imageUrl, detail.recipe.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.auth_grocery_illustration), fallback = painterResource(R.drawable.auth_grocery_illustration))
                if (detail.videoUrl != null) {
                    FilledIconButton(onClick = {
                        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(detail.videoUrl))) }
                        catch (_: ActivityNotFoundException) { scope.launch { snackbar.showSnackbar("Không tìm thấy ứng dụng mở video.") } }
                    }, modifier = Modifier.size(64.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White)) {
                        Icon(Icons.Filled.PlayArrow, "Phát video hướng dẫn", Modifier.size(34.dp))
                    }
                    Surface(Modifier.align(Alignment.BottomEnd).padding(12.dp), shape = RoundedCornerShape(8.dp), color = Color(0xAA101D29)) {
                        Text("Video hướng dẫn", Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = Color.White, fontSize = 11.sp)
                    }
                } else Surface(Modifier.align(Alignment.BottomCenter).padding(10.dp), shape = RoundedCornerShape(8.dp), color = Color(0xAA101D29)) {
                    Text("Chưa có video hướng dẫn", Modifier.padding(8.dp), fontSize = 11.sp, color = Color.White)
                }
            }
        }
        item {
            Surface(shape = RoundedCornerShape(18.dp), color = Color.White) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    DetailMetric(R.drawable.ic_servings, "Serving", detail.recipe.servings?.let { "$it người" } ?: "—", Modifier.weight(1f))
                    DetailMetric(R.drawable.ic_time, "Time", detail.recipe.minutes?.let { "$it phút" } ?: "—", Modifier.weight(1f))
                    DetailMetric(R.drawable.ic_difficulty, "Difficulty", detail.recipe.difficulty ?: "—", Modifier.weight(1f))
                }
            }
        }
        item {
            DetailSection("Nguyên liệu", R.drawable.ic_nguyenlieu) {
                if (detail.ingredients.isEmpty()) Text("Chưa có danh sách nguyên liệu.", color = DetailMuted)
                detail.ingredients.forEach { BulletLine("${it.name} — ${it.amount}") }
            }
        }
        item { DetailSection("Sơ chế", R.drawable.ic_soche) { InstructionLines(detail.preparation) } }
        item { DetailSection("Thực hiện", R.drawable.ic_thuchien) { InstructionLines(detail.instructions, numbered = true) } }
        item { DetailSection("Cách dùng", R.drawable.ic_cachdung) { InstructionLines(detail.servingText, bullets = false) } }
    }
    sheet?.let { selected ->
        ModalBottomSheet(onDismissRequest = { sheet = null }, containerColor = Color.White,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(selected, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text(if (selected == "Have") "Nguyên liệu bạn đang có" else "Nguyên liệu còn thiếu hoặc chưa đủ", color = DetailMuted, fontSize = 14.sp)
                    Text("Định lượng bên dưới theo công thức", color = DetailMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                }
                IconButton(onClick = { sheet = null }) { Icon(Icons.Outlined.Close, "Đóng") }
            }
            val ingredients = if (selected == "Have") detail.have else detail.need
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 480.dp), contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)) {
                if (ingredients == null) item { Text("Chọn tủ lạnh trong Inventory để đối chiếu nguyên liệu.", color = DetailMuted) }
                else if (ingredients.isEmpty()) item {
                    Text(if (selected == "Have") "Chưa có nguyên liệu phù hợp trong tủ." else "Bạn đã có đủ các nguyên liệu được đối chiếu.", color = DetailMuted)
                } else itemsIndexed(ingredients) { _, ingredient -> IngredientSheetRow(ingredient, selected == "Have") }
            }
        }
    }
}

@Composable
private fun DetailMetric(icon: Int, label: String, value: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Icon(painterResource(icon), null, Modifier.size(24.dp), tint = DetailGreen)
        Text(label, fontSize = 11.sp, color = DetailMuted)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }
}

@Composable
private fun DetailSection(title: String, icon: Int, content: @Composable ColumnScope.() -> Unit) {
    var expanded by rememberSaveable(title) { mutableStateOf(true) }
    Surface(shape = RoundedCornerShape(18.dp), color = Color.White, modifier = Modifier.fillMaxWidth().animateContentSize()) {
        Column {
            Row(Modifier.fillMaxWidth().clickable(onClickLabel = if (expanded) "Thu gọn" else "Mở rộng") { expanded = !expanded }.padding(15.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(painterResource(icon), null, Modifier.size(23.dp), tint = DetailGreen)
                Text(title, Modifier.weight(1f), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, tint = DetailMuted)
            }
            AnimatedVisibility(expanded) { Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp), content = content) }
        }
    }
}

@Composable
private fun BulletLine(text: String, number: Int? = null, bullets: Boolean = true) {
    Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.Top) {
        if (number != null) Surface(shape = CircleShape, color = Color(0xFFE9F2EE)) {
            Text(number.toString(), Modifier.widthIn(min = 24.dp).padding(3.dp), textAlign = TextAlign.Center, fontSize = 13.sp)
        } else if (bullets) Text("•", color = DetailGreen, fontWeight = FontWeight.Bold)
        Text(text, fontSize = 14.sp, lineHeight = 21.sp)
    }
}

@Composable
private fun InstructionLines(raw: String?, numbered: Boolean = false, bullets: Boolean = true) {
    val lines = remember(raw) {
        val plain = if (raw?.contains(Regex("</?[A-Za-z][^>]*>")) == true)
            HtmlCompat.fromHtml(raw, HtmlCompat.FROM_HTML_MODE_LEGACY).toString() else raw.orEmpty()
        plain.lines().map { it.trim() }.filter { it.isNotBlank() }
    }
    if (lines.isEmpty()) Text("Chưa có hướng dẫn.", color = DetailMuted, fontSize = 14.sp)
    lines.forEachIndexed { index, line ->
        BulletLine(if (numbered) line.replace(Regex("^\\d+[.)]\\s*"), "") else line,
            if (numbered) index + 1 else null, bullets)
    }
}

@Composable
private fun IngredientSheetRow(item: RecipeIngredient, have: Boolean) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE1E8E5))) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(painterResource(R.drawable.use_soon), null, Modifier.size(46.dp))
            Column(Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(item.amount, color = DetailMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
            }
            if (have) Surface(shape = CircleShape, color = DetailGreen) {
                Icon(painterResource(R.drawable.ic_check), "Đang có", Modifier.padding(5.dp).size(18.dp), tint = Color.White)
            } else Text("Cần thêm", color = DetailGreen, fontSize = 11.sp)
        }
    }
}
