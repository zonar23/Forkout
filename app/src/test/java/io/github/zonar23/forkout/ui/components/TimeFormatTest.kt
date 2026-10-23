package io.github.zonar23.forkout.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatTest {

    @Test
    fun formatMinutesSeconds() {
        assertEquals("0:00", TimeFormat.formatMinutesSeconds(0))
        assertEquals("0:45", TimeFormat.formatMinutesSeconds(45))
        assertEquals("1:30", TimeFormat.formatMinutesSeconds(90))
        assertEquals("10:05", TimeFormat.formatMinutesSeconds(605))
        assertEquals("0:00", TimeFormat.formatMinutesSeconds(-5))
    }

    @Test
    fun formatDate_usesGermanWeekdayAndDate() {
        assertEquals(
            "Fr., 23.10.2026",
            DateFormat.formatDate(java.time.Instant.parse("2026-10-23T10:00:00Z"), java.time.ZoneOffset.UTC),
        )
    }
}
