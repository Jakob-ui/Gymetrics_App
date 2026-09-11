package com.laschober.gymetrics.core.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private val dateTimeFormat = LocalDateTime.Format {
    day()
    char('.')
    monthNumber()
    char('.')
    year()
}


@OptIn(ExperimentalTime::class)
fun formatDateTime(iso: String): String = try {
    val localDateTime = Instant.parse(iso).toLocalDateTime(TimeZone.currentSystemDefault())
    dateTimeFormat.format(localDateTime)
} catch (e: Exception) {
    iso
}

// Parses a backend ISO timestamp down to just the calendar date (device-local timezone), or
// null if it isn't parseable. Used to bucket trainings by day in the Planning week view.
@OptIn(ExperimentalTime::class)
fun parseLocalDate(iso: String): LocalDate? = try {
    Instant.parse(iso).toLocalDateTime(TimeZone.currentSystemDefault()).date
} catch (e: Exception) {
    null
}

@OptIn(ExperimentalTime::class)
fun todayLocalDate(): LocalDate =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
