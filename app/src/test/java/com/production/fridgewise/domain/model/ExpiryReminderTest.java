package com.production.fridgewise.domain.model;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.List;

public class ExpiryReminderTest {
    private Food food(String id, String expiry, String category, String source) {
        return new Food(id, id, 1.0, "kg", null, "2026-10-01", expiry,
                null, null, null, null, category, source);
    }
    private ReportCategory group(int total, int expired) {
        return new ReportCategory("Rau", total, 0, expired, 0);
    }
    @Test public void defaultsForSmallSamplesAndUnknownCategories() {
        assertEquals(3, ExpiryReminderKt.reminderLeadDays(null));
        assertEquals(3, ExpiryReminderKt.reminderLeadDays(group(4, 4)));
        assertEquals(3, ExpiryReminderKt.reminderLeadDays(group(5, 1)));
        assertEquals(3, ExpiryReminderKt.reminderLeadDays(new ReportCategory("Chưa phân loại", 10, 0, 8, 0)));
    }
    @Test public void thresholdsAreBoundedAtThreeFiveAndSevenDays() {
        assertEquals(3, ExpiryReminderKt.reminderLeadDays(group(10, 2)));
        assertEquals(5, ExpiryReminderKt.reminderLeadDays(group(10, 3)));
        assertEquals(5, ExpiryReminderKt.reminderLeadDays(group(10, 4)));
        assertEquals(7, ExpiryReminderKt.reminderLeadDays(group(10, 5)));
        assertEquals(7, ExpiryReminderKt.reminderLeadDays(group(10, 10)));
    }
    @Test public void doesNotNotifyExpiredUnknownInvalidOrDistantDates() {
        var result = ExpiryReminderKt.expiryReminders(List.of(
            food("expired", "2026-10-18", "Rau", "receipt"),
            food("unknown", null, "Rau", "receipt"),
            food("invalid", "2026-02-30", "Rau", "receipt"),
            food("far", "2026-11-01", "Rau", "receipt")), "2026-10-19");
        assertTrue(result.isEmpty());
    }
    @Test public void remindersIncludeTodayAndDefaultBoundaryAndSortByUrgency() {
        var result = ExpiryReminderKt.expiryReminders(List.of(
            food("three", "2026-10-22", "Rau", "receipt"),
            food("today", "2026-10-19", "Rau", "receipt"),
            food("four", "2026-10-23", "Rau", "receipt"),
            food("tomorrow", "2026-10-20", "Rau", "receipt")), "2026-10-19");
        assertEquals(List.of("today", "tomorrow", "three"), result.stream().map(r -> r.getFood().getId()).toList());
        assertTrue(result.get(0).getMessage().contains("hôm nay"));
        assertTrue(result.get(1).getMessage().contains("ngày mai"));
    }
    @Test public void riskyCategoryUsesReportCohortButDoesNotAffectOtherCategories() {
        var rows = new ArrayList<Food>();
        for (int i = 0; i < 3; i++) rows.add(food("expired" + i, "2026-10-18", "Rau", "receipt"));
        rows.add(food("early", "2026-10-26", "Rau", "receipt"));
        rows.add(food("distant", "2026-11-01", "Rau", "product"));
        rows.add(food("other", "2026-10-26", "Thịt", "receipt"));
        var result = ExpiryReminderKt.expiryReminders(rows, "2026-10-19");
        assertEquals(1, result.size());
        assertEquals("early", result.get(0).getFood().getId());
        assertEquals(7, result.get(0).getLeadDays());
        assertTrue(result.get(0).getPersonalized());
    }
    @Test public void legacyRowsDoNotInflatePersonalizationAndDuplicatesDoNotInflateSample() {
        var rows = new ArrayList<Food>();
        for (int i = 0; i < 8; i++) rows.add(food("legacy" + i, "2026-10-18", "Rau", null));
        rows.add(food("future", "2026-10-24", "Rau", "receipt"));
        assertTrue(ExpiryReminderKt.expiryReminders(rows, "2026-10-19").isEmpty());
        var repeated = food("one", "2026-10-18", "Rau", "receipt");
        assertTrue(ExpiryReminderKt.expiryReminders(List.of(repeated, repeated, repeated, repeated,
            food("new", "2026-10-24", "Rau", "receipt")), "2026-10-19").isEmpty());
    }
    @Test public void emptyQuantityIsNotSuggestedAndDuplicateCandidatesAreCollapsed() {
        var zero = new Food("zero", "zero", 0.0, "kg", null, null, "2026-10-19", null, null, null, null, "Rau", "receipt");
        var one = food("one", "2026-10-19", "Rau", "receipt");
        assertEquals(1, ExpiryReminderKt.expiryReminders(List.of(zero, one, one), "2026-10-19").size());
    }
    @Test public void monthYearAndLeapDayBoundariesUseCalendarDays() {
        var result = ExpiryReminderKt.expiryReminders(List.of(food("newyear", "2027-01-01", null, null)), "2026-12-30");
        assertEquals(2, result.get(0).getDaysLeft());
        assertEquals(1, ExpiryReminderKt.expiryReminders(List.of(food("leap", "2028-02-29", null, null)), "2028-02-28").get(0).getDaysLeft());
        assertTrue(ExpiryReminderKt.expiryReminders(List.of(food("one", "2026-10-20", null, null)), "invalid").isEmpty());
    }
}
