package de.privat.schmuddelwetter.data.metar.model

import kotlinx.serialization.Serializable

@Serializable
data class Airport(
    val icao: String,
    val name: String,
    val lat: Double,
    val lon: Double,
)
