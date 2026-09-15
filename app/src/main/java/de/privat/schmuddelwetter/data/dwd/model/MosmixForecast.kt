package de.privat.schmuddelwetter.data.dwd.model

import de.privat.schmuddelwetter.util.WeatherCondition
import java.time.Instant

/** Ein einzelner Vorhersage-Zeitschritt für eine MOSMIX-Station. */
data class MosmixTimeStep(
    val time: Instant,
    val tempC: Double?,
    val dewpointC: Double?,
    val windSpeedKmh: Double?,
    val windGustKmh: Double?,
    val windDirDeg: Double?,
    val precipMm: Double?,
    val precipProbabilityPercent: Double?,
    val cloudCoverPercent: Double?,
    val pressureHpa: Double?,
    val visibilityM: Double?,
    val weatherCode: Int?,
    val condition: WeatherCondition,
)

/** Für einen Kalendertag aggregierte Werte, aus den stündlichen Zeitschritten gebildet. */
data class MosmixDaySummary(
    val date: java.time.LocalDate,
    val minTempC: Double?,
    val maxTempC: Double?,
    val totalPrecipMm: Double?,
    val maxPrecipProbabilityPercent: Double?,
    val dominantCondition: WeatherCondition,
)

data class MosmixForecast(
    val station: DwdStation,
    val issuedAt: Instant,
    val hourly: List<MosmixTimeStep>,
    val daily: List<MosmixDaySummary>,
) {
    /** Zeitschritt, der der aktuellen Uhrzeit am nächsten liegt – dient als "aktuelles Wetter". */
    fun currentTimeStep(now: Instant = Instant.now()): MosmixTimeStep? =
        hourly.minByOrNull { kotlin.math.abs(it.time.epochSecond - now.epochSecond) }
}
