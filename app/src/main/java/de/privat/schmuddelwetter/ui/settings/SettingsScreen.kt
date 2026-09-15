package de.privat.schmuddelwetter.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.privat.schmuddelwetter.data.settings.ThemeMode
import de.privat.schmuddelwetter.di.ServiceLocator
import de.privat.schmuddelwetter.ui.components.NCard
import de.privat.schmuddelwetter.ui.components.SectionLabel

@Composable
fun SettingsScreen(serviceLocator: ServiceLocator) {
    val viewModel: SettingsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { SettingsViewModel(serviceLocator.settingsRepository) }
        },
    )
    val themeMode by viewModel.themeMode.collectAsState()
    val favoriteAirport by viewModel.favoriteAirport.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        SectionLabel(text = "Einstellungen")
        Spacer(modifier = Modifier.height(16.dp))

        NCard {
            SectionLabel(text = "Darstellung")
            ThemeMode.entries.forEach { mode ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setThemeMode(mode) }
                        .padding(vertical = 4.dp),
                ) {
                    RadioButton(selected = themeMode == mode, onClick = { viewModel.setThemeMode(mode) })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = themeModeLabel(mode), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        NCard {
            SectionLabel(text = "Bevorzugter Flughafen")
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = favoriteAirport ?: "Kein Favorit gesetzt",
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (favoriteAirport != null) {
                    IconButton(onClick = viewModel::clearFavoriteAirport) {
                        Icon(Icons.Filled.Close, contentDescription = "Favorit entfernen")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        NCard {
            SectionLabel(text = "Über diese App")
            Text(
                text = "Schmuddelwetter ist eine private Wetter-App ohne Werbung, Tracking oder " +
                    "Cloud-Konto.\n\nAllgemeines Wetter: Deutscher Wetterdienst (DWD), MOSMIX Open Data.\n" +
                    "Flugwetter (METAR/TAF): aviationweather.gov (NOAA).",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private fun themeModeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> "System"
    ThemeMode.LIGHT -> "Hell"
    ThemeMode.DARK -> "Dunkel"
}
