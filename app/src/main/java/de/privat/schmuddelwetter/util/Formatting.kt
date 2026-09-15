package de.privat.schmuddelwetter.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val hourFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.GERMANY)
private val weekdayFormatter = DateTimeFormatter.ofPattern("EE", Locale.GERMANY)
private val dayMonthFormatter = DateTimeFormatter.ofPattern("d.M.", Locale.GERMANY)

fun Instant.toLocalHourString(zone: ZoneId = ZoneId.systemDefault()): String =
    hourFormatter.format(this.atZone(zone))

fun LocalDate.toWeekdayString(): String = weekdayFormatter.format(this).replaceFirstChar { it.uppercase() }

fun LocalDate.toDayMonthString(): String = dayMonthFormatter.format(this)

fun Double?.roundedOrDash(suffix: String = ""): String =
    if (this == null) "–" else "${roundToInt()}$suffix"

private val COMPASS_DIRECTIONS = listOf(
    "N", "NNO", "NO", "ONO", "O", "OSO", "SO", "SSO",
    "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW",
)

fun degreesToCompass(deg: Double?): String {
    if (deg == null) return "–"
    val index = (((deg % 360.0) / 22.5) + 0.5).toInt() % 16
    return COMPASS_DIRECTIONS[if (index < 0) index + 16 else index]
}

fun visibilityMetersToText(meters: Double?): String = when {
    meters == null -> "–"
    meters >= 10000 -> "≥10 km"
    meters >= 1000 -> "%.1f km".format(meters / 1000.0)
    else -> "${meters.roundToInt()} m"
}
