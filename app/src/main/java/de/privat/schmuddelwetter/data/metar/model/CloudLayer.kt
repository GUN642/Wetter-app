package de.privat.schmuddelwetter.data.metar.model

/** Bedeckungsgrad in aufsteigender Reihenfolge – wichtig für Vergleiche (max = größte Bedeckung). */
enum class CloudCoverage { FEW, SCT, BKN, OVC }

data class CloudLayer(
    val coverage: CloudCoverage,
    val heightFt: Int,
    val isCumulonimbus: Boolean = false,
    val isToweringCumulus: Boolean = false,
)
