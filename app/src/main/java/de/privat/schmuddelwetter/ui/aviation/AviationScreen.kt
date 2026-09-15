package de.privat.schmuddelwetter.ui.aviation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.privat.schmuddelwetter.data.metar.model.Airport
import de.privat.schmuddelwetter.data.metar.model.CloudLayer
import de.privat.schmuddelwetter.data.metar.model.FlightCategory
import de.privat.schmuddelwetter.data.metar.model.Metar
import de.privat.schmuddelwetter.data.metar.model.Taf
import de.privat.schmuddelwetter.data.metar.model.TafPeriod
import de.privat.schmuddelwetter.data.metar.model.Wind
import de.privat.schmuddelwetter.data.metar.model.toGermanDescription
import de.privat.schmuddelwetter.di.ServiceLocator
import de.privat.schmuddelwetter.ui.components.NCard
import de.privat.schmuddelwetter.ui.components.NDivider
import de.privat.schmuddelwetter.ui.components.SectionLabel
import de.privat.schmuddelwetter.ui.components.StatTile
import de.privat.schmuddelwetter.ui.components.WeatherIcon
import de.privat.schmuddelwetter.ui.theme.FlightCategoryIfr
import de.privat.schmuddelwetter.ui.theme.FlightCategoryLifr
import de.privat.schmuddelwetter.ui.theme.FlightCategoryMvfr
import de.privat.schmuddelwetter.ui.theme.FlightCategoryUnknown
import de.privat.schmuddelwetter.ui.theme.FlightCategoryVfr
import de.privat.schmuddelwetter.util.degreesToCompass
import de.privat.schmuddelwetter.util.visibilityMetersToText

@Composable
fun AviationScreen(serviceLocator: ServiceLocator) {
    val viewModel: AviationViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                AviationViewModel(
                    airportRepository = serviceLocator.airportRepository,
                    aviationWeatherRepository = serviceLocator.aviationWeatherRepository,
                    locationProvider = serviceLocator.locationProvider,
                    settingsRepository = serviceLocator.settingsRepository,
                )
            }
        },
    )
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        SectionLabel(text = "Flugwetter")
        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("ICAO-Code, z. B. EDDF") },
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { viewModel.selectAirport(state.query) }) {
                        Icon(Icons.Filled.Search, contentDescription = "Suchen")
                    }
                },
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = viewModel::useNearestAirport) {
                Icon(Icons.Filled.MyLocation, contentDescription = "Nächster Flughafen")
            }
        }

        if (state.suggestions.isNotEmpty()) {
            NCard(modifier = Modifier.padding(top = 8.dp)) {
                state.suggestions.forEach { airport ->
                    AirportSuggestionRow(airport = airport, onClick = { viewModel.selectAirport(airport.icao) })
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.errorMessage != null && state.metar == null && state.taf == null -> {
                    Text(
                        text = state.errorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    )
                }
                state.metar != null || state.taf != null -> {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        item {
                            AirportHeader(
                                icao = state.selectedIcao.orEmpty(),
                                flightCategory = state.metar?.flightCategory ?: FlightCategory.UNKNOWN,
                                onFavoriteClick = viewModel::markCurrentAsFavorite,
                            )
                        }
                        state.metar?.let { metar -> item { MetarCard(metar) } }
                        state.taf?.let { taf -> item { TafCard(taf) } }
                    }
                }
                else -> {
                    Text(
                        text = "Flughafen suchen oder Standort verwenden, um METAR/TAF zu laden.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AirportSuggestionRow(airport: Airport, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        Text(text = airport.icao, style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(64.dp))
        Text(text = airport.name, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun AirportHeader(icao: String, flightCategory: FlightCategory, onFavoriteClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = icao, style = MaterialTheme.typography.displayLarge.copy(fontSize = 40.sp))
            Spacer(modifier = Modifier.width(12.dp))
            FlightCategoryBadge(flightCategory)
        }
        IconButton(onClick = onFavoriteClick) {
            Icon(Icons.Filled.StarBorder, contentDescription = "Als Favorit merken")
        }
    }
}

@Composable
private fun FlightCategoryBadge(category: FlightCategory) {
    val color = category.toColor()
    Box(
        modifier = Modifier
            .background(color = color, shape = RoundedCornerShape(4.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = category.name,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
        )
    }
}

private fun FlightCategory.toColor(): Color = when (this) {
    FlightCategory.VFR -> FlightCategoryVfr
    FlightCategory.MVFR -> FlightCategoryMvfr
    FlightCategory.IFR -> FlightCategoryIfr
    FlightCategory.LIFR -> FlightCategoryLifr
    FlightCategory.UNKNOWN -> FlightCategoryUnknown
}

@Composable
private fun MetarCard(metar: Metar) {
    NCard {
        SectionLabel(text = "METAR")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            WeatherIcon(condition = metar.condition, size = 32.dp)
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "${metar.tempC ?: "–"}°C / Taupunkt ${metar.dewpointC ?: "–"}°C", style = MaterialTheme.typography.bodyMedium)
                Text(text = "QNH ${metar.qnhHpa ?: "–"} hPa", style = MaterialTheme.typography.bodyMedium)
            }
        }
        NDivider()
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatTile(
                label = "Wind",
                value = windText(metar.wind),
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = "Sicht",
                value = visibilityMetersToText(metar.visibilityM?.toDouble()),
                modifier = Modifier.weight(1f),
            )
        }
        StatTile(
            label = "Wolkenuntergrenze",
            value = ceilingText(metar),
        )
        if (metar.weather.isNotEmpty()) {
            StatTile(
                label = "Wettererscheinungen",
                value = metar.weather.joinToString(", ") { it.toGermanDescription() },
            )
        }
        if (metar.clouds.isNotEmpty()) {
            StatTile(
                label = "Bewölkung",
                value = metar.clouds.joinToString(", ") { cloudLayerText(it) },
            )
        }
        NDivider()
        Text(
            text = metar.raw,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TafCard(taf: Taf) {
    NCard {
        SectionLabel(text = "TAF")
        taf.periods.forEachIndexed { index, period ->
            if (index > 0) NDivider()
            TafPeriodRow(period)
        }
        NDivider()
        Text(
            text = taf.raw,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TafPeriodRow(period: TafPeriod) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(text = period.label, style = MaterialTheme.typography.titleMedium)
            WeatherIcon(condition = period.condition, size = 22.dp)
        }
        if (period.validityRaw != null) {
            Text(text = "Gültig: ${period.validityRaw}", style = MaterialTheme.typography.bodyMedium)
        }
        Text(text = "Wind: ${windText(period.wind)}", style = MaterialTheme.typography.bodyMedium)
        Text(
            text = "Sicht: ${visibilityMetersToText(period.visibilityM?.toDouble())}" +
                (period.ceilingFt?.let { " · Wolkenuntergrenze: $it ft" } ?: ""),
            style = MaterialTheme.typography.bodyMedium,
        )
        if (period.weather.isNotEmpty()) {
            Text(
                text = "Wetter: " + period.weather.joinToString(", ") { it.toGermanDescription() },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (period.clouds.isNotEmpty()) {
            Text(
                text = "Wolken: " + period.clouds.joinToString(", ") { cloudLayerText(it) },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private fun windText(wind: Wind?): String {
    if (wind == null) return "–"
    val direction = if (wind.isVariableDirection) "wechselnd" else degreesToCompass(wind.directionDeg?.toDouble())
    val gust = wind.gustKt?.let { " (Böen ${it} kt)" } ?: ""
    return "$direction ${wind.speedKt} kt$gust"
}

private fun ceilingText(metar: Metar): String = when {
    metar.verticalVisibilityFt != null -> "Himmel verdeckt, VV ${metar.verticalVisibilityFt} ft"
    metar.ceilingFt != null -> "${metar.ceilingFt} ft"
    metar.cavok -> "Keine (CAVOK)"
    else -> "Keine signifikante Bewölkung"
}

private fun cloudLayerText(layer: CloudLayer): String {
    val special = when {
        layer.isCumulonimbus -> " CB"
        layer.isToweringCumulus -> " TCU"
        else -> ""
    }
    return "${layer.coverage.name} ${layer.heightFt} ft$special"
}
