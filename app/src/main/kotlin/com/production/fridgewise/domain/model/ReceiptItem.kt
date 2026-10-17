package com.production.fridgewise.domain.model

import java.math.BigDecimal

data class ReceiptItem(
    val name: String,
    val ingredientId: String,
    val quantity: Double?,
    val unit: String?,
    val purchasedAt: String?,
    val expiryDate: String?,
    val id: String? = null
) {
    val quantityLabel: String get() = quantity?.let {
        "${BigDecimal.valueOf(it).stripTrailingZeros().toPlainString()} ${unit.orEmpty()}".trim()
    } ?: "Chưa rõ"
}
data class ReceiptDraft(val scanId: String, val items: List<ReceiptItem>)
