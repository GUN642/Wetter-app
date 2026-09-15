package de.privat.schmuddelwetter.data.dwd

import de.privat.schmuddelwetter.data.dwd.model.MosmixForecast

/** Kombiniert Stationssuche und Vorhersageabruf zu einem einzigen Aufruf pro Standort. */
class DwdWeatherRepository(
    private val stationCatalog: DwdStationCatalog,
    private val api: DwdWeatherApi,
) {
    suspend fun forecastForLocation(lat: Double, lon: Double): Result<MosmixForecast> {
        val station = stationCatalog.findNearestStation(lat, lon)
            ?: return Result.failure(IllegalStateException("Keine DWD-Station in der Nähe gefunden"))
        return try {
            Result.success(api.fetchMosmix(station))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
