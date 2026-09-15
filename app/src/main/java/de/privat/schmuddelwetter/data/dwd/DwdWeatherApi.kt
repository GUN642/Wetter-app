package de.privat.schmuddelwetter.data.dwd

import de.privat.schmuddelwetter.data.dwd.model.DwdStation
import de.privat.schmuddelwetter.data.dwd.model.MosmixForecast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.zip.ZipInputStream

/** Lädt die MOSMIX_S-Einzelstationsvorhersage (KMZ) von opendata.dwd.de. */
class DwdWeatherApi(private val client: OkHttpClient) {

    suspend fun fetchMosmix(station: DwdStation): MosmixForecast = withContext(Dispatchers.IO) {
        val url = "https://opendata.dwd.de/weather/local_forecasts/mos/MOSMIX_S/single_stations/" +
            "${station.id}/kml/MOSMIX_S_LATEST_${station.id}.kmz"
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("DWD-MOSMIX-Abruf fehlgeschlagen: HTTP ${response.code}")
            }
            val bytes = response.body?.bytes() ?: throw IOException("Leere Antwort von DWD")
            val kmlBytes = extractKmlFromKmz(bytes)
            MosmixKmlParser.parse(ByteArrayInputStream(kmlBytes), station)
        }
    }

    private fun extractKmlFromKmz(kmzBytes: ByteArray): ByteArray {
        ZipInputStream(ByteArrayInputStream(kmzBytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name.endsWith(".kml", ignoreCase = true)) {
                    return zip.readBytes()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        throw IOException("Keine KML-Datei im DWD-Archiv gefunden")
    }
}
