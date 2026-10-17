package com.production.fridgewise.ui.scan

import com.production.fridgewise.domain.model.ReceiptItem
import com.production.fridgewise.domain.model.ReceiptPlacement
import org.junit.Assert.*
import org.junit.Test

class ReceiptPlacementTest {
    private val items = listOf("a", "b").map { ReceiptItem(it, "ingredient", 1.0, "kg", null, null, it) }
    @Test fun placesInOrderIgnoresStaleDropsAndCanUndo() {
        val empty = ReceiptPlacement()
        assertEquals("a", empty.next(items)?.id)
        assertEquals(empty, empty.place(items, "b", "fridge"))
        assertEquals(empty, empty.place(items, "a", "pantry"))
        val first = empty.place(items, "a", "freezer")
        assertEquals(first, first.place(items, "a", "fridge"))
        assertEquals("b", first.next(items)?.id)
        val done = first.place(items, "b", "fridge")
        assertTrue(done.isComplete(items))
        assertEquals(mapOf("a" to "freezer", "b" to "fridge"), done.locations)
        assertEquals(first, done.undo())
        assertFalse(empty.isComplete(emptyList()))
    }
}
