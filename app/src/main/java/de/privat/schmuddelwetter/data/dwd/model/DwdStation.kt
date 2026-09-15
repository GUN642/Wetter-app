package de.privat.schmuddelwetter.data.dwd.model

import kotlinx.serialization.Serializable

/** Ein Eintrag aus dem offiziellen DWD-MOSMIX-Stationskatalog. */
@Serializable
data class DwdStation(
    val id: String,
    val icao: String?,
    val name: String,
    val lat: Double,
    val lon: Double,
    val elevationM: Int,
)
