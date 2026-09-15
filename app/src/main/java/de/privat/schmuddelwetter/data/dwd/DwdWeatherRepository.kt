package de.privat.schmuddelwetter.data.dwd

import de.privat.schmuddelwetter.data.dwd.model.MosmixForecast

/** Dünne Fassade um [DwdWeatherApi] mit einheitlicher Fehlerbehandlung als [Result]. */
class DwdWeatherRepository(private val api: DwdWeatherApi) {
    suspend fun forecastForLocation(lat: Double, lon: Double): Result<MosmixForecast> = try {
        Result.success(api.fetchNearestStationForecast(lat, lon))
    } catch (e: Exception) {
        Result.failure(e)
    }
}
