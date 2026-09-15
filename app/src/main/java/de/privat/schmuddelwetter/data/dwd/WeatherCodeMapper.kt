package de.privat.schmuddelwetter.data.dwd

import de.privat.schmuddelwetter.util.WeatherCondition

/**
 * Bildet den synoptischen ww-Signifikantwettercode (WMO-Code-Tabelle 4677/4680,
 * Werte 0–99), wie ihn DWD MOSMIX liefert, auf eine vereinfachte [WeatherCondition] ab.
 * Ist ww nicht vorhanden, wird ersatzweise aus dem Bedeckungsgrad geschätzt.
 */
object WeatherCodeMapper {

    fun fromWwCode(code: Int?, cloudCoverPercent: Double? = null): WeatherCondition {
        if (code != null) {
            fromWwCodeOrNull(code)?.let { return it }
        }
        return fromCloudCover(cloudCoverPercent)
    }

    private fun fromWwCodeOrNull(code: Int): WeatherCondition? = when (code) {
        0, 1, 2, 3 -> WeatherCondition.PARTLY_CLOUDY
        in 4..9 -> WeatherCondition.FOG
        10, 11, 12 -> WeatherCondition.FOG
        13, 17, 19 -> WeatherCondition.THUNDERSTORM
        18 -> WeatherCondition.WIND
        in 14..16 -> WeatherCondition.RAIN
        in 20..29 -> WeatherCondition.CLOUDY
        in 30..35 -> WeatherCondition.SANDSTORM
        in 36..39 -> WeatherCondition.SNOW
        in 40..49 -> WeatherCondition.FOG
        in 50..55 -> WeatherCondition.DRIZZLE
        56, 57 -> WeatherCondition.SLEET
        58, 59 -> WeatherCondition.DRIZZLE
        in 60..65 -> WeatherCondition.RAIN
        66, 67 -> WeatherCondition.SLEET
        68, 69 -> WeatherCondition.SLEET
        in 70..78 -> WeatherCondition.SNOW
        79 -> WeatherCondition.HAIL
        80, 81, 82 -> WeatherCondition.RAIN_SHOWERS
        83, 84 -> WeatherCondition.SLEET
        85, 86 -> WeatherCondition.SNOW_SHOWERS
        in 87..90 -> WeatherCondition.HAIL
        in 91..99 -> WeatherCondition.THUNDERSTORM
        else -> null
    }

    private fun fromCloudCover(percent: Double?): WeatherCondition = when {
        percent == null -> WeatherCondition.UNKNOWN
        percent < 20.0 -> WeatherCondition.CLEAR
        percent < 70.0 -> WeatherCondition.PARTLY_CLOUDY
        else -> WeatherCondition.CLOUDY
    }

    /** Kurze deutsche Beschreibung für Anzeige/Barrierefreiheit. */
    fun description(condition: WeatherCondition): String = when (condition) {
        WeatherCondition.CLEAR -> "Klar"
        WeatherCondition.PARTLY_CLOUDY -> "Teils bewölkt"
        WeatherCondition.CLOUDY -> "Bewölkt"
        WeatherCondition.FOG -> "Nebel"
        WeatherCondition.DRIZZLE -> "Nieselregen"
        WeatherCondition.RAIN -> "Regen"
        WeatherCondition.RAIN_SHOWERS -> "Regenschauer"
        WeatherCondition.SLEET -> "Schneeregen"
        WeatherCondition.SNOW -> "Schnee"
        WeatherCondition.SNOW_SHOWERS -> "Schneeschauer"
        WeatherCondition.HAIL -> "Hagel"
        WeatherCondition.THUNDERSTORM -> "Gewitter"
        WeatherCondition.WIND -> "Windböen"
        WeatherCondition.SANDSTORM -> "Sand-/Staubsturm"
        WeatherCondition.UNKNOWN -> "Unbekannt"
    }
}
