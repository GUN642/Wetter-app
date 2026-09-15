package de.privat.schmuddelwetter.data.metar.model

/** Standard-Flugwetterkategorien (ICAO/FAA-Konvention), berechnet aus Sicht und Wolkenuntergrenze. */
enum class FlightCategory {
    VFR,
    MVFR,
    IFR,
    LIFR,
    UNKNOWN,
}

object FlightCategoryCalculator {
    /** @param visibilityM Sichtweite in Metern, [ceilingFt] tiefste BKN/OVC-Untergrenze in Fuß. */
    fun calculate(visibilityM: Int?, ceilingFt: Int?): FlightCategory {
        if (visibilityM == null && ceilingFt == null) return FlightCategory.UNKNOWN
        val visSm = visibilityM?.let { it / 1609.34 }

        val isLifr = (ceilingFt != null && ceilingFt < 500) || (visSm != null && visSm < 1.0)
        if (isLifr) return FlightCategory.LIFR

        val isIfr = (ceilingFt != null && ceilingFt < 1000) || (visSm != null && visSm < 3.0)
        if (isIfr) return FlightCategory.IFR

        val isMvfr = (ceilingFt != null && ceilingFt <= 3000) || (visSm != null && visSm <= 5.0)
        if (isMvfr) return FlightCategory.MVFR

        return FlightCategory.VFR
    }
}
