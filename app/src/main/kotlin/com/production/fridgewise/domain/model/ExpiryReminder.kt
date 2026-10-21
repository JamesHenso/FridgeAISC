package com.production.fridgewise.domain.model

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class ReminderCandidate(val food: Food, val daysLeft: Int, val leadDays: Int) {
    val personalized: Boolean get() = leadDays > 3
    val message: String get() = when (daysLeft) {
        0 -> "${food.name}: cần dùng trong hôm nay"
        1 -> "${food.name}: hết hạn ngày mai"
        else -> "${food.name}: còn $daysLeft ngày đến hạn"
    }
}

/** Same category cohorts as Report. This estimates risk; it does not infer consumption. */
fun reminderLeadDays(category: ReportCategory?): Int {
    if (category == null || category.name == "Chưa phân loại" || category.count < 5 || category.expiredCount < 2) return 3
    val ratio = category.expiredCount.toDouble() / category.count
    return when {
        ratio >= 0.5 -> 7
        ratio >= 0.3 -> 5
        else -> 3
    }
}

fun expiryReminders(foods: List<Food>, today: String = inventoryToday()): List<ReminderCandidate> {
    val day = reminderDay(today) ?: return emptyList()
    val distinct = foods.distinctBy { it.id }
    val stats = inventoryReport(distinct, today = today).categories.associateBy { it.name }
    return distinct.mapNotNull { food ->
        if (food.quantity != null && food.quantity <= 0) return@mapNotNull null
        val expiry = food.expiryDate?.let(::reminderDay) ?: return@mapNotNull null
        val left = (expiry - day).toInt()
        val lead = reminderLeadDays(stats[food.category?.trim()])
        if (left !in 0..lead) null else ReminderCandidate(food, left, lead)
    }.sortedWith(compareBy<ReminderCandidate> { it.daysLeft }.thenByDescending { it.leadDays }.thenBy { it.food.id })
}

private fun reminderDay(value: String): Long? {
    val date = value.take(10)
    if (!Regex("\\d{4}-\\d{2}-\\d{2}").matches(date)) return null
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        isLenient = false
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return runCatching { format.parse(date)?.time?.div(86_400_000) }.getOrNull()
}
