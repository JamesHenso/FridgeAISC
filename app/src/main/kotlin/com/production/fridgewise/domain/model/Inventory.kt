package com.production.fridgewise.domain.model

import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class Fridge(val id: String, val name: String)
data class InventoryStats(val inStock: Int = 0, val useToday: Int = 0, val useSoon: Int = 0, val cleanOut: Int = 0)
enum class FoodUrgency { NORMAL, SOON, TODAY, EXPIRED }
enum class InventoryFilter(val label: String) {
    STOCK("Stock"), TODAY("Today"), SOON("Soon"), CLEAN("Clean");

    fun matches(food: Food, today: String = inventoryToday()): Boolean {
        val urgency = food.status(today).urgency
        return when (this) {
            STOCK -> urgency != FoodUrgency.EXPIRED
            TODAY -> urgency == FoodUrgency.TODAY
            SOON -> urgency == FoodUrgency.SOON
            CLEAN -> urgency == FoodUrgency.EXPIRED
        }
    }
}
data class FoodStatus(val text: String, val urgency: FoodUrgency = FoodUrgency.NORMAL)
data class Food(
    val id: String,
    val name: String,
    val quantity: Double? = null,
    val unit: String? = null,
    val location: String? = null,
    val purchasedAt: String? = null,
    val expiryDate: String? = null,
    val ingredientId: String? = null,
    val imageUrl: String? = null,
    val priceVnd: Long? = null,
    val unitPriceVnd: Long? = null,
    val category: String? = null,
    val sourceType: String? = null
) {
    // Prefer unit price × current quantity so partially used stock is not counted at its original full price.
    val estimatedValueVnd: Long? get() = when {
        unitPriceVnd != null && quantity != null -> (unitPriceVnd * quantity).toLong()
        else -> priceVnd
    }

    val quantityLabel: String get() = quantity?.let {
        "${BigDecimal.valueOf(it).stripTrailingZeros().toPlainString()} ${unit.orEmpty()}".trim()
    } ?: "—"

    fun status(today: String = inventoryToday()): FoodStatus {
        val now = dayNumber(today) ?: return FoodStatus("No expiry date")
        val remaining = expiryDate?.let(::dayNumber)?.minus(now)
        if (remaining != null) {
            val days = kotlin.math.abs(remaining)
            val suffix = if (days == 1L) "day" else "days"
            if (remaining < 0) return FoodStatus("Expired $days $suffix ago!", FoodUrgency.EXPIRED)
            if (remaining == 0L) return FoodStatus("Use today!", FoodUrgency.TODAY)
            if (remaining <= 3) return FoodStatus("Expiring in $days $suffix!", FoodUrgency.SOON)
        }
        val age = purchasedAt?.let(::dayNumber)?.let { (now - it).coerceAtLeast(0) }
        return FoodStatus(when {
            age != null -> "$age ${if (age == 1L) "day" else "days"} in"
            remaining != null -> "Expires in $remaining days"
            else -> "No expiry date"
        })
    }
}

// Matches the backend's UTC calendar-day buckets, independent of the device timezone.
fun inventoryToday(): String = dateFormat().format(Date())
private fun dateFormat() = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("UTC")
    isLenient = false
}
private fun dayNumber(value: String): Long? = runCatching {
    dateFormat().parse(value.take(10))?.time?.div(86_400_000)
}.getOrNull()

data class InventoryUiState(
    val fridges: List<Fridge> = emptyList(),
    val selectedFridgeId: String? = null,
    val stats: InventoryStats = InventoryStats(),
    val foods: List<Food> = emptyList(),
    val location: String? = null,
    val query: String = "",
    val filter: InventoryFilter = InventoryFilter.STOCK,
    val isLoading: Boolean = true,
    val error: String? = null,
    val needsSignIn: Boolean = false
)
