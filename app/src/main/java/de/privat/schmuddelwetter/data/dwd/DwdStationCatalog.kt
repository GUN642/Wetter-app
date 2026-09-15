package de.privat.schmuddelwetter.data.dwd

import android.content.Context
import de.privat.schmuddelwetter.data.dwd.model.DwdStation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Lädt und cached den offiziellen DWD-MOSMIX-Stationskatalog (Liste aller Stationen
 * mit ID, ICAO-Kürzel und Koordinaten) und findet die nächstgelegene Station zu
 * einer gegebenen Position.
 *
 * Der Katalog wird lokal zwischengespeichert, damit die App nicht bei jedem Start
 * neu laden muss und auch offline (mit ggf. veralteten Daten) funktioniert.
 */
class DwdStationCatalog(
    private val context: Context,
    private val client: OkHttpClient,
) {
    private val cacheFile: File get() = File(context.filesDir, "dwd_stations_cache.json")
    private val json = Json { ignoreUnknownKeys = true }

    private var inMemory: List<DwdStation>? = null

    private val catalogUrl =
        "https://opendata.dwd.de/weather/local_forecasts/mos/MOSMIX_S/mosmix_stationskatalog.cfg"

    suspend fun findNearestStation(lat: Double, lon: Double, maxDistanceKm: Double = 150.0): DwdStation? {
        val stations = getStations()
        return stations
            .map { it to haversineKm(lat, lon, it.lat, it.lon) }
            .filter { it.second <= maxDistanceKm }
            .minByOrNull { it.second }
            ?.first
    }

    suspend fun getStations(): List<DwdStation> {
        inMemory?.let { return it }
        val cached = readCache()
        if (cached != null) {
            inMemory = cached
            // Im Hintergrund aktualisieren, aber die App nicht blockieren.
            return cached
        }
        val fresh = fetchAndParse()
        if (fresh.isNotEmpty()) {
            inMemory = fresh
            writeCache(fresh)
        }
        return fresh
    }

    suspend fun refresh(): List<DwdStation> = withContext(Dispatchers.IO) {
        val fresh = fetchAndParse()
        if (fresh.isNotEmpty()) {
            inMemory = fresh
            writeCache(fresh)
        }
        fresh
    }

    private suspend fun fetchAndParse(): List<DwdStation> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(catalogUrl).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                parseCatalog(body)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun readCache(): List<DwdStation>? {
        return try {
            if (!cacheFile.exists()) return null
            json.decodeFromString<List<DwdStation>>(cacheFile.readText())
        } catch (e: Exception) {
            null
        }
    }

    private fun writeCache(stations: List<DwdStation>) {
        try {
            cacheFile.writeText(json.encodeToString(stations))
        } catch (e: Exception) {
            // Cache ist ein reiner Optimierungspfad – Fehler beim Schreiben sind unkritisch.
        }
    }

    /**
     * Parst die textbasierte Stationstabelle. Das Format besteht aus einer Kopf-/
     * Trennzeile gefolgt von Datenzeilen, deren Felder durch Leerzeichen getrennt sind:
     * ID, ICAO (oder "----"), NAME (Leerzeichen als "_"), LAT, LON, HÖHE.
     * Zur Robustheit werden LAT/LON/HÖHE von rechts, die ID von links gelesen.
     */
    internal fun parseCatalog(raw: String): List<DwdStation> {
        val result = mutableListOf<DwdStation>()
        for (line in raw.lineSequence()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            val tokens = trimmed.split(Regex("\\s+"))
            if (tokens.size < 6) continue
            val id = tokens[0]
            if (!id.matches(Regex("\\d{4,5}"))) continue

            val elevation = tokens.last().toIntOrNull() ?: continue
            val lon = tokens[tokens.size - 2].toDoubleOrNull() ?: continue
            val lat = tokens[tokens.size - 3].toDoubleOrNull() ?: continue

            val nameTokens = tokens.subList(1, tokens.size - 3)
            if (nameTokens.isEmpty()) continue

            val icaoCandidate = nameTokens.first()
            val icao = if (icaoCandidate.matches(Regex("[A-Z]{4}"))) icaoCandidate else null
            val nameStartIndex = if (icao != null) 1 else 0
            val name = nameTokens.drop(nameStartIndex)
                .joinToString(" ")
                .replace('_', ' ')
                .trim()
            if (name.isEmpty()) continue

            result += DwdStation(id = id, icao = icao, name = name, lat = lat, lon = lon, elevationM = elevation)
        }
        return result
    }

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
