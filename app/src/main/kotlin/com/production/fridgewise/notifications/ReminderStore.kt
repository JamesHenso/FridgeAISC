package com.production.fridgewise.notifications

import android.content.Context
import android.util.AtomicFile
import com.google.gson.Gson
import com.production.fridgewise.domain.model.Food
import java.io.File

data class ReminderSnapshot(
    val owner: String = "",
    val enabled: Boolean = false,
    val permissionRequested: Boolean = false,
    val foods: List<Food> = emptyList(),
    val syncedAt: Long = 0,
    val sentDay: String? = null,
    /** "Để sau" tạm hoãn tới mốc này; 0 = không hoãn. */
    val snoozeUntil: Long = 0
) {
    fun snoozed(now: Long = System.currentTimeMillis()): Boolean = snoozeUntil > now
}

/** No credentials; no-backup storage keeps inventory and notification preferences device-local. */
class ReminderStore(context: Context) {
    private val file = AtomicFile(File(context.noBackupFilesDir, "expiry-reminders.json"))
    private val gson = Gson()
    fun read(): ReminderSnapshot = synchronized(lock) { readLocked() }
    private fun readLocked(): ReminderSnapshot = runCatching {
        file.openRead().bufferedReader().use { gson.fromJson(it, ReminderSnapshot::class.java) }
    }.getOrNull() ?: ReminderSnapshot()

    fun update(change: (ReminderSnapshot) -> ReminderSnapshot): ReminderSnapshot = synchronized(lock) {
        val result = change(readLocked())
        val stream = file.startWrite()
        try {
            stream.write(gson.toJson(result).toByteArray(Charsets.UTF_8))
            file.finishWrite(stream)
        } catch (error: Exception) {
            file.failWrite(stream)
            throw error
        }
        result
    }
    companion object { private val lock = Any() }
}
