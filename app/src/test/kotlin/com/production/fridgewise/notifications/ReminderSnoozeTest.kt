package com.production.fridgewise.notifications

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderSnoozeTest {
    @Test
    fun snoozeSuppressesPostingUntilDeadline() {
        val now = 1_000_000L
        assertFalse(ReminderSnapshot().snoozed(now))
        assertTrue(ReminderSnapshot(snoozeUntil = now + 1).snoozed(now))
        assertFalse(ReminderSnapshot(snoozeUntil = now + 1).snoozed(now + 1))
    }
}
