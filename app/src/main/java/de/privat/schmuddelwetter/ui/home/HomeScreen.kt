package de.privat.schmuddelwetter.ui.home

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.privat.schmuddelwetter.data.dwd.WeatherCodeMapper
import de.privat.schmuddelwetter.data.dwd.model.MosmixDaySummary
import de.privat.schmuddelwetter.data.dwd.model.MosmixForecast
import de.privat.schmuddelwetter.data.dwd.model.MosmixTimeStep
import de.privat.schmuddelwetter.di.ServiceLocator
import de.privat.schmuddelwetter.ui.components.NCard
import de.privat.schmuddelwetter.ui.components.NDivider
import de.privat.schmuddelwetter.ui.components.SectionLabel
import de.privat.schmuddelwetter.ui.components.StatTile
import de.privat.schmuddelwetter.ui.components.WeatherIcon
import de.privat.schmuddelwetter.util.WeatherCondition
import de.privat.schmuddelwetter.util.degreesToCompass
import de.privat.schmuddelwetter.util.roundedOrDash
import de.privat.schmuddelwetter.util.toDayMonthString
import de.privat.schmuddelwetter.util.toLocalHourString
import de.privat.schmuddelwetter.util.toWeekdayString
import de.privat.schmuddelwetter.util.visibilityMetersToText
import java.time.Instant

@Composable
fun HomeScreen(serviceLocator: ServiceLocator) {
    val viewModel: HomeViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                HomeViewModel(
                    locationProvider = serviceLocator.locationProvider,
                    weatherRepository = serviceLocator.dwdWeatherRepository,
                    settingsRepository = serviceLocator.settingsRepository,
                )
            }
        },
    )
    val state by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        when (val current = state) {
            is HomeUiState.Loading -> LoadingView()
            is HomeUiState.Error -> ErrorView(message = current.message, onRetry = viewModel::refresh)
            is HomeUiState.Success -> WeatherContent(forecast = current.forecast, onRefresh = viewModel::refresh)
        }
    }
}

@Composable
private fun LoadingView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            IconButton(onClick = onRetry) {
                Icon(imageVector = Icons.Filled.Refresh, contentDescription = "Erneut versuchen")
            }
        }
    }
}

@Composable
private fun WeatherContent(forecast: MosmixForecast, onRefresh: () -> Unit) {
    val current = forecast.currentTimeStep()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    SectionLabel(text = "Schmuddelwetter")
                    Text(text = forecast.station.name, style = MaterialTheme.typography.titleMedium)
                }
                IconButton(onClick = onRefresh) {
                    Icon(imageVector = Icons.Filled.Refresh, contentDescription = "Aktualisieren")
                }
            }
        }

        item { CurrentWeatherCard(current) }

        item {
            NCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatTile(
                        label = "Wind",
                        value = "${current?.windSpeedKmh.roundedOrDash(" km/h")} ${degreesToCompass(current?.windDirDeg)}",
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = "Niederschlag",
                        value = current?.precipProbabilityPercent.roundedOrDash(" %"),
                        modifier = Modifier.weight(1f),
                    )
                }
                NDivider()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatTile(
                        label = "Sicht",
                        value = visibilityMetersToText(current?.visibilityM),
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = "Luftdruck",
                        value = current?.pressureHpa.roundedOrDash(" hPa"),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        item {
            Column {
                SectionLabel(text = "Stündlich")
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    items(forecast.hourly.filter { it.time.isAfter(Instant.now().minusSeconds(1800)) }.take(24)) { step ->
                        HourlyItem(step)
                    }
                }
            }
        }

        item {
            Column {
                SectionLabel(text = "7 Tage")
                Spacer(modifier = Modifier.height(8.dp))
                NCard {
                    forecast.daily.take(7).forEachIndexed { index, day ->
                        if (index > 0) NDivider()
                        DailyRow(day)
                    }
                }
            }
        }

        item {
            Text(
                text = "Quelle: Deutscher Wetterdienst (DWD), MOSMIX · Stand ${forecast.issuedAt.toLocalHourString()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CurrentWeatherCard(current: MosmixTimeStep?) {
    NCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(
                    text = "${current?.tempC.roundedOrDash()}°",
                    style = MaterialTheme.typography.displayLarge,
                )
                Text(
                    text = WeatherCodeMapper.description(current?.condition ?: WeatherCondition.UNKNOWN),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            WeatherIcon(
                condition = current?.condition ?: WeatherCondition.UNKNOWN,
                size = 56.dp,
            )
        }
    }
}

@Composable
private fun HourlyItem(step: MosmixTimeStep) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(48.dp)) {
        Text(text = step.time.toLocalHourString(), style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(4.dp))
        WeatherIcon(condition = step.condition, size = 24.dp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "${step.tempC.roundedOrDash()}°", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun DailyRow(day: MosmixDaySummary) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "${day.date.toWeekdayString()} ${day.date.toDayMonthString()}",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.width(90.dp),
        )
        WeatherIcon(condition = day.dominantCondition, size = 22.dp)
        Text(
            text = day.maxPrecipProbabilityPercent.roundedOrDash(" %"),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(48.dp),
            textAlign = TextAlign.End,
        )
        Row(modifier = Modifier.width(90.dp), horizontalArrangement = Arrangement.End) {
            Text(text = day.minTempC.roundedOrDash("°"), style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = day.maxTempC.roundedOrDash("°"), style = MaterialTheme.typography.titleMedium)
        }
    }
}
