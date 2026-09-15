package de.privat.schmuddelwetter.data.metar

import de.privat.schmuddelwetter.data.metar.model.Metar
import de.privat.schmuddelwetter.data.metar.model.Taf

class AviationWeatherRepository(private val api: AviationWeatherApi) {

    suspend fun getMetar(icao: String): Result<Metar> = try {
        val raw = api.fetchRawMetar(icao)
        val firstLine = raw.lineSequence().firstOrNull { it.isNotBlank() } ?: raw
        Result.success(MetarTafParser.parseMetar(firstLine))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getTaf(icao: String): Result<Taf> = try {
        val raw = api.fetchRawTaf(icao)
        Result.success(MetarTafParser.parseTaf(raw))
    } catch (e: Exception) {
        Result.failure(e)
    }
}
