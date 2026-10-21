package com.production.fridgewise.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.production.fridgewise.notifications.ExpiryNotifications
import com.production.fridgewise.notifications.ReminderStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Chỉ có ở bản debug: đăng thông báo nhắc hạn ngay để test popup/âm thanh.
 *
 * adb shell am broadcast -n com.production.fridgewise/.debug.TestReminderReceiver
 */
class TestReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                ExpiryNotifications.createChannel(context)
                // Bỏ mốc "đã gửi hôm nay" + xoá thông báo cũ để mỗi lần test đều kêu + bật popup đầy đủ.
                NotificationManagerCompat.from(context).cancel(ExpiryNotifications.NOTIFICATION_ID)
                ReminderStore(context).update { it.copy(sentDay = null, snoozeUntil = 0) }
                Log.d("TestReminder", "posted=${ExpiryNotifications.post(context)}")
            } finally {
                pending.finish()
            }
        }
    }
}
