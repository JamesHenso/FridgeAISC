package com.production.fridgewise.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class InventoryTest {
    @Test fun expiryLabelsUseCalendarDaysAndThreeDaySoonWindow() {
        assertEquals("Expired 2 days ago!", Food("1", "Burger", expiryDate = "2026-10-13").status("2026-10-15").text)
        assertEquals("Use today!", Food("1", "Egg", expiryDate = "2026-10-15").status("2026-10-15").text)
        assertEquals("Expiring in 2 days!", Food("1", "Beef", expiryDate = "2026-10-17").status("2026-10-15").text)
        assertEquals("11 days in", Food("1", "Duck", purchasedAt = "2026-10-04", expiryDate = "2026-11-01").status("2026-10-15").text)
        assertEquals("No expiry date", Food("1", "Unknown").status("2026-10-15").text)
        assertEquals("Expires in 10 days", Food("1", "Milk", expiryDate = "2026-10-25").status("2026-10-15").text)
    }
    @Test fun quantityPreservesFractionsAndDoesNotInventMissingAmounts() {
        assertEquals("0.5 kg", Food("1", "Tomato", quantity = 0.5, unit = "kg").quantityLabel)
        assertEquals("2 pcs", Food("1", "Burger", quantity = 2.0, unit = "pcs").quantityLabel)
        assertEquals("—", Food("1", "Unknown").quantityLabel)
    }
}
