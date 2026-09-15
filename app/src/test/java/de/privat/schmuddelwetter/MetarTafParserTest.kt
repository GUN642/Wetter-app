package de.privat.schmuddelwetter

import de.privat.schmuddelwetter.data.metar.MetarTafParser
import de.privat.schmuddelwetter.data.metar.model.CloudCoverage
import de.privat.schmuddelwetter.data.metar.model.FlightCategory
import de.privat.schmuddelwetter.util.WeatherCondition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MetarTafParserTest {

    @Test
    fun `parses standard metar with wind, clouds, temp and qnh`() {
        val metar = MetarTafParser.parseMetar(
            "EDDF 151720Z 25012G22KT 9999 SCT030 BKN045 12/07 Q1013",
        )

        assertEquals("EDDF", metar.icao)
        assertEquals(250, metar.wind?.directionDeg)
        assertEquals(12, metar.wind?.speedKt)
        assertEquals(22, metar.wind?.gustKt)
        assertEquals(10000, metar.visibilityM)
        assertEquals(12, metar.tempC)
        assertEquals(7, metar.dewpointC)
        assertEquals(1013, metar.qnhHpa)
        assertEquals(4500, metar.ceilingFt)
        assertEquals(FlightCategory.VFR, metar.flightCategory)
    }

    @Test
    fun `parses cavok as clear with full visibility`() {
        val metar = MetarTafParser.parseMetar("EDDM 151750Z 03005KT CAVOK 18/10 Q1018")

        assertTrue(metar.cavok)
        assertEquals(10000, metar.visibilityM)
        assertNull(metar.ceilingFt)
        assertEquals(WeatherCondition.CLEAR, metar.condition)
        assertEquals(FlightCategory.VFR, metar.flightCategory)
    }

    @Test
    fun `detects thunderstorm and low ceiling as LIFR`() {
        val metar = MetarTafParser.parseMetar(
            "EDDB 151800Z 28025G40KT 1500 +TSRA BKN004 OVC010CB 18/17 Q0995",
        )

        assertEquals(WeatherCondition.THUNDERSTORM, metar.condition)
        assertEquals(400, metar.ceilingFt)
        assertEquals(FlightCategory.LIFR, metar.flightCategory)
        assertEquals(1, metar.clouds.count { it.isCumulonimbus })
    }

    @Test
    fun `parses statute mile visibility fraction`() {
        val metar = MetarTafParser.parseMetar("KJFK 151751Z 18010KT 1/2SM FG VV002 07/07 A2992")

        assertTrue(metar.visibilityM!! in 700..900)
        assertEquals(200, metar.verticalVisibilityFt)
        assertEquals(WeatherCondition.FOG, metar.condition)
        assertEquals(FlightCategory.LIFR, metar.flightCategory)
        assertEquals(1013, metar.qnhHpa)
    }

    @Test
    fun `parses taf with becmg tempo and fm groups`() {
        val taf = MetarTafParser.parseTaf(
            "TAF EDDF 151700Z 1518/1624 24012KT 9999 SCT030 " +
                "BECMG 1521/1523 25015G25KT " +
                "PROB30 TEMPO 1600/1606 4000 SHRA BKN015CB " +
                "FM160800 27008KT 9999 SCT025",
        )

        assertEquals("EDDF", taf.icao)
        assertEquals(4, taf.periods.size)
        assertEquals("Grundvorhersage", taf.periods[0].label)
        assertEquals(240, taf.periods[0].wind?.directionDeg)
        assertTrue(taf.periods[1].label.contains("BECMG"))
        assertTrue(taf.periods[2].label.contains("Wahrscheinlichkeit") || taf.periods[2].label.contains("30"))
        assertEquals(WeatherCondition.RAIN_SHOWERS, taf.periods[2].condition)
        assertTrue(taf.periods[3].label.startsWith("Ab 16."))
        assertEquals(CloudCoverage.SCT, taf.periods[3].clouds.first().coverage)
    }
}
