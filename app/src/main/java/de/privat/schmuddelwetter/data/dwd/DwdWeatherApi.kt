package de.privat.schmuddelwetter.data.dwd

import de.privat.schmuddelwetter.data.dwd.model.MosmixForecast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedInputStream
import java.io.IOException
import java.util.zip.ZipInputStream

/**
 * Lädt die DWD-MOSMIX_S-Sammeldatei (alle Stationen in einer Datei, keine
 * Einzelstations-Endpunkte mehr) und wertet sie im Streaming-Verfahren aus,
 * ohne die – entpackt mehrere hundert MB große – KML jemals komplett im
 * Speicher zu halten.
 */
class DwdWeatherApi(private val client: OkHttpClient) {

    private val allStationsUrl =
        "https://opendata.dwd.de/weather/local_forecasts/mos/MOSMIX_S/all_stations/kml/MOSMIX_S_LATEST_240.kmz"

    suspend fun fetchNearestStationForecast(lat: Double, lon: Double): MosmixForecast =
        withContext(Dispatchers.IO) {
            val request = Request.Builder().url(allStationsUrl).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("DWD-MOSMIX-Abruf fehlgeschlagen: HTTP ${response.code}")
                }
                val body = response.body ?: throw IOException("Leere Antwort von DWD")
                ZipInputStream(BufferedInputStream(body.byteStream())).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (entry.name.endsWith(".kml", ignoreCase = true)) {
                            return@withContext MosmixKmlParser.parseNearestStation(zip, lat, lon)
                                ?: throw IOException("Keine DWD-Station in der Nähe gefunden")
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                    throw IOException("Keine KML-Datei im DWD-Archiv gefunden")
                }
            }
        }
}
