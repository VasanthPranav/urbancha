package com.urbancha.inventory.data

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ShiftClockTest {
    private fun at(s: String) = LocalDateTime.parse(s)

    @Test fun afternoonBelongsToSameDay() =
        assertEquals("2026-11-21", ShiftClock.shiftIdFor(at("2026-11-21T15:00")))

    @Test fun lateNightBelongsToSameDay() =
        assertEquals("2026-11-21", ShiftClock.shiftIdFor(at("2026-11-21T23:59")))

    @Test fun afterMidnightBelongsToPreviousDay() =
        assertEquals("2026-11-21", ShiftClock.shiftIdFor(at("2026-11-22T02:59")))

    @Test fun threeAmStartsNextShift() =
        assertEquals("2026-11-22", ShiftClock.shiftIdFor(at("2026-11-22T03:00")))

    @Test fun rangeLabel() =
        assertEquals("21 Nov 3:00 PM – 22 Nov 3:00 AM", ShiftClock.rangeLabel("2026-11-21"))
}
