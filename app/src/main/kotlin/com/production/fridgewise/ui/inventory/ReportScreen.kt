package com.production.fridgewise.ui.inventory

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.production.fridgewise.R
import com.production.fridgewise.domain.model.Food
import com.production.fridgewise.domain.model.FoodUrgency
import com.production.fridgewise.domain.model.InventoryReport
import com.production.fridgewise.domain.model.InventoryUiState
import com.production.fridgewise.domain.model.ReportCategory
import com.production.fridgewise.domain.model.ReportPeriod
import com.production.fridgewise.domain.model.inventoryReport
import com.production.fridgewise.domain.model.inventoryToday
import com.production.fridgewise.domain.model.reportMonth
import com.production.fridgewise.ui.components.RefreshableContent
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.delay

private fun money(value: Long) = NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(value) + " đ"
private fun monthLabel(month: String?) = month?.let { "Tháng ${it.takeLast(2).toInt()} / ${it.take(4)}" } ?: "Tất cả thời gian"

@Composable
internal fun WeeklyReportScreen(state: InventoryUiState, onRefresh: () -> Unit, onSignIn: () -> Unit, onFeature: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    BackHandler { onFeature("Inventory") }
    var month by rememberSaveable(state.selectedFridgeId) { mutableStateOf<String?>(null) }
    var menu by remember { mutableStateOf(false) }
    val today = inventoryToday()
    val report = remember(state.foods, month, today) { inventoryReport(state.foods, month, today) }
    val months = remember(state.foods) {
        state.foods.filter { it.sourceType in listOf("receipt", "product") }.mapNotNull { it.reportMonth() }.distinct().sortedDescending()
    }
    val attention = remember(report.foods, today) {
        report.foods.count { food -> food.status(today).urgency in listOf(FoodUrgency.SOON, FoodUrgency.TODAY) }
    }
    Scaffold(
        containerColor = colors.background,
        contentColor = colors.onBackground,
        bottomBar = { InventoryBottomBar(onFeature, "Report") }
    ) { padding ->
        RefreshableContent(state.isLoading, onRefresh, Modifier.padding(padding)) { pullRefreshing ->
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onFeature("Inventory") }) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Về Inventory")
                        }
                        Column(Modifier.weight(1f).padding(start = 4.dp)) {
                            Text("Report", style = MaterialTheme.typography.headlineMedium, color = colors.onSurface)
                            Text(
                                state.fridges.firstOrNull { it.id == state.selectedFridgeId }?.name ?: "Tổng quan tủ lạnh",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                        }
                    }
                }
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box {
                            Surface(
                                onClick = { menu = true },
                                color = colors.surfaceContainerLowest,
                                shape = CircleShape,
                                border = BorderStroke(1.dp, colors.outlineVariant)
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Outlined.CalendarToday, null, Modifier.size(16.dp), tint = colors.primary)
                                    Text(monthLabel(month), style = MaterialTheme.typography.labelMedium, color = colors.onSurface)
                                    Icon(Icons.Outlined.ExpandMore, null, Modifier.size(18.dp), tint = colors.onSurfaceVariant)
                                }
                            }
                            DropdownMenu(menu, { menu = false }) {
                                DropdownMenuItem(text = { Text("Tất cả thời gian") }, onClick = { month = null; menu = false })
                                months.forEach { value -> DropdownMenuItem(text = { Text(monthLabel(value)) }, onClick = { month = value; menu = false }) }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(6.dp).background(colors.primary, CircleShape))
                            Text(
                                "${report.foods.size} lô · ${report.categories.size} nhóm",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant
                            )
                        }
                    }
                }
                when {
                    state.isLoading && !pullRefreshing -> item { ReportSkeleton() }
                    state.error != null -> item { ReportError(state.error, state.needsSignIn, onSignIn, onRefresh) }
                    else -> {
                        item { Reveal(0) { SpendingHero(report) } }
                        item { Reveal(1) { HealthStrip(report.foods.size, attention, report.expired.size) } }
                        if (report.foods.isEmpty()) item {
                            Reveal(2) { EmptyReport(onScan = { onFeature("Scan") }) }
                        } else {
                            item { Reveal(2) { CategoryChart(report.categories, report.foods.size) } }
                            item { Reveal(3) { SpendingChart(report.periods, month != null) } }
                            item { Reveal(4) { ExpiredImpact(report.categories) } }
                            if (report.expired.isNotEmpty()) {
                                item {
                                    Reveal(5) {
                                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                "Cần dọn khỏi tủ",
                                                Modifier.weight(1f),
                                                style = MaterialTheme.typography.titleLarge,
                                                color = colors.onSurface
                                            )
                                            Surface(color = colors.errorContainer, shape = CircleShape) {
                                                Text(
                                                    "${report.expired.size} lô",
                                                    color = colors.onErrorContainer,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                items(report.expired, key = { "expired-${it.id}" }) { food -> ExpiredReportItem(food) }
                            }
                            item {
                                Reveal(6) {
                                    ReportCard {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Outlined.Info, null, Modifier.size(18.dp), tint = colors.onSurfaceVariant)
                                            Text("Cách tính báo cáo", style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                                        }
                                        Text(
                                            "Mỗi dòng thực phẩm là một lô đã quét và còn được lưu trong Inventory. Lọc tháng theo ngày mua; hết hạn tính đến hôm nay. Không bao gồm thực phẩm đã xóa khỏi kho.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.onSurfaceVariant
                                        )
                                        if (report.missingPrices > 0) Text(
                                            "${report.missingPrices} lô chưa có thành tiền, chưa tính vào tổng chi.",
                                            color = colors.error, style = MaterialTheme.typography.bodySmall
                                        )
                                        if (report.unknownDates > 0) Text(
                                            "${report.unknownDates} lô thiếu ngày mua: chỉ có trong mục Tất cả thời gian, không đưa vào biểu đồ theo thời gian.",
                                            color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall
                                        )
                                        if (report.untrackedSources > 0) Text(
                                            "${report.untrackedSources} lô không xác định nguồn quét, không tính trong báo cáo này.",
                                            color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Staggered entrance so the dashboard assembles instead of popping in at once. */
@Composable
private fun Reveal(index: Int, content: @Composable () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(index * 55L); shown = true }
    val alpha by animateFloatAsState(if (shown) 1f else 0f, tween(420), label = "report-reveal-alpha")
    val lift by animateFloatAsState(if (shown) 0f else 14f, tween(420, easing = FastOutSlowInEasing), label = "report-reveal-lift")
    Box(Modifier.fillMaxWidth().graphicsLayer {
        this.alpha = alpha
        translationY = lift.dp.toPx()
    }) { content() }
}

@Composable
private fun ReportSkeleton() {
    val colors = MaterialTheme.colorScheme
    val pulse by rememberInfiniteTransition(label = "report-skeleton").animateFloat(
        0.45f, 0.9f,
        infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "report-skeleton-alpha"
    )
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SkeletonBlock(176.dp, 24.dp, pulse, colors.surfaceContainerHigh)
        SkeletonBlock(96.dp, 20.dp, pulse, colors.surfaceContainerHigh)
        SkeletonBlock(330.dp, 20.dp, pulse, colors.surfaceContainerHigh)
    }
}

@Composable
private fun SkeletonBlock(height: Dp, radius: Dp, pulse: Float, color: Color) {
    Box(
        Modifier.fillMaxWidth().height(height)
            .clip(RoundedCornerShape(radius))
            .background(color.copy(alpha = pulse))
    )
}

@Composable
private fun ReportError(message: String, needsSignIn: Boolean, onSignIn: () -> Unit, onRefresh: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    ReportCard {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.WarningAmber, null, Modifier.size(24.dp), tint = colors.error)
            Text("Không thể tải báo cáo", style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
        }
        Text(message, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Button(
            onClick = if (needsSignIn) onSignIn else onRefresh,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
        ) {
            Text(if (needsSignIn) "Đăng nhập" else "Thử lại", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun EmptyReport(onScan: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    ReportCard {
        Box(
            Modifier.size(48.dp).clip(CircleShape).background(colors.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Insights, null, Modifier.size(26.dp), tint = colors.onPrimaryContainer)
        }
        Text("Chưa có dữ liệu trong kỳ này", style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
        Text(
            "Quét hóa đơn và thêm thực phẩm vào Inventory để bắt đầu theo dõi chi tiêu, hạn dùng và nhóm nguyên liệu.",
            color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium
        )
        Button(
            onClick = onScan,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
        ) {
            Icon(Icons.Outlined.CameraAlt, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Quét hóa đơn", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun ReportCard(content: @Composable ColumnScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = colors.surfaceContainerLowest,
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun SpendingHero(report: InventoryReport) {
    val colors = MaterialTheme.colorScheme
    val ratio = if (report.spent > 0) (report.expiredSpent.toFloat() / report.spent).coerceIn(0f, 1f) else 0f
    val progress by animateFloatAsState(ratio, tween(650), label = "expired-spend")
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = colors.primary,
        contentColor = colors.onPrimary
    ) {
        Box {
            Canvas(Modifier.matchParentSize()) {
                drawCircle(colors.onPrimary.copy(alpha = .05f), radius = size.width * .45f, center = Offset(size.width, 0f))
                drawCircle(colors.onPrimary.copy(alpha = .05f), radius = size.width * .27f, center = Offset(size.width, 0f))
            }
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.AccountBalanceWallet, null, Modifier.size(20.dp), tint = colors.onPrimary.copy(alpha = .8f))
                    Text(
                        "TỔNG CHI THỰC PHẨM",
                        color = colors.onPrimary.copy(alpha = .8f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                AnimatedContent(report.spent, label = "report-total") { total ->
                    Text(money(total), color = colors.onPrimary, style = MaterialTheme.typography.headlineLarge)
                }
                HorizontalDivider(color = colors.onPrimary.copy(alpha = .16f))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "Chi cho thực phẩm hết hạn",
                            color = colors.onPrimary.copy(alpha = .75f),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(money(report.expiredSpent), color = colors.tertiaryContainer, style = MaterialTheme.typography.titleLarge)
                    }
                    Surface(color = colors.onPrimary.copy(alpha = .14f), shape = CircleShape) {
                        Text(
                            "${(ratio * 100).toInt()}%",
                            Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            color = colors.tertiaryContainer,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = colors.tertiaryContainer,
                    trackColor = colors.onPrimary.copy(alpha = .16f),
                    drawStopIndicator = {}
                )
            }
        }
    }
}

@Composable
private fun HealthStrip(total: Int, attention: Int, expired: Int) {
    val colors = MaterialTheme.colorScheme
    Surface(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = colors.surfaceContainerLowest,
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Metric("Tổng lô", total, Icons.Outlined.Inventory2, colors.primary, Modifier.weight(1f))
            MetricDivider()
            Metric("Sắp hết hạn", attention, Icons.Outlined.Schedule, colors.tertiary, Modifier.weight(1f))
            MetricDivider()
            Metric("Đã hết hạn", expired, Icons.Outlined.WarningAmber, colors.error, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MetricDivider() {
    Box(Modifier.width(1.dp).height(46.dp).background(MaterialTheme.colorScheme.outlineVariant))
}

@Composable
private fun Metric(label: String, value: Int, icon: ImageVector, accent: Color, modifier: Modifier) {
    val animated by animateIntAsState(value, tween(600), label = "report-metric-$label")
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, null, Modifier.size(20.dp), tint = accent)
        Text(animated.toString(), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun CategoryChart(categories: List<ReportCategory>, total: Int) {
    val colors = MaterialTheme.colorScheme
    val chartColors = listOf(
        colors.primary,
        colors.tertiary,
        colors.secondary,
        colors.error,
        colors.inversePrimary,
        colors.outline
    )
    val visible = if (categories.size <= 6) categories else categories.take(5) + ReportCategory("Nhóm còn lại", categories.drop(5).sumOf { it.count }, 0, 0, 0)
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(visible) { entered = true }
    val sweep by animateFloatAsState(if (entered) 1f else 0f, tween(750), label = "category-donut")
    ReportCard {
        SectionTitle(Icons.Outlined.PieChart, "Bạn đang mua những gì?")
        Text("Phân bố nhóm nguyên liệu đã quét", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        Box(Modifier.align(Alignment.CenterHorizontally).size(190.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize().semantics { contentDescription = "Phân bố $total lô thực phẩm theo nhóm" }) {
                val stroke = 24.dp.toPx()
                val inset = stroke / 2
                val bounds = Size(size.width - stroke, size.height - stroke)
                drawArc(colors.surfaceContainerHigh, 0f, 360f, false, Offset(inset, inset), bounds, style = Stroke(stroke))
                var start = -90f
                visible.forEachIndexed { index, category ->
                    val angle = category.count.toFloat() / total.coerceAtLeast(1) * 360f
                    drawArc(
                        chartColors[index % chartColors.size], start, (angle - 3f).coerceAtLeast(0f) * sweep, false,
                        Offset(inset, inset), bounds, style = Stroke(stroke, cap = StrokeCap.Butt)
                    )
                    start += angle
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(total.toString(), style = MaterialTheme.typography.headlineLarge, color = colors.onSurface)
                Text("lô thực phẩm", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
        }
        visible.forEachIndexed { index, category ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(8.dp).background(chartColors[index % chartColors.size], CircleShape))
                Text(category.name, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = colors.onSurface)
                Text("${category.count}", style = MaterialTheme.typography.labelMedium, color = colors.onSurface)
                Text(
                    "${category.count * 100 / total.coerceAtLeast(1)}%",
                    Modifier.width(40.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(icon: ImageVector, title: String) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, Modifier.size(20.dp), tint = colors.primary)
        Text(title, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
    }
}

@Composable
private fun SpendingChart(periods: List<ReportPeriod>, weekly: Boolean) {
    val colors = MaterialTheme.colorScheme
    val max = periods.maxOfOrNull { it.spent }?.coerceAtLeast(1) ?: 1L
    val average = if (periods.isEmpty()) 0L else periods.sumOf { it.spent } / periods.size
    ReportCard {
        SectionTitle(Icons.Outlined.BarChart, if (weekly) "Chi tiêu theo tuần" else "Chi tiêu theo tháng")
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Legend("Đã chi", colors.primary)
            Legend("Đã hết hạn", colors.error)
        }
        if (periods.isEmpty()) Text(
            "Chưa có ngày mua để vẽ biểu đồ",
            color = colors.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
        else {
            Text(
                "TB ${money(average)}/kỳ · nghìn đồng · kéo ngang để xem",
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall
            )
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                periods.forEach { period ->
                    Column(
                        Modifier.width(64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            Modifier.height(136.dp),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            MoneyBar(period.spent, max, colors.primary)
                            MoneyBar(period.expiredSpent, max, colors.error)
                        }
                        Text(
                            if (weekly) period.label else period.label.takeLast(2) + "/" + period.label.substring(2, 4),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }
            if (weekly) Text(
                "T1: 1–7 · T2: 8–14 · T3: 15–21 · T4: 22–28 · T5: 29–31",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MoneyBar(amount: Long, max: Long, color: Color) {
    val height by animateFloatAsState((amount.toFloat() / max).coerceIn(0f, 1f), tween(650), label = "spend-bar")
    Column(
        Modifier.width(29.dp).semantics { contentDescription = money(amount) },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).apply { maximumFractionDigits = 1 }.format(amount / 1000.0),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier.fillMaxWidth().height((104 * height).dp.coerceAtLeast(2.dp))
                .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                .background(color)
        )
    }
}

@Composable
private fun Legend(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ExpiredImpact(categories: List<ReportCategory>) {
    val colors = MaterialTheme.colorScheme
    val rows = categories.filter { it.expiredCount > 0 }.sortedByDescending { it.expiredCount }
    // Share of all expired lots, not of the biggest category: three equal groups should read 33% each, not 100%.
    val totalExpired = rows.sumOf { it.expiredCount }.coerceAtLeast(1)
    ReportCard {
        SectionTitle(Icons.Outlined.WarningAmber, "Tác động thực phẩm hết hạn")
        Text(
            "Số lô và chi phí đã lưu theo nhóm nguyên liệu",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant
        )
        if (rows.isEmpty()) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.CheckCircle, null, Modifier.size(18.dp), tint = colors.primary)
            Text("Tuyệt vời, chưa có thực phẩm hết hạn!", color = colors.primary, style = MaterialTheme.typography.bodySmall)
        }
        rows.forEach { row ->
            val fraction by animateFloatAsState(row.expiredCount.toFloat() / totalExpired, tween(600), label = "expired-category")
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        row.name, Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurface,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "${row.expiredCount} lô",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.error
                        )
                        Text(
                            if (row.expiredSpent > 0) money(row.expiredSpent) else "Chưa có giá",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = colors.error,
                    trackColor = colors.errorContainer,
                    drawStopIndicator = {}
                )
            }
        }
    }
}

@Composable
private fun ExpiredReportItem(food: Food) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = colors.surfaceContainerLowest,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(Modifier.width(4.dp).height(42.dp).clip(CircleShape).background(colors.error))
            AsyncImage(
                food.imageUrl, null,
                Modifier.size(44.dp).clip(MaterialTheme.shapes.medium).background(colors.surfaceContainerHigh).padding(4.dp),
                error = painterResource(R.drawable.ic_nguyenlieu),
                fallback = painterResource(R.drawable.ic_nguyenlieu)
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    food.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.onSurface,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                Text(
                    "HSD ${food.expiryDate?.take(10).orEmpty()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
            }
            Text(
                food.priceVnd?.let(::money) ?: "Chưa có giá",
                style = MaterialTheme.typography.labelMedium,
                color = colors.error
            )
        }
    }
}
