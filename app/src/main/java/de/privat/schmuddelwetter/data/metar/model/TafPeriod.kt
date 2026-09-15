package de.privat.schmuddelwetter.data.metar.model

import de.privat.schmuddelwetter.util.WeatherCondition

data class TafPeriod(
    /** Menschenlesbares Label, z. B. "Grundvorhersage", "BECMG", "TEMPO", "Ab 15. 20:00 UTC". */
    val label: String,
    /** Rohe Gültigkeitsgruppe "DDHH/DDHH", falls vorhanden. */
    val validityRaw: String?,
    val wind: Wind?,
    val visibilityM: Int?,
    val cavok: Boolean,
    val weather: List<WeatherPhenomenon>,
    val clouds: List<CloudLayer>,
    val ceilingFt: Int?,
    val condition: WeatherCondition,
    val raw: String,
)

data class Taf(
    val icao: String,
    val issueTimeRaw: String?,
    val validityRaw: String?,
    val periods: List<TafPeriod>,
    val raw: String,
)
