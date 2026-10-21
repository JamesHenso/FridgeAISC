package com.production.fridgewise.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.production.fridgewise.MainActivity
import com.production.fridgewise.R
import com.production.fridgewise.data.repository.InventoryRepository
import com.production.fridgewise.domain.model.expiryReminders
import com.production.fridgewise.domain.model.inventoryToday
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import java.util.concurrent.TimeUnit

object ExpiryNotifications {
    const val CHANNEL = "food_expiry_v2"

    /** Channel cũ tạo với IMPORTANCE_DEFAULT; hệ thống không cho nâng importance nên bỏ nó. */
    private const val LEGACY_CHANNEL = "food_expiry"
    private const val WORK = "food-expiry-periodic"
    private const val NOW = "food-expiry-check"
    const val NOTIFICATION_ID = 7310

    /** Nút "Để sau" hoãn nhắc lại 3 tiếng. */
    const val SNOOZE_MILLIS = 3 * 60 * 60 * 1000L

    fun allowed(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < 26 || context.getSystemService(NotificationManager::class.java)
            .getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE
    }

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, "Thực phẩm sắp hết hạn", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Nhắc dùng thực phẩm đúng hạn, cá nhân hóa theo nhóm nguyên liệu trong Report."
                }
            )
            manager.deleteNotificationChannel(LEGACY_CHANNEL)
        }
    }

    fun account(context: Context, owner: String) {
        val store = ReminderStore(context)
        if (store.read().owner != owner) {
            // Wait for any worker holding the store lock before cancelling its notification.
            store.update { ReminderSnapshot(owner = owner) }
            stop(context)
        }
        createChannel(context)
        if (store.read().enabled) schedule(context)
    }

    fun enable(context: Context, owner: String, enabled: Boolean) {
        val state = ReminderStore(context).update {
            if (it.owner == owner) it.copy(enabled = enabled) else it
        }
        if (state.owner != owner) return
        if (state.enabled) { schedule(context); checkSoon(context) } else stop(context)
    }

    fun stop(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK)
        WorkManager.getInstance(context).cancelUniqueWork(NOW)
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    fun clearAccount(context: Context) {
        ReminderStore(context).update { ReminderSnapshot() }
        stop(context)
    }

    private fun schedule(context: Context) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ExpiryReminderWorker>(6, TimeUnit.HOURS).build())
    }
    fun checkSoon(context: Context, delayMillis: Long = 0) {
        val request = OneTimeWorkRequestBuilder<ExpiryReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            NOW,
            if (delayMillis > 0) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            request
        )
    }

    /** Full, unfiltered inventory from every fridge accessible to the authenticated account. */
    suspend fun sync(context: Context, owner: String, token: String) {
        val repository = InventoryRepository()
        val fridges = repository.fridges(token)
        val foods = fridges.flatMap { repository.foods(token, it.id) }.distinctBy { it.id }
        val snapshot = ReminderStore(context).update {
            if (it.owner == owner) it.copy(foods = foods, syncedAt = System.currentTimeMillis()) else it
        }
        if (snapshot.owner == owner && snapshot.enabled) checkSoon(context)
    }

    /** Dựng và đăng thông báo từ snapshot đã lưu. Dùng chung cho worker và trigger test. */
    @SuppressLint("MissingPermission") // allowed() đã kiểm tra POST_NOTIFICATIONS trước khi notify.
    suspend fun post(context: Context): Boolean {
        val today = inventoryToday()
        val now = System.currentTimeMillis()
        // Read before the lock: tải ảnh có thể chậm, không được chặn nút "Để sau".
        val snapshot = ReminderStore(context).read()
        if (!snapshot.enabled || snapshot.owner.isBlank() || snapshot.snoozed(now)) return false
        val candidates = expiryReminders(snapshot.foods, today)
        if (candidates.isEmpty()) {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
            return false
        }
        val largeIcon = loadLargeIcon(candidates.first().food.imageUrl)
        var posted = false
        // Posting and marking happen under the same lock, including account/opt-out checks.
        ReminderStore(context).update { current ->
            if (!current.enabled || current.owner.isBlank() || current.snoozed(now)) return@update current
            // Đã gửi hôm nay và không phải lần nhắc lại sau "Để sau" → chỉ làm mới, không kêu.
            val alreadySent = current.sentDay == today && current.snoozeUntil <= 0
            val lines = candidates.take(5).map { it.message }.toMutableList()
            if (candidates.size > 5) lines += "Và ${candidates.size - 5} thực phẩm khác."
            if (candidates.any { it.personalized }) lines += "Nhắc sớm hơn vì nhóm này có tỷ lệ hết hạn cao trong Report."
            val intent = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val snooze = PendingIntent.getBroadcast(
                context, 1, Intent(context, SnoozeReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val builder = NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_expiry_notification)
                .setContentTitle("${candidates.size} thực phẩm cần dùng sớm")
                .setContentText(candidates.first().message)
                .addAction(0, "Xem thực phẩm", pending)
                .addAction(0, "Để sau", snooze)
                .setContentIntent(pending).setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOnlyAlertOnce(true)
                // Đã gửi hôm nay và không phải lần nhắc lại sau "Để sau" → chỉ làm mới, không kêu.
                .setSilent(alreadySent)
            // Một món: giữ layout gọn (tiêu đề + dòng trạng thái + ảnh). Nhiều món: liệt kê.
            if (candidates.size > 1) builder.setStyle(NotificationCompat.BigTextStyle().bigText(lines.joinToString("\n")))
            if (largeIcon != null) builder.setLargeIcon(largeIcon)
            if (allowed(context)) {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
                posted = true
                current.copy(sentDay = today, snoozeUntil = 0)
            } else current
        }
        return posted
    }
}

class ExpiryReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val context = applicationContext
        ExpiryNotifications.createChannel(context)
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (hour !in 8..19 || !ExpiryNotifications.allowed(context)) return Result.success()
        return try {
            ExpiryNotifications.post(context)
            Result.success()
        } catch (e: CancellationException) { throw e
        } catch (_: SecurityException) { Result.success()
        } catch (_: Exception) { if (runAttemptCount < 3) Result.retry() else Result.failure() }
    }
}

/** "Để sau": tắt thông báo hiện tại, hoãn 3 tiếng rồi nhắc lại (có âm thanh). */
class SnoozeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val snapshot = ReminderStore(context).update {
            it.copy(snoozeUntil = System.currentTimeMillis() + ExpiryNotifications.SNOOZE_MILLIS)
        }
        NotificationManagerCompat.from(context).cancel(ExpiryNotifications.NOTIFICATION_ID)
        if (snapshot.enabled && snapshot.owner.isNotBlank()) {
            ExpiryNotifications.checkSoon(context, ExpiryNotifications.SNOOZE_MILLIS)
        }
    }
}

/** Ảnh món ăn cho large icon; lỗi mạng/URL thì bỏ qua, không chặn thông báo. */
private suspend fun loadLargeIcon(url: String?): Bitmap? = withContext(Dispatchers.IO) {
    if (url.isNullOrBlank()) return@withContext null
    val bitmap = runCatching {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 5_000
        connection.readTimeout = 5_000
        connection.inputStream.use { BitmapFactory.decodeStream(it) }
    }.getOrNull() ?: return@withContext null
    val largest = maxOf(bitmap.width, bitmap.height)
    if (largest <= 320) bitmap
    else {
        val scale = 320f / largest
        Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true
        ).also { if (it !== bitmap) bitmap.recycle() }
    }
}
