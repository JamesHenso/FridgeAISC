package com.production.fridgewise.domain.model

import java.math.BigDecimal

data class ReceiptItem(
    val name: String,
    val ingredientId: String?,
    val quantity: Double?,
    val unit: String?,
    val purchasedAt: String?,
    val expiryDate: String?,
    val id: String? = null,
    val rawName: String? = null,
    val unitPriceVnd: Long? = null,
    val priceVnd: Long? = null,
    val sourceType: String? = null,
    val matchStatus: String? = null,
    val candidates: List<IngredientCandidate> = emptyList(),
    val imageUrl: String? = null
) {
    val quantityLabel: String get() = quantity?.let {
        "${BigDecimal.valueOf(it).stripTrailingZeros().toPlainString()} ${unit.orEmpty()}".trim()
    } ?: "Chưa rõ"
}
data class ReceiptDraft(val scanId: String, val items: List<ReceiptItem>)

data class IngredientCandidate(val name: String, val id: String)

enum class ScanMode { RECEIPT, PRODUCT }

data class OcrScanResult(
    val items: List<ReceiptItem>,
    val excludedNames: List<String> = emptyList(),
    val merchant: String? = null,
    val totalVnd: Long? = null,
    val subtotalVnd: Long? = null,
    val discountVnd: Long? = null,
    val lineTotalVnd: Long? = null,
    val reconciliationDeltaVnd: Long? = null,
    val mode: ScanMode = ScanMode.RECEIPT
)
