package com.production.fridgewise.ui.inventory

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.production.fridgewise.domain.model.expiryReminders
import com.production.fridgewise.notifications.ExpiryNotifications
import com.production.fridgewise.notifications.ReminderStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun ReminderSettings(owner: String, token: String, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    BackHandler(onBack = onDismiss)
    val context = LocalContext.current
    val store = remember { ReminderStore(context) }
    val scope = rememberCoroutineScope()
    var snapshot by remember { mutableStateOf(store.read()) }
    var allowed by remember { mutableStateOf(ExpiryNotifications.allowed(context)) }
    var rationale by remember { mutableStateOf(false) }
    var permissionMessage by remember { mutableStateOf<String?>(null) }
    var syncing by remember { mutableStateOf(false) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                allowed = ExpiryNotifications.allowed(context)
                snapshot = store.read()
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    fun enable() {
        ExpiryNotifications.enable(context, owner, true)
        snapshot = store.read()
        scope.launch {
            syncing = true
            try {
                ExpiryNotifications.sync(context, owner, token)
                snapshot = store.read()
                permissionMessage = null
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) {
                permissionMessage = "Chưa đồng bộ được dữ liệu mới. Nhắc hạn sẽ dùng lần đồng bộ thành công gần nhất."
            } finally { syncing = false }
        }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        allowed = ExpiryNotifications.allowed(context)
        if (granted && allowed) enable()
        else permissionMessage = "Chưa được cấp quyền thông báo. Bạn có thể bật trong cài đặt Android."
    }
    fun requestPermission() {
        store.update { if (it.owner == owner) it.copy(permissionRequested = true) else it }
        snapshot = store.read()
        if (Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    fun openSettings() {
        context.startActivity(Intent(if (Build.VERSION.SDK_INT >= 26) Settings.ACTION_APP_NOTIFICATION_SETTINGS else Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            if (Build.VERSION.SDK_INT >= 26) putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            else data = android.net.Uri.parse("package:${context.packageName}")
        })
    }
    val iconScale by animateFloatAsState(if (snapshot.enabled) 1.12f else 1f, label = "settings-bell")
    Scaffold(
        containerColor = colors.background,
        contentColor = colors.onBackground
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(Modifier.widthIn(max = 620.dp).fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 64.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Quay lại")
                    }
                    Text(
                        "Cài đặt",
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.onSurface
                    )
                }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(colors.primaryContainer)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        Modifier.size(56.dp).clip(CircleShape).background(colors.onPrimaryContainer.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.NotificationsActive, null,
                            Modifier.size(30.dp).graphicsLayer { scaleX = iconScale; scaleY = iconScale },
                            tint = colors.onPrimaryContainer
                        )
                    }
                    Text(
                        "Dùng thực phẩm đúng lúc",
                        color = colors.onPrimaryContainer,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        "Nhận nhắc nhở trước khi thực phẩm hết hạn.",
                        color = colors.onPrimaryContainer.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                SectionLabel("THÔNG BÁO")
                Surface(
                    Modifier.fillMaxWidth().animateContentSize(),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = colors.surfaceContainerLowest,
                    border = BorderStroke(1.dp, colors.outlineVariant)
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(48.dp).clip(MaterialTheme.shapes.medium)
                                    .background(colors.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.NotificationsActive, null, tint = colors.onPrimaryContainer)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Nhắc hạn dùng",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.onSurface
                                )
                                Text(
                                    if (snapshot.enabled && allowed) "Đang bật" else "Đang tắt",
                                    color = if (snapshot.enabled && allowed) colors.primary else colors.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Switch(checked = snapshot.enabled && allowed, enabled = !syncing, onCheckedChange = { checked ->
                                if (!checked) {
                                    ExpiryNotifications.enable(context, owner, false)
                                    snapshot = store.read()
                                } else if (ExpiryNotifications.allowed(context)) enable()
                                else if (Build.VERSION.SDK_INT >= 33) {
                                    val activity = context.activity()
                                    when {
                                        activity != null && ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS) -> rationale = true
                                        snapshot.permissionRequested -> openSettings()
                                        else -> requestPermission()
                                    }
                                } else openSettings()
                            })
                        }
                        AnimatedVisibility(
                            snapshot.enabled,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                HorizontalDivider(color = colors.outlineVariant)
                                val count = expiryReminders(snapshot.foods).size
                                Text(
                                    if (snapshot.syncedAt == 0L) "Chưa có dữ liệu đồng bộ" else
                                        "Đồng bộ gần nhất: " + SimpleDateFormat("dd/MM HH:mm", Locale.forLanguageTag("vi-VN")).format(Date(snapshot.syncedAt)),
                                    color = colors.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (snapshot.syncedAt > 0) Text(
                                    "$count thực phẩm đang trong khoảng nhắc",
                                    color = colors.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        if (syncing) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 14.dp), color = colors.primary)
                        permissionMessage?.let {
                            Text(
                                it,
                                color = colors.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 10.dp)
                            )
                        }
                        if (snapshot.enabled && !allowed) Text(
                            "Android đang chặn thông báo. Hãy cấp quyền trong cài đặt Android.",
                            color = colors.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                        if (!allowed) TextButton(onClick = ::openSettings) { Text("Mở cài đặt thông báo Android") }
                    }
                }
                SectionLabel("CÁCH NHẮC")
                Surface(
                    Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = colors.surfaceContainerLowest,
                    border = BorderStroke(1.dp, colors.outlineVariant)
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Outlined.Security, null, tint = colors.primary)
                            Text(
                                "Nhắc sớm theo thói quen",
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.onSurface
                            )
                        }
                        Text(
                            "Mặc định trước 3 ngày. Nếu một nhóm thực phẩm thường bị hết hạn, FridgeWise sẽ nhắc trước 5 hoặc 7 ngày dựa trên dữ liệu Report.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                        HorizontalDivider(color = colors.outlineVariant)
                        Text(
                            "Tối đa một thông báo tổng hợp mỗi ngày, từ 08:00 đến 20:00. Không nhắc món đã hết hạn hoặc chưa có hạn dùng.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 20.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(colors.surfaceContainerHigh)
                        .padding(16.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Outlined.Info, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    Text(
                        "Thông báo chạy trên điện thoại kể cả khi đóng app. Hãy mở app để đồng bộ thay đổi từ thiết bị khác.",
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(Modifier.size(32.dp))
            }
        }
    }
    if (rationale) AlertDialog(
        onDismissRequest = { rationale = false },
        title = { Text("Cho phép nhắc hạn dùng?") },
        text = { Text("FridgeWise cần quyền thông báo để nhắc bạn dùng thực phẩm trước hạn, ngay cả khi app đang đóng.") },
        confirmButton = { TextButton(onClick = { rationale = false; requestPermission() }) { Text("Tiếp tục") } },
        dismissButton = { TextButton(onClick = { rationale = false }) { Text("Để sau") } }
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, top = 24.dp, bottom = 8.dp)
    )
}

private tailrec fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}
