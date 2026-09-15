package de.privat.schmuddelwetter.util

/**
 * Vereinheitlichte, grobe Wetterlage – sowohl DWD-ww-Codes als auch
 * METAR/TAF-Wettererscheinungen werden hierauf abgebildet, damit UI und
 * Icon-Auswahl für beide Datenquellen identisch funktionieren.
 */
enum class WeatherCondition {
    CLEAR,
    PARTLY_CLOUDY,
    CLOUDY,
    FOG,
    DRIZZLE,
    RAIN,
    RAIN_SHOWERS,
    SLEET,
    SNOW,
    SNOW_SHOWERS,
    HAIL,
    THUNDERSTORM,
    WIND,
    SANDSTORM,
    UNKNOWN,
}
