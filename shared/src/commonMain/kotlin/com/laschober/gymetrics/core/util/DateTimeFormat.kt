package com.laschober.gymetrics.core.util

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
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
