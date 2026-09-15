package de.privat.schmuddelwetter.data.dwd.model

/**
 * Eine Station aus der MOSMIX_S-Sammeldatei (kein separater ICAO-Code mehr
 * verfügbar – DWD liefert nur noch die 5-stellige WMO-Nummer und einen
 * Klartextnamen direkt in der Vorhersagedatei selbst).
 */
data class DwdStation(
    val id: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val elevationM: Int,
)
