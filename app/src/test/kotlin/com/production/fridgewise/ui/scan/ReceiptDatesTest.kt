package com.production.fridgewise.ui.scan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Bấm lịch đổi millis UTC -> chuỗi yyyy-MM-dd; lệch ngày/timezone ở đây sẽ âm thầm sai HSD. */
class ReceiptDatesTest {
    @Test fun pickerMillisRoundTripsToSameCalendarDay() {
        listOf("2026-04-28", "2027-01-01", "2026-12-31").forEach {
            assertEquals(it, formatReceiptDate(requireNotNull(parseReceiptDate(it))))
        }
    }

    @Test fun cardShowsDayMonthAndYear() {
        assertEquals("28/04/2026", displayDate("2026-04-28"))
        assertEquals("Chưa rõ", displayDate(null))
        assertEquals("Chưa rõ", displayDate("28/04/2026"))
    }

    @Test fun malformedDatesAreRejected() {
        assertTrue(validReceiptDate(""))
        assertTrue(validReceiptDate("2026-04-28"))
        assertFalse(validReceiptDate("2026-13-01"))
        assertFalse(validReceiptDate("28/04/2026"))
    }
}
