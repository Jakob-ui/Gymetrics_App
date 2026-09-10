package com.laschober.gymetrics.ui.main.logbook

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private val trainingDateFormat = LocalDateTime.Format {
    day()
    char('.')
    monthNumber()
    char('.')
    year()
    chars(", ")
    hour()
    char(':')
    minute()
}

// Backend sends the raw ISO-8601 instant (e.g. "2026-09-10T13:59:55.995Z") - this turns it into
// something readable in the device's local time zone, e.g. "10.09.2026, 15:59".
// Falls back to the raw string if parsing fails, so a malformed date never crashes the screen.
@OptIn(ExperimentalTime::class)
fun formatTrainingDate(iso: String): String = try {
    val localDateTime = Instant.parse(iso).toLocalDateTime(TimeZone.currentSystemDefault())
    trainingDateFormat.format(localDateTime)
} catch (e: Exception) {
    iso
}
