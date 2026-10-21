package com.production.fridgewise.ui.recipes

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil3.compose.AsyncImage
import com.production.fridgewise.R
import com.production.fridgewise.domain.model.RecipeDetail
import com.production.fridgewise.domain.model.RecipeIngredient
import com.production.fridgewise.ui.components.RefreshableContent
import kotlinx.coroutines.launch

@Composable
internal fun RecipeDetailRoute(token: String, recipeId: String, fridgeId: String?, favorite: Boolean,
    favoriteReady: Boolean, favoriteUpdating: Boolean, onFavorite: () -> Unit,
    onBack: () -> Unit, onSignIn: () -> Unit, snackbar: SnackbarHostState) {
    val model: RecipeDetailViewModel = viewModel(key = "recipe-detail-${token.hashCode()}-$fridgeId-$recipeId",
        factory = viewModelFactory { initializer { RecipeDetailViewModel(token, recipeId, fridgeId) } })
    val state by model.uiState.collectAsState()
    LaunchedEffect(model) { model.refresh() }
    LaunchedEffect(state.error) {
        if (state.detail != null && state.error != null) snackbar.showSnackbar(state.error!!)
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        if (state.detail != null) {
            RefreshableContent(state.loading, model::refresh, Modifier.padding(padding)) {
                RecipeDetailContent(state.detail!!, favorite, favoriteReady && !favoriteUpdating,
                    favoriteUpdating, onFavorite, onBack, snackbar, Modifier)
            }
        } else Column(Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Quay lại Recipes") }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (state.loading) CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
    val colors = MaterialTheme.colorScheme
    var sheet by rememberSaveable(detail.recipe.id) { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val openExternal: () -> Unit = {
        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(detail.videoUrl))) }
        catch (_: ActivityNotFoundException) { scope.launch { snackbar.showSnackbar("Không tìm thấy ứng dụng mở video.") } }
    }
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    onClick = onBack,
                    shape = CircleShape,
                    color = colors.surfaceContainerLowest,
                    border = BorderStroke(1.dp, colors.outlineVariant)
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack, "Quay lại Recipes",
                        Modifier.padding(12.dp).size(20.dp), tint = colors.onSurface
                    )
                }
                Text(
                    detail.recipe.name,
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    onClick = { if (favoriteEnabled) onFavorite() },
                    shape = CircleShape,
                    color = colors.surfaceContainerLowest,
                    border = BorderStroke(1.dp, colors.outlineVariant)
                ) {
                    if (favoriteUpdating) CircularProgressIndicator(
                        Modifier.padding(12.dp).size(20.dp),
                        strokeWidth = 2.dp,
                        color = colors.primary
                    )
                    else Icon(
                        painterResource(R.drawable.ic_favorite),
                        if (favorite) "Bỏ yêu thích" else "Thêm yêu thích",
                        Modifier.padding(12.dp).size(20.dp),
                        tint = if (favorite) colors.error else colors.onSurfaceVariant
                    )
                }
            }
        }
        item { RecipeHero(detail, openExternal) }
        item { MetricStrip(detail) }
        item {
            DetailSection("Nguyên liệu", R.drawable.ic_nguyenlieu) {
                IngredientModeSwitch(sheet ?: "Have", onSelect = { sheet = it })
                if (detail.ingredients.isEmpty()) Text(
                    "Chưa có danh sách nguyên liệu.",
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                detail.ingredients.forEach { IngredientLine(it) }
            }
        }
        item { DetailSection("Sơ chế", R.drawable.ic_soche) { StepList(RecipeText.stepLines(detail.preparation)) } }
        item { DetailSection("Thực hiện", R.drawable.ic_thuchien) {
            StepList(RecipeText.stepLines(detail.instructions), numbered = true) } }
        item { DetailSection("Cách dùng", R.drawable.ic_cachdung) {
            StepList(RecipeText.stepLines(detail.servingText), bulleted = false) } }
    }
    sheet?.let { selected ->
        ModalBottomSheet(
            onDismissRequest = { sheet = null },
            containerColor = colors.surfaceContainerLow,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            IngredientSheet(selected, detail, onClose = { sheet = null })
        }
    }
}

/** Video (nếu có) được ưu tiên hơn ảnh — có video thì không hiển thị ảnh. */
@Composable
private fun RecipeHero(detail: RecipeDetail, onOpenExternal: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val videoId = youtubeVideoId(detail.videoUrl)
    if (videoId != null) {
        VideoHero(videoId, onOpenExternal)
    } else {
        Box(
            Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(colors.surfaceContainerHigh)
        ) {
            AsyncImage(
                detail.recipe.imageUrl, detail.recipe.name, Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.auth_grocery_illustration),
                fallback = painterResource(R.drawable.auth_grocery_illustration)
            )
            if (detail.videoUrl != null) Surface(
                onClick = onOpenExternal,
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp)
            ) {
                Text(
                    "Xem video hướng dẫn",
                    Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

/**
 * Poster YouTube hiện tức thì (Coil, có cache) để chặn màn hình đen lúc player tải.
 * WebView chỉ được tạo sau khi người dùng bấm Play, rồi fade-in khi trang xong.
 */
@Composable
private fun VideoHero(videoId: String, onOpenExternal: () -> Unit, modifier: Modifier = Modifier) {
    var playing by rememberSaveable(videoId) { mutableStateOf(false) }
    Box(
        modifier.fillMaxWidth().aspectRatio(16f / 9f)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(Color.Black)
    ) {
        AsyncImage(
            "https://i.ytimg.com/vi/$videoId/hqdefault.jpg", null, Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            error = painterResource(R.drawable.auth_grocery_illustration),
            fallback = painterResource(R.drawable.auth_grocery_illustration)
        )
        if (playing) {
            YouTubePlayer(videoId, Modifier.fillMaxSize())
        } else {
            Box(
                Modifier.fillMaxSize().clickable(onClickLabel = "Phát video") { playing = true },
                contentAlignment = Alignment.Center
            ) {
                Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.75f)) {
                    Icon(
                        Icons.Filled.PlayArrow, null,
                        Modifier.padding(start = 4.dp, top = 12.dp, end = 12.dp, bottom = 12.dp).size(36.dp),
                        tint = Color.White
                    )
                }
            }
            Surface(
                onClick = onOpenExternal,
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp)
            ) {
                Text(
                    "Xem trên YouTube",
                    Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun YouTubePlayer(videoId: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var ready by remember(videoId) { mutableStateOf(false) }
    val alpha by animateFloatAsState(if (ready) 1f else 0f, tween(300), label = "player-fade")
    val html = remember(videoId) {
        """<!doctype html><html><head><meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1"></head>
            <body style="margin:0;background:transparent"><div style="position:relative;width:100%;aspect-ratio:16/9">
            <iframe width="100%" height="100%" style="position:absolute;inset:0;border:0"
            src="https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&playsinline=1&controls=1&rel=0" title="Video hướng dẫn"
            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" allowfullscreen></iframe></div></body></html>""".trimIndent()
    }
    val webView = remember(videoId) {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) { ready = true }
            }
            loadDataWithBaseURL("https://www.youtube-nocookie.com", html, "text/html", "UTF-8", null)
        }
    }
    DisposableEffect(webView) { onDispose { webView.stopLoading(); webView.destroy() } }
    AndroidView(factory = { webView }, modifier = modifier.graphicsLayer { this.alpha = alpha })
}

private fun youtubeVideoId(url: String?): String? {
    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return null
    val host = uri.host?.lowercase().orEmpty()
    val id = when {
        host == "youtu.be" -> uri.pathSegments.firstOrNull()
        host == "youtube.com" || host == "www.youtube.com" || host == "m.youtube.com" || host == "youtube-nocookie.com" ->
            uri.getQueryParameter("v") ?: uri.pathSegments.let { segments ->
                val marker = segments.indexOfFirst { it == "embed" || it == "shorts" }
                if (marker >= 0) segments.getOrNull(marker + 1) else null
            }
        else -> null
    }
    return id?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{11}")) }
}

@Composable
private fun MetricStrip(detail: RecipeDetail) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = colors.surfaceContainerLowest,
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            DetailMetric(R.drawable.ic_servings, "Khẩu phần",
                detail.recipe.servings?.let { "$it người" } ?: "—", Modifier.weight(1f))
            MetricDivider()
            DetailMetric(R.drawable.ic_time, "Thời gian",
                detail.recipe.minutes?.let { "$it phút" } ?: "—", Modifier.weight(1f))
            MetricDivider()
            DetailMetric(R.drawable.ic_difficulty, "Độ khó", detail.recipe.difficulty ?: "—", Modifier.weight(1f))
        }
    }
}

@Composable
private fun MetricDivider() {
    Box(Modifier.size(width = 1.dp, height = 34.dp).background(MaterialTheme.colorScheme.outlineVariant))
}

@Composable
private fun DetailMetric(icon: Int, label: String, value: String, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(painterResource(icon), null, Modifier.size(22.dp), tint = colors.primary)
        Text(value, style = MaterialTheme.typography.titleSmall, color = colors.onSurface, textAlign = TextAlign.Center, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
    }
}

@Composable
private fun DetailSection(title: String, icon: Int, content: @Composable ColumnScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    var expanded by rememberSaveable(title) { mutableStateOf(true) }
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = colors.surfaceContainerLowest,
        border = BorderStroke(1.dp, colors.outlineVariant),
        modifier = Modifier.fillMaxWidth().animateContentSize()
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth()
                    .clickable(onClickLabel = if (expanded) "Thu gọn" else "Mở rộng") { expanded = !expanded }
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    Modifier.size(40.dp).clip(MaterialTheme.shapes.medium).background(colors.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painterResource(icon), null, Modifier.size(20.dp), tint = colors.onPrimaryContainer)
                }
                Text(
                    title,
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface
                )
                Icon(
                    if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    null,
                    Modifier.size(20.dp),
                    tint = colors.onSurfaceVariant
                )
            }
            AnimatedVisibility(
                expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically()
            ) {
                Column(
                    Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    content = content
                )
            }
        }
    }
}

@Composable
private fun StepList(items: List<String>, numbered: Boolean = false, bulleted: Boolean = true) {
    val colors = MaterialTheme.colorScheme
    if (items.isEmpty()) {
        Text("Chưa có hướng dẫn.", color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        return
    }
    val asSteps = numbered && items.size > 1
    items.forEachIndexed { index, line ->
        when {
            asSteps -> Surface(
                shape = MaterialTheme.shapes.medium,
                color = colors.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(shape = CircleShape, color = colors.primary) {
                        Text(
                            "${index + 1}", Modifier.size(26.dp),
                            textAlign = TextAlign.Center,
                            color = colors.onPrimary,
                            style = MaterialTheme.typography.labelMedium,
                            lineHeight = MaterialTheme.typography.labelMedium.lineHeight
                        )
                    }
                    Text(
                        line, Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurface
                    )
                }
            }
            bulleted -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                Box(Modifier.padding(top = 8.dp).size(5.dp).clip(CircleShape).background(colors.primary))
                Text(line, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
            }
            else -> Text(line, Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IngredientModeSwitch(selected: String, onSelect: (String) -> Unit) {
    val modes = listOf("Have" to "Đang có", "Need" to "Cần thêm")
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        modes.forEachIndexed { index, (key, label) ->
            SegmentedButton(
                selected = selected == key,
                onClick = { onSelect(key) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                icon = {}
            )
        }
    }
}

@Composable
private fun IngredientLine(item: RecipeIngredient) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.padding(top = 8.dp).size(5.dp).clip(CircleShape).background(colors.primary))
        Text(item.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
        Text(item.amount, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, textAlign = TextAlign.End)
    }
}

@Composable
private fun IngredientSheet(selected: String, detail: RecipeDetail, onClose: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                if (selected == "Have") "Đang có" else "Cần thêm",
                style = MaterialTheme.typography.titleLarge,
                color = colors.onSurface
            )
            Text(
                if (selected == "Have") "Nguyên liệu bạn đang có" else "Nguyên liệu còn thiếu hoặc chưa đủ",
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "Định lượng bên dưới theo công thức",
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall
            )
        }
        IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, "Đóng") }
    }
    val ingredients = if (selected == "Have") detail.have else detail.need
    LazyColumn(
        Modifier.fillMaxWidth().heightIn(max = 480.dp),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (ingredients == null) item {
            Text(
                "Chọn tủ lạnh trong Inventory để đối chiếu nguyên liệu.",
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        else if (ingredients.isEmpty()) item {
            Text(
                if (selected == "Have") "Chưa có nguyên liệu phù hợp trong tủ." else "Bạn đã có đủ các nguyên liệu được đối chiếu.",
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        } else itemsIndexed(ingredients) { _, ingredient -> IngredientSheetRow(ingredient, selected == "Have") }
    }
}

@Composable
private fun IngredientSheetRow(item: RecipeIngredient, have: Boolean) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.large,
        color = colors.surfaceContainerLowest,
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncImage(
                item.imageUrl, item.name, Modifier.size(48.dp),
                contentScale = ContentScale.Fit,
                error = painterResource(R.drawable.ic_nguyenlieu),
                fallback = painterResource(R.drawable.ic_nguyenlieu)
            )
            Column(Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                Text(
                    item.amount,
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            if (have) Surface(shape = CircleShape, color = colors.primary) {
                Icon(
                    painterResource(R.drawable.ic_check), "Đang có",
                    Modifier.padding(6.dp).size(18.dp), tint = colors.onPrimary
                )
            } else Text("Cần thêm", color = colors.primary, style = MaterialTheme.typography.labelSmall)
        }
    }
}
