package com.production.fridgewise.domain.model

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class ReportCategory(val name: String, val count: Int, val spent: Long, val expiredCount: Int, val expiredSpent: Long)
data class ReportPeriod(val label: String, val spent: Long, val expiredSpent: Long)
data class InventoryReport(
    val foods: List<Food>,
    val expired: List<Food>,
    val spent: Long,
    val expiredSpent: Long,
    val missingPrices: Int,
    val unknownDates: Int,
    val untrackedSources: Int,
    val categories: List<ReportCategory>,
    val periods: List<ReportPeriod>
)

fun Food.reportMonth(): String? {
    val value = purchasedAt?.take(10) ?: return null
    if (!Regex("\\d{4}-\\d{2}-\\d{2}").matches(value)) return null
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        isLenient = false
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return runCatching { format.parse(value) }.getOrNull()?.let { value.take(7) }
}

fun inventoryReport(foods: List<Food>, month: String? = null, today: String = inventoryToday()): InventoryReport {
    val scanned = foods.filter { it.sourceType == "receipt" || it.sourceType == "product" }
    val selected = scanned.filter { month == null || it.reportMonth() == month }
    fun Food.expired() = status(today).urgency == FoodUrgency.EXPIRED
    // Use the saved receipt line amount, not unit price × remaining inventory quantity.
    fun List<Food>.spent() = mapNotNull { it.priceVnd?.takeIf { value -> value >= 0 } }.sum()
    val expired = selected.filter { it.expired() }
    val categories = selected.groupBy { it.category?.trim()?.takeIf(String::isNotEmpty) ?: "Chưa phân loại" }
        .map { (name, rows) ->
            val old = rows.filter { it.expired() }
            ReportCategory(name, rows.size, rows.spent(), old.size, old.spent())
        }.sortedWith(compareByDescending<ReportCategory> { it.count }.thenBy { it.name })
    val buckets = if (month == null) {
        selected.mapNotNull { it.reportMonth() }.distinct().sorted().map { key ->
            key to selected.filter { it.reportMonth() == key }
        }
    } else {
        (1..5).map { week ->
            "T$week" to selected.filter { ((it.purchasedAt!!.substring(8, 10).toInt() - 1) / 7 + 1) == week }
        }
    }
    return InventoryReport(selected, expired, selected.spent(), expired.spent(),
        selected.count { it.priceVnd == null || it.priceVnd < 0 },
        scanned.count { it.reportMonth() == null }, foods.size - scanned.size,
        categories, buckets.map { (key, rows) -> ReportPeriod(key, rows.spent(), rows.filter { it.expired() }.spent()) })
}
