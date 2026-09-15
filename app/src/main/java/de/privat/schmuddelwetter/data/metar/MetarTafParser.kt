package de.privat.schmuddelwetter.data.metar

import de.privat.schmuddelwetter.data.metar.model.CloudCoverage
import de.privat.schmuddelwetter.data.metar.model.CloudLayer
import de.privat.schmuddelwetter.data.metar.model.FlightCategoryCalculator
import de.privat.schmuddelwetter.data.metar.model.Intensity
import de.privat.schmuddelwetter.data.metar.model.Metar
import de.privat.schmuddelwetter.data.metar.model.Taf
import de.privat.schmuddelwetter.data.metar.model.TafPeriod
import de.privat.schmuddelwetter.data.metar.model.WeatherPhenomenon
import de.privat.schmuddelwetter.data.metar.model.Wind
import de.privat.schmuddelwetter.util.WeatherCondition

/**
 * Reiner Text-Parser für METAR/TAF-Rohmeldungen (unabhängig von einem bestimmten
 * API-JSON-Schema). Jeder Token wird gegen bekannte Gruppenformate geprüft; nicht
 * erkannte Token werden ignoriert – die Rohmeldung bleibt immer zusätzlich sichtbar.
 */
object MetarTafParser {

    private val windRegex = Regex("^(\\d{3}|VRB)(\\d{2,3})(G(\\d{2,3}))?(KT|MPS|KMH)$")
    private val windVarRegex = Regex("^(\\d{3})V(\\d{3})$")
    private val visMetersRegex = Regex("^(\\d{4})$")
    private val visSmRegex = Regex("^([MP])?(\\d+)SM$")
    private val visFractionSmRegex = Regex("^([MP])?(\\d+)/(\\d+)SM$")
    private val cloudRegex = Regex("^(FEW|SCT|BKN|OVC)(\\d{3})(CB|TCU)?$")
    private val vvRegex = Regex("^VV(\\d{3}|///)$")
    private val noCloudRegex = Regex("^(SKC|CLR|NSC|NCD)$")
    private val tempDewRegex = Regex("^(M?\\d{2})/(M?\\d{2})$")
    private val qnhRegex = Regex("^Q(\\d{4})$")
    private val altimeterRegex = Regex("^A(\\d{4})$")
    private val weatherRegex = Regex(
        "^(-|\\+|VC)?(MI|PR|BC|DR|BL|SH|TS|FZ)?" +
            "((?:DZ|RA|SN|SG|IC|PL|GR|GS|UP|BR|FG|FU|VA|DU|SA|HZ|PY|PO|SQ|FC|SS|DS){1,3})$",
    )
    private val obsTimeRegex = Regex("^\\d{6}Z$")
    private val fmGroupRegex = Regex("^FM\\d{6}$")
    private val probRegex = Regex("^PROB\\d{2}$")
    private val validityRangeRegex = Regex("^\\d{4}/\\d{4}$")
    private val icaoRegex = Regex("^[A-Z]{4}$")

    // ---------------------------------------------------------------- METAR

    fun parseMetar(raw: String): Metar {
        val clean = normalize(raw)
        val tokens = clean.split(Regex("\\s+")).filter { it.isNotBlank() }
        val icao = tokens.firstOrNull { icaoRegex.matches(it) } ?: tokens.getOrElse(0) { "----" }
        val obsTimeRaw = tokens.firstOrNull { obsTimeRegex.matches(it) }
        val isAuto = tokens.contains("AUTO")

        val body = parseWeatherBody(tokens)
        val flightCategory = FlightCategoryCalculator.calculate(body.visibilityM, body.ceilingFt)

        return Metar(
            icao = icao,
            raw = clean,
            observationTimeRaw = obsTimeRaw,
            isAuto = isAuto,
            wind = body.wind,
            visibilityM = body.visibilityM,
            cavok = body.cavok,
            weather = body.weather,
            clouds = body.clouds,
            verticalVisibilityFt = body.verticalVisibilityFt,
            ceilingFt = body.ceilingFt,
            tempC = body.tempC,
            dewpointC = body.dewpointC,
            qnhHpa = body.qnhHpa,
            condition = body.condition,
            flightCategory = flightCategory,
        )
    }

    // ------------------------------------------------------------------ TAF

    fun parseTaf(raw: String): Taf {
        val clean = normalize(raw)
        var tokens = clean.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.firstOrNull() == "TAF") tokens = tokens.drop(1)
        while (tokens.firstOrNull() == "AMD" || tokens.firstOrNull() == "COR") tokens = tokens.drop(1)

        val icao = tokens.getOrNull(0)?.takeIf { icaoRegex.matches(it) } ?: tokens.getOrElse(0) { "----" }
        tokens = tokens.drop(1)

        val issueTimeRaw = tokens.getOrNull(0)?.takeIf { obsTimeRegex.matches(it) }
        if (issueTimeRaw != null) tokens = tokens.drop(1)

        val validityRaw = tokens.getOrNull(0)?.takeIf { validityRangeRegex.matches(it) }
        if (validityRaw != null) tokens = tokens.drop(1)

        val periods = mutableListOf<TafPeriod>()
        var currentLabel = "Grundvorhersage"
        var currentValidity: String? = validityRaw
        var currentTokens = mutableListOf<String>()

        fun flush() {
            if (currentTokens.isNotEmpty() || periods.isEmpty()) {
                val body = parseWeatherBody(currentTokens)
                periods += TafPeriod(
                    label = currentLabel,
                    validityRaw = currentValidity,
                    wind = body.wind,
                    visibilityM = body.visibilityM,
                    cavok = body.cavok,
                    weather = body.weather,
                    clouds = body.clouds,
                    ceilingFt = body.ceilingFt,
                    condition = body.condition,
                    raw = currentTokens.joinToString(" "),
                )
            }
        }

        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]
            when {
                fmGroupRegex.matches(token) -> {
                    flush()
                    currentLabel = "Ab " + formatFmTime(token)
                    currentValidity = null
                    currentTokens = mutableListOf()
                }
                token == "BECMG" -> {
                    flush()
                    currentLabel = "Übergang (BECMG)"
                    val next = tokens.getOrNull(i + 1)
                    if (next != null && validityRangeRegex.matches(next)) {
                        currentValidity = next
                        i++
                    } else {
                        currentValidity = null
                    }
                    currentTokens = mutableListOf()
                }
                token == "TEMPO" -> {
                    flush()
                    currentLabel = "Vorübergehend (TEMPO)"
                    val next = tokens.getOrNull(i + 1)
                    if (next != null && validityRangeRegex.matches(next)) {
                        currentValidity = next
                        i++
                    } else {
                        currentValidity = null
                    }
                    currentTokens = mutableListOf()
                }
                probRegex.matches(token) -> {
                    flush()
                    val prob = token.removePrefix("PROB")
                    var label = "$prob % Wahrscheinlichkeit"
                    var lookAhead = i + 1
                    if (tokens.getOrNull(lookAhead) == "TEMPO") {
                        label += " (TEMPO)"
                        lookAhead++
                    }
                    currentLabel = label
                    val validityToken = tokens.getOrNull(lookAhead)
                    if (validityToken != null && validityRangeRegex.matches(validityToken)) {
                        currentValidity = validityToken
                        lookAhead++
                    } else {
                        currentValidity = null
                    }
                    i = lookAhead - 1
                    currentTokens = mutableListOf()
                }
                else -> currentTokens.add(token)
            }
            i++
        }
        flush()

        return Taf(icao = icao, issueTimeRaw = issueTimeRaw, validityRaw = validityRaw, periods = periods, raw = clean)
    }

    // ---------------------------------------------------------- gemeinsam

    private data class DecodedWeatherBody(
        val wind: Wind?,
        val visibilityM: Int?,
        val cavok: Boolean,
        val weather: List<WeatherPhenomenon>,
        val clouds: List<CloudLayer>,
        val verticalVisibilityFt: Int?,
        val ceilingFt: Int?,
        val tempC: Int?,
        val dewpointC: Int?,
        val qnhHpa: Int?,
        val condition: WeatherCondition,
    )

    private fun parseWeatherBody(tokens: List<String>): DecodedWeatherBody {
        var wind: Wind? = null
        var windVarFrom: Int? = null
        var windVarTo: Int? = null
        var visibilityM: Int? = null
        var cavok = false
        val weatherList = mutableListOf<WeatherPhenomenon>()
        val cloudList = mutableListOf<CloudLayer>()
        var verticalVisibilityFt: Int? = null
        var tempC: Int? = null
        var dewpointC: Int? = null
        var qnhHpa: Int? = null

        for (token in tokens) {
            when {
                token == "CAVOK" -> cavok = true

                windRegex.matches(token) -> wind = parseWind(token)

                windVarRegex.matches(token) -> {
                    val m = windVarRegex.matchEntire(token)!!
                    windVarFrom = m.groupValues[1].toIntOrNull()
                    windVarTo = m.groupValues[2].toIntOrNull()
                }

                visFractionSmRegex.matches(token) -> {
                    val m = visFractionSmRegex.matchEntire(token)!!
                    val num = m.groupValues[2].toDouble()
                    val den = m.groupValues[3].toDoubleOrNull()?.takeIf { it != 0.0 } ?: 1.0
                    visibilityM = ((num / den) * 1609.34).toInt()
                }

                visSmRegex.matches(token) -> {
                    val m = visSmRegex.matchEntire(token)!!
                    visibilityM = (m.groupValues[2].toDouble() * 1609.34).toInt()
                }

                visibilityM == null && visMetersRegex.matches(token) -> {
                    val meters = token.toInt()
                    visibilityM = if (meters == 9999) 10000 else meters
                }

                vvRegex.matches(token) -> {
                    val m = vvRegex.matchEntire(token)!!
                    verticalVisibilityFt = m.groupValues[1].toIntOrNull()?.times(100)
                }

                noCloudRegex.matches(token) -> Unit

                cloudRegex.matches(token) -> {
                    val m = cloudRegex.matchEntire(token)!!
                    val coverage = CloudCoverage.valueOf(m.groupValues[1])
                    val heightFt = m.groupValues[2].toInt() * 100
                    val special = m.groupValues[3]
                    cloudList += CloudLayer(
                        coverage = coverage,
                        heightFt = heightFt,
                        isCumulonimbus = special == "CB",
                        isToweringCumulus = special == "TCU",
                    )
                }

                tempDewRegex.matches(token) -> {
                    val m = tempDewRegex.matchEntire(token)!!
                    tempC = parseTemp(m.groupValues[1])
                    dewpointC = parseTemp(m.groupValues[2])
                }

                qnhRegex.matches(token) -> qnhHpa = qnhRegex.matchEntire(token)!!.groupValues[1].toIntOrNull()

                altimeterRegex.matches(token) -> {
                    val inHg = altimeterRegex.matchEntire(token)!!.groupValues[1].toDouble() / 100.0
                    qnhHpa = (inHg * 33.8639).toInt()
                }

                token != "NSW" && weatherRegex.matches(token) -> weatherList += parseWeatherToken(token)
            }
        }

        if (cavok && visibilityM == null) visibilityM = 10000

        if (wind != null && (windVarFrom != null || windVarTo != null)) {
            wind = wind.copy(variableFromDeg = windVarFrom, variableToDeg = windVarTo)
        }

        val ceilingFt = cloudList
            .filter { it.coverage == CloudCoverage.BKN || it.coverage == CloudCoverage.OVC }
            .minOfOrNull { it.heightFt }
            ?: verticalVisibilityFt

        val condition = deriveCondition(weatherList, cavok, cloudList)

        return DecodedWeatherBody(
            wind = wind,
            visibilityM = visibilityM,
            cavok = cavok,
            weather = weatherList,
            clouds = cloudList,
            verticalVisibilityFt = verticalVisibilityFt,
            ceilingFt = ceilingFt,
            tempC = tempC,
            dewpointC = dewpointC,
            qnhHpa = qnhHpa,
            condition = condition,
        )
    }

    private fun parseWind(token: String): Wind {
        val m = windRegex.matchEntire(token)!!
        val dirRaw = m.groupValues[1]
        val speedRaw = m.groupValues[2].toDouble()
        val gustRaw = m.groupValues[4].toDoubleOrNull()
        val unit = m.groupValues[5]

        fun toKt(v: Double) = when (unit) {
            "MPS" -> v * 1.94384
            "KMH" -> v * 0.539957
            else -> v
        }

        return Wind(
            directionDeg = if (dirRaw == "VRB") null else dirRaw.toIntOrNull(),
            isVariableDirection = dirRaw == "VRB",
            speedKt = toKt(speedRaw).toInt(),
            gustKt = gustRaw?.let { toKt(it).toInt() },
        )
    }

    private fun parseTemp(raw: String): Int =
        if (raw.startsWith("M")) -raw.substring(1).toInt() else raw.toInt()

    private fun parseWeatherToken(token: String): WeatherPhenomenon {
        val m = weatherRegex.matchEntire(token)!!
        val intensityRaw = m.groupValues[1]
        val descriptor = m.groupValues[2].ifEmpty { null }
        val phenomena = m.groupValues[3].chunked(2)
        val intensity = when (intensityRaw) {
            "-" -> Intensity.LIGHT
            "+" -> Intensity.HEAVY
            "VC" -> Intensity.VICINITY
            else -> Intensity.MODERATE
        }
        return WeatherPhenomenon(intensity, descriptor, phenomena, token)
    }

    private fun deriveCondition(
        weather: List<WeatherPhenomenon>,
        cavok: Boolean,
        clouds: List<CloudLayer>,
    ): WeatherCondition {
        if (weather.isNotEmpty()) {
            val hasThunder = weather.any { it.descriptor == "TS" }
            if (hasThunder) return WeatherCondition.THUNDERSTORM

            val phenomena = weather.flatMap { it.phenomena }.toSet()
            val isShower = weather.any { it.descriptor == "SH" }

            return when {
                "GR" in phenomena || "GS" in phenomena -> WeatherCondition.HAIL
                ("SN" in phenomena || "SG" in phenomena || "IC" in phenomena || "PL" in phenomena) && "RA" in phenomena ->
                    WeatherCondition.SLEET
                "SN" in phenomena || "SG" in phenomena || "IC" in phenomena || "PL" in phenomena ->
                    if (isShower) WeatherCondition.SNOW_SHOWERS else WeatherCondition.SNOW
                "RA" in phenomena -> if (isShower) WeatherCondition.RAIN_SHOWERS else WeatherCondition.RAIN
                "DZ" in phenomena -> WeatherCondition.DRIZZLE
                "SS" in phenomena || "DS" in phenomena -> WeatherCondition.SANDSTORM
                "FG" in phenomena || "BR" in phenomena || "HZ" in phenomena || "FU" in phenomena ||
                    "VA" in phenomena || "DU" in phenomena || "SA" in phenomena -> WeatherCondition.FOG
                "SQ" in phenomena || "FC" in phenomena -> WeatherCondition.THUNDERSTORM
                else -> WeatherCondition.UNKNOWN
            }
        }

        if (cavok) return WeatherCondition.CLEAR

        val maxCoverage = clouds.maxByOrNull { it.coverage.ordinal }?.coverage
            ?: return WeatherCondition.CLEAR

        return when (maxCoverage) {
            CloudCoverage.FEW, CloudCoverage.SCT -> WeatherCondition.PARTLY_CLOUDY
            CloudCoverage.BKN, CloudCoverage.OVC -> WeatherCondition.CLOUDY
        }
    }

    private fun formatFmTime(token: String): String {
        val digits = token.removePrefix("FM")
        val day = digits.substring(0, 2)
        val hour = digits.substring(2, 4)
        val minute = digits.substring(4, 6)
        return "$day. $hour:$minute UTC"
    }

    private fun normalize(raw: String): String = raw.trim().removeSuffix("=").replace("\n", " ").trim()
}
