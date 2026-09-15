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

/**
 * Parst eine DWD-MOSMIX_S-Einzelstations-KML-Datei (eine Placemark je Datei,
 * ein `dwd:Forecast`-Block je Wetterelement mit einer whitespace-separierten
 * Werteliste, deren Index sich auf die gemeinsame `dwd:ForecastTimeSteps`-Liste
 * bezieht). Fehlende Werte sind in MOSMIX als "-" kodiert.
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

    fun parse(input: InputStream, station: DwdStation): MosmixForecast {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(input, "UTF-8")

        val timeSteps = mutableListOf<Instant>()
        val elementValues = mutableMapOf<String, List<Double?>>()
        var issuedAt: Instant? = null

        var inForecastTimeSteps = false
        var inPlacemark = false
        var placemarkDone = false
        var currentElementName: String? = null
        var currentValuesText: StringBuilder? = null

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "dwd:IssueTime" -> {
                        issuedAt = parseIsoInstant(parser.nextText())
                    }
                    "dwd:ForecastTimeSteps" -> inForecastTimeSteps = true
                    "dwd:TimeStep" -> if (inForecastTimeSteps) {
                        parseIsoInstant(parser.nextText())?.let { timeSteps += it }
                    }
                    "kml:Placemark" -> if (!placemarkDone) inPlacemark = true
                    "dwd:Forecast" -> if (inPlacemark) {
                        currentElementName = parser.getAttributeValue(null, "dwd:elementName")
                        currentValuesText = StringBuilder()
                    }
                    "dwd:value" -> currentValuesText?.append(parser.nextText())
                }
                XmlPullParser.END_TAG -> when (parser.name) {
                    "dwd:ForecastTimeSteps" -> inForecastTimeSteps = false
                    "dwd:Forecast" -> {
                        val name = currentElementName
                        val text = currentValuesText?.toString()
                        if (name != null && text != null) {
                            elementValues[name] = parseValueList(text)
                        }
                        currentElementName = null
                        currentValuesText = null
                    }
                    "kml:Placemark" -> if (inPlacemark) {
                        inPlacemark = false
                        placemarkDone = true
                    }
                }
            }
            eventType = parser.next()
        }

        val hourly = buildTimeSteps(timeSteps, elementValues)
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
}
