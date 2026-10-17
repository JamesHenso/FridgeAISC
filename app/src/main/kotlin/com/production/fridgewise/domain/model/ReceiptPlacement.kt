package com.production.fridgewise.domain.model

/** Ordered placements use scan-item IDs, never ingredient IDs (which may repeat). */
data class ReceiptPlacement(val locations: Map<String, String> = emptyMap()) {
    fun next(items: List<ReceiptItem>): ReceiptItem? = items.firstOrNull { it.id !in locations }
    fun isComplete(items: List<ReceiptItem>): Boolean = items.isNotEmpty() &&
        items.all { it.id != null && locations[it.id] in setOf("freezer", "fridge") }
    fun place(items: List<ReceiptItem>, id: String, location: String): ReceiptPlacement {
        if (location !in setOf("freezer", "fridge") || next(items)?.id != id) return this
        return copy(locations = locations + (id to location))
    }
    fun undo(): ReceiptPlacement = locations.keys.lastOrNull()?.let { copy(locations = locations - it) } ?: this
}
