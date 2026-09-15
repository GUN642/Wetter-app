package de.privat.schmuddelwetter.data.dwd

import android.util.Xml
import de.privat.schmuddelwetter.data.dwd.model.DwdStation
import de.privat.schmuddelwetter.data.dwd.model.MosmixDaySummary
import de.privat.schmuddelwetter.data.dwd.model.MosmixForecast
import de.privat.schmuddelwetter.data.dwd.model.MosmixTimeStep
import de.privat.schmuddelwetter.util.WeatherCondition
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.time.Instant
import java.time.ZoneId
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Parst die DWD-MOSMIX_S-Sammeldatei, die inzwischen (anders als früher) *alle*
 * ca. 5600 Stationen in einer einzigen, sehr großen KML-Datei enthält (entpackt
 * mehrere hundert MB). Ein Einzelstations-Endpunkt existiert bei DWD nicht mehr.
 *
 * Damit das auf einem Handy funktioniert, wird die Datei in einem einzigen
 * Streaming-Durchlauf gelesen: Für jede Station (Placemark) werden Name und
 * Koordinaten sofort ausgewertet; nur für die bisher nächstgelegene Station
 * werden die Vorhersagewerte tatsächlich in Objekte umgewandelt und behalten,
 * alle anderen Stationen werden nach dem Vergleich sofort wieder verworfen.
 * So bleibt der Speicherbedarf unabhängig von der Dateigröße konstant klein.
 */
object MosmixKmlParser {

    private val TEMP_KEYS = listOf("TTT")
    private val DEWPOINT_KEYS = listOf("Td")
    private val WIND_SPEED_KEYS = listOf("FF")
    private val WIND_GUST_KEYS = listOf("FX1", "FXh", "FX625", "FX1h", "FX3")
    private val WIND_DIR_KEYS = listOf("DD")
    private val PRECIP_KEYS = listOf("RR1c", "RRS1c", "RR1")
    private val PRECIP_PROB_KEYS = listOf("wwP", "R101", "Rh00")
    private val CLOUD_COVER_KEYS = listOf("Neff", "N")
    private val PRESSURE_KEYS = listOf("PPPP")
    private val VISIBILITY_KEYS = listOf("VV")
    private val WEATHER_CODE_KEYS = listOf("ww", "WW")

    /** Alle Element-Codes, die wir überhaupt auswerten – alles andere wird beim Parsen übersprungen. */
    private val WANTED_ELEMENTS: Set<String> = (
        TEMP_KEYS + DEWPOINT_KEYS + WIND_SPEED_KEYS + WIND_GUST_KEYS + WIND_DIR_KEYS +
            PRECIP_KEYS + PRECIP_PROB_KEYS + CLOUD_COVER_KEYS + PRESSURE_KEYS +
            VISIBILITY_KEYS + WEATHER_CODE_KEYS
        ).toSet()

    /**
     * Liest [input] (die entpackte KML) einmal komplett durch und liefert die
     * Vorhersage für die Station, die [targetLat]/[targetLon] am nächsten liegt
     * (max. [maxDistanceKm]), oder `null`, wenn keine Station in Reichweite ist.
     */
    fun parseNearestStation(
        input: InputStream,
        targetLat: Double,
        targetLon: Double,
        maxDistanceKm: Double = 150.0,
    ): MosmixForecast? {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        // encoding=null → Auto-Erkennung anhand der XML-Deklaration (DWD liefert ISO-8859-1).
        parser.setInput(input, null)

        val timeSteps = mutableListOf<Instant>()
        var issuedAt: Instant? = null

        var inForecastTimeSteps = false
        var inPlacemark = false

        var currentStationId: String? = null
        var currentStationName: String? = null
        var currentLat: Double? = null
        var currentLon: Double? = null
        var currentElevation: Int = 0
        var currentElementName: String? = null
        var currentValuesText: StringBuilder? = null
        var currentElementValues = mutableMapOf<String, List<Double?>>()

        var bestDistanceKm = Double.MAX_VALUE
        var bestStation: DwdStation? = null
        var bestElementValues: Map<String, List<Double?>>? = null

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "dwd:IssueTime" -> issuedAt = parseIsoInstant(parser.nextText())
                    "dwd:ForecastTimeSteps" -> inForecastTimeSteps = true
                    "dwd:TimeStep" -> if (inForecastTimeSteps) {
                        parseIsoInstant(parser.nextText())?.let { timeSteps += it }
                    }
                    "kml:Placemark" -> {
                        inPlacemark = true
                        currentStationId = null
                        currentStationName = null
                        currentLat = null
                        currentLon = null
                        currentElevation = 0
                        currentElementValues = mutableMapOf()
                    }
                    "kml:name" -> if (inPlacemark) currentStationId = parser.nextText().trim()
                    "kml:description" -> if (inPlacemark) currentStationName = parser.nextText().trim()
                    "kml:coordinates" -> if (inPlacemark) {
                        val parts = parser.nextText().trim().split(",")
                        currentLon = parts.getOrNull(0)?.toDoubleOrNull()
                        currentLat = parts.getOrNull(1)?.toDoubleOrNull()
                        currentElevation = parts.getOrNull(2)?.toDoubleOrNull()?.toInt() ?: 0
                    }
                    "dwd:Forecast" -> if (inPlacemark) {
                        val elementName = parser.getAttributeValue(null, "dwd:elementName")
                        currentElementName = elementName
                        currentValuesText = if (elementName != null && elementName in WANTED_ELEMENTS) {
                            StringBuilder()
                        } else {
                            null
                        }
                    }
                    "dwd:value" -> if (inPlacemark) {
                        val target = currentValuesText
                        if (target != null) {
                            target.append(parser.nextText())
                        } else {
                            // Element ist nicht in WANTED_ELEMENTS: Text (oft tausende Zeichen,
                            // ×5600 Stationen) wird übersprungen statt per nextText() als String
                            // materialisiert zu werden – das war der eigentliche Performance-Killer.
                            skipSubtree(parser)
                        }
                    }
                    "kml:kml" -> Unit
                }
                XmlPullParser.END_TAG -> when (parser.name) {
                    "dwd:ForecastTimeSteps" -> inForecastTimeSteps = false
                    "dwd:Forecast" -> {
                        val name = currentElementName
                        val text = currentValuesText?.toString()
                        if (name != null && text != null) {
                            currentElementValues[name] = parseValueList(text)
                        }
                        currentElementName = null
                        currentValuesText = null
                    }
                    "kml:Placemark" -> {
                        val lat = currentLat
                        val lon = currentLon
                        val id = currentStationId
                        if (lat != null && lon != null && id != null) {
                            val distanceKm = haversineKm(targetLat, targetLon, lat, lon)
                            if (distanceKm <= maxDistanceKm && distanceKm < bestDistanceKm) {
                                bestDistanceKm = distanceKm
                                bestStation = DwdStation(
                                    id = id,
                                    name = currentStationName?.takeIf { it.isNotBlank() } ?: id,
                                    lat = lat,
                                    lon = lon,
                                    elevationM = currentElevation,
                                )
                                bestElementValues = currentElementValues
                            }
                        }
                        inPlacemark = false
                    }
                }
            }
            eventType = parser.next()
        }

        val station = bestStation ?: return null
        val hourly = buildTimeSteps(timeSteps, bestElementValues.orEmpty())
        val daily = buildDailySummaries(hourly)
        return MosmixForecast(
            station = station,
            issuedAt = issuedAt ?: (timeSteps.firstOrNull() ?: Instant.now()),
            hourly = hourly,
            daily = daily,
        )
    }

    private fun parseValueList(text: String): List<Double?> =
        text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.map { token ->
            if (token == "-") null else token.toDoubleOrNull()
        }

    private fun pick(elementValues: Map<String, List<Double?>>, keys: List<String>, index: Int): Double? {
        for (key in keys) {
            val value = elementValues[key]?.getOrNull(index)
            if (value != null) return value
        }
        return null
    }

    private fun buildTimeSteps(
        timeSteps: List<Instant>,
        elementValues: Map<String, List<Double?>>,
    ): List<MosmixTimeStep> {
        return timeSteps.mapIndexed { index, time ->
            val tempK = pick(elementValues, TEMP_KEYS, index)
            val dewpointK = pick(elementValues, DEWPOINT_KEYS, index)
            val windMs = pick(elementValues, WIND_SPEED_KEYS, index)
            val gustMs = pick(elementValues, WIND_GUST_KEYS, index)
            val pressurePa = pick(elementValues, PRESSURE_KEYS, index)
            val cloudCover = pick(elementValues, CLOUD_COVER_KEYS, index)
            val weatherCode = pick(elementValues, WEATHER_CODE_KEYS, index)?.toInt()

            MosmixTimeStep(
                time = time,
                tempC = tempK?.minus(273.15),
                dewpointC = dewpointK?.minus(273.15),
                windSpeedKmh = windMs?.times(3.6),
                windGustKmh = gustMs?.times(3.6),
                windDirDeg = pick(elementValues, WIND_DIR_KEYS, index),
                precipMm = pick(elementValues, PRECIP_KEYS, index),
                precipProbabilityPercent = pick(elementValues, PRECIP_PROB_KEYS, index),
                cloudCoverPercent = cloudCover,
                pressureHpa = pressurePa?.div(100.0),
                visibilityM = pick(elementValues, VISIBILITY_KEYS, index),
                weatherCode = weatherCode,
                condition = WeatherCodeMapper.fromWwCode(weatherCode, cloudCover),
            )
        }
    }

    private fun buildDailySummaries(hourly: List<MosmixTimeStep>): List<MosmixDaySummary> {
        val zone = ZoneId.systemDefault()
        val byDate = hourly.groupBy { it.time.atZone(zone).toLocalDate() }
        return byDate.entries.sortedBy { it.key }.map { (date, steps) ->
            val temps = steps.mapNotNull { it.tempC }
            val precipValues = steps.mapNotNull { it.precipMm }
            val probValues = steps.mapNotNull { it.precipProbabilityPercent }
            val middayStep = steps.minByOrNull { kotlin.math.abs(it.time.atZone(zone).hour - 12) }
            MosmixDaySummary(
                date = date,
                minTempC = temps.minOrNull(),
                maxTempC = temps.maxOrNull(),
                totalPrecipMm = if (precipValues.isEmpty()) null else precipValues.sum(),
                maxPrecipProbabilityPercent = probValues.maxOrNull(),
                dominantCondition = middayStep?.condition ?: WeatherCondition.UNKNOWN,
            )
        }
    }

    private fun parseIsoInstant(text: String): Instant? = try {
        Instant.parse(text.trim())
    } catch (e: Exception) {
        null
    }

    /**
     * Überspringt den Rest des aktuellen Elements (der Parser muss auf dem
     * START_TAG stehen), ohne dessen Textinhalt als String zu materialisieren.
     */
    private fun skipSubtree(parser: XmlPullParser) {
        var depth = 1
        while (depth > 0) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> depth++
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.END_DOCUMENT -> return
            }
        }
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
