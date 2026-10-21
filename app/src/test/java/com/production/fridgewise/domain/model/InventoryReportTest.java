package com.production.fridgewise.domain.model;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.List;

public class InventoryReportTest {
    private Food food(String id, String purchase, String expiry, Long amount, String category, String source) {
        // Unit price deliberately differs: report spending must use the receipt amount.
        return new Food(id, id, 0.5, "kg", null, purchase, expiry, null, null,
                amount, 200000L, category, source);
    }

    @Test public void countsOnlyScannedLotsAndPreservesPaidAmount() {
        var rows = List.of(
            food("a", "2026-10-01", "2026-10-18", 45000L, "Thịt", "receipt"),
            food("b", "2026-10-08", "2026-10-19", 70000L, "Thịt", "product"),
            food("legacy", "2026-10-01", "2026-10-18", 999999L, "Rau", null));
        var report = InventoryReportKt.inventoryReport(rows, null, "2026-10-19");
        assertEquals(2, report.getFoods().size());
        assertEquals(1, report.getExpired().size());
        assertEquals(115000L, report.getSpent());
        assertEquals(45000L, report.getExpiredSpent());
        assertEquals(1, report.getUntrackedSources());
        assertEquals(2, report.getCategories().get(0).getCount());
        assertEquals(45000L, report.getCategories().get(0).getExpiredSpent());
    }

    @Test public void monthFilterAndWeekBoundariesAreConsistent() {
        var rows = List.of(
            food("a", "2026-10-07", null, 10L, "Rau", "receipt"),
            food("b", "2026-10-08", null, 20L, "Rau", "receipt"),
            food("c", "2026-10-29", "2026-11-01", 30L, "Rau", "receipt"),
            food("d", "2026-09-30", null, 100L, "Rau", "receipt"));
        var report = InventoryReportKt.inventoryReport(rows, "2026-10", "2026-11-02");
        assertEquals(60L, report.getSpent());
        assertEquals(5, report.getPeriods().size());
        assertEquals(10L, report.getPeriods().get(0).getSpent());
        assertEquals(20L, report.getPeriods().get(1).getSpent());
        assertEquals(30L, report.getPeriods().get(4).getSpent());
        assertEquals(30L, report.getExpiredSpent());
    }

    @Test public void unknownsAreNotInventedAndZeroIsKnown() {
        var rows = List.of(
            food("a", null, "2026-10-01", null, null, "receipt"),
            food("b", "2026-02-30", null, 0L, " ", "receipt"));
        var report = InventoryReportKt.inventoryReport(rows, null, "2026-10-19");
        assertEquals(0L, report.getSpent());
        assertEquals(1, report.getMissingPrices());
        assertEquals(2, report.getUnknownDates());
        assertEquals("Chưa phân loại", report.getCategories().get(0).getName());
        assertTrue(report.getPeriods().isEmpty());
        assertTrue(InventoryReportKt.inventoryReport(rows, "2026-10", "2026-10-19").getFoods().isEmpty());
    }

    @Test public void emptyReportIsSafe() {
        var report = InventoryReportKt.inventoryReport(List.of(), null, "2026-10-19");
        assertEquals(0L, report.getSpent());
        assertEquals(0L, report.getExpiredSpent());
        assertTrue(report.getCategories().isEmpty());
    }
}
