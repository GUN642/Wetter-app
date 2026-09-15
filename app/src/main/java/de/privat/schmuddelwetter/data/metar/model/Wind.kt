package de.privat.schmuddelwetter.data.metar.model

data class Wind(
    val directionDeg: Int?,
    val isVariableDirection: Boolean,
    val speedKt: Int,
    val gustKt: Int?,
    val variableFromDeg: Int? = null,
    val variableToDeg: Int? = null,
)
