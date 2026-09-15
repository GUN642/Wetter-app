package de.privat.schmuddelwetter.data.metar

import android.content.Context
import de.privat.schmuddelwetter.data.metar.model.Airport
import kotlinx.serialization.json.Json
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Kuratierte, gebündelte Flughafenliste (ICAO-Code, Name, Koordinaten) für Suche
 * und Umkreissuche. Ein beliebiger ICAO-Code kann unabhängig davon jederzeit
 * direkt eingegeben werden – der Katalog dient nur dem Komfort.
 */
class AirportRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private var cached: List<Airport>? = null

    fun getAll(): List<Airport> {
        cached?.let { return it }
        val text = context.assets.open("airports.json").bufferedReader().use { it.readText() }
        val list = json.decodeFromString<List<Airport>>(text)
        cached = list
        return list
    }

    fun search(query: String): List<Airport> {
        if (query.isBlank()) return getAll()
        val q = query.trim().uppercase()
        return getAll().filter { it.icao.contains(q) || it.name.uppercase().contains(q) }
    }

    fun findNearest(lat: Double, lon: Double): Airport? =
        getAll().minByOrNull { haversineKm(lat, lon, it.lat, it.lon) }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
