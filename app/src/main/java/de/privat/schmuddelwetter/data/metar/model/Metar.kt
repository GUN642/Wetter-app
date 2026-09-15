package de.privat.schmuddelwetter.data.metar.model

import de.privat.schmuddelwetter.util.WeatherCondition

data class Metar(
    val icao: String,
    val raw: String,
    /** Rohe DDHHMMZ-Zeitgruppe, z. B. "151720Z" (Tag 15, 17:20 UTC). */
    val observationTimeRaw: String?,
    val isAuto: Boolean,
    val wind: Wind?,
    val visibilityM: Int?,
    val cavok: Boolean,
    val weather: List<WeatherPhenomenon>,
    val clouds: List<CloudLayer>,
    val verticalVisibilityFt: Int?,
    val ceilingFt: Int?,
    val tempC: Int?,
    val dewpointC: Int?,
    val qnhHpa: Int?,
    val condition: WeatherCondition,
    val flightCategory: FlightCategory,
)
