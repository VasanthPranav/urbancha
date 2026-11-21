package com.urbancha.inventory.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Urban Cha's business day runs from 3:00 PM to 3:00 AM the next morning.
 *
 * A shift is named by the date it starts on ("2026-11-21" = 21 Nov 3 PM to 22 Nov 3 AM).
 * Anything recorded before 3 AM belongs to the previous date's shift; anything from
 * 3 AM onwards (including daytime prep before opening) belongs to that day's shift.
 */
object ShiftClock {
    const val START_HOUR = 15
    const val END_HOUR = 3

    fun shiftIdFor(time: LocalDateTime): String {
        val date = if (time.hour < END_HOUR) time.toLocalDate().minusDays(1) else time.toLocalDate()
        return date.toString()
    }

    fun currentShiftId(): String = shiftIdFor(LocalDateTime.now())

    fun start(shiftId: String): LocalDateTime = LocalDate.parse(shiftId).atTime(START_HOUR, 0)

    fun end(shiftId: String): LocalDateTime = LocalDate.parse(shiftId).plusDays(1).atTime(END_HOUR, 0)

    fun previous(shiftId: String): String = LocalDate.parse(shiftId).minusDays(1).toString()

    fun next(shiftId: String): String = LocalDate.parse(shiftId).plusDays(1).toString()

    private val dayFmt = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH)
    private val shortFmt = DateTimeFormatter.ofPattern("d MMM h:mm a", Locale.ENGLISH)

    /** "Sat, 21 Nov" */
    fun dayLabel(shiftId: String): String = LocalDate.parse(shiftId).format(dayFmt)

    /** "21 Nov 3:00 PM – 22 Nov 3:00 AM" */
    fun rangeLabel(shiftId: String): String =
        "${start(shiftId).format(shortFmt)} – ${end(shiftId).format(shortFmt)}"
}
