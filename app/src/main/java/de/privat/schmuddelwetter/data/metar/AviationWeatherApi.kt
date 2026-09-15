package de.privat.schmuddelwetter.data.metar

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/** Roh-Text-Client für die kostenlose, keyfreie aviationweather.gov-Datenschnittstelle. */
class AviationWeatherApi(private val client: OkHttpClient) {

    suspend fun fetchRawMetar(icao: String): String =
        fetchRaw("https://aviationweather.gov/api/data/metar?ids=${icao.uppercase()}&format=raw")

    suspend fun fetchRawTaf(icao: String): String =
        fetchRaw("https://aviationweather.gov/api/data/taf?ids=${icao.uppercase()}&format=raw")

    private suspend fun fetchRaw(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Abruf fehlgeschlagen: HTTP ${response.code}")
            }
            val body = response.body?.string()?.trim().orEmpty()
            if (body.isEmpty()) throw IOException("Keine Daten für diese Station verfügbar")
            body
        }
    }
}
