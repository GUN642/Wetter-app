package de.privat.schmuddelwetter.data.metar.model

enum class Intensity { LIGHT, MODERATE, HEAVY, VICINITY }

/** Ein einzelner METAR/TAF-Wettercode-Token, z. B. "-SHRA" oder "+TSRA". */
data class WeatherPhenomenon(
    val intensity: Intensity,
    val descriptor: String?,
    val phenomena: List<String>,
    val raw: String,
)

private val PHENOMENON_NAMES_DE = mapOf(
    "DZ" to "Nieselregen",
    "RA" to "Regen",
    "SN" to "Schnee",
    "SG" to "Schneegriesel",
    "IC" to "Eiskristalle",
    "PL" to "Eiskörner",
    "GR" to "Hagel",
    "GS" to "Graupel",
    "UP" to "unbek. Niederschlag",
    "BR" to "Dunst",
    "FG" to "Nebel",
    "FU" to "Rauch",
    "VA" to "Vulkanasche",
    "DU" to "Staub",
    "SA" to "Sand",
    "HZ" to "Trübung",
    "PY" to "Gischt",
    "PO" to "Staubteufel",
    "SQ" to "Böenwalze",
    "FC" to "Trichterwolke/Tornado",
    "SS" to "Sandsturm",
    "DS" to "Staubsturm",
)

private val DESCRIPTOR_NAMES_DE = mapOf(
    "MI" to "flach",
    "PR" to "teilweise",
    "BC" to "in Schwaden",
    "DR" to "tief treibend",
    "BL" to "aufgewirbelt",
    "SH" to "Schauer",
    "TS" to "Gewitter",
    "FZ" to "gefrierend",
)

fun WeatherPhenomenon.toGermanDescription(): String {
    val intensityPrefix = when (intensity) {
        Intensity.LIGHT -> "leicht"
        Intensity.HEAVY -> "stark"
        Intensity.VICINITY -> "in der Nähe:"
        Intensity.MODERATE -> null
    }
    val descriptorText = descriptor?.let { DESCRIPTOR_NAMES_DE[it] ?: it }
    val phenomenaText = phenomena.joinToString(" ") { PHENOMENON_NAMES_DE[it] ?: it }
    return listOfNotNull(intensityPrefix, descriptorText, phenomenaText)
        .joinToString(" ")
        .replaceFirstChar { it.uppercase() }
}
