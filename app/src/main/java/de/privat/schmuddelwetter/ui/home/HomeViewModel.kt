package de.privat.schmuddelwetter.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.privat.schmuddelwetter.data.dwd.DwdWeatherRepository
import de.privat.schmuddelwetter.data.dwd.model.MosmixForecast
import de.privat.schmuddelwetter.data.location.GeoLocation
import de.privat.schmuddelwetter.data.location.LocationProvider
import de.privat.schmuddelwetter.data.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val forecast: MosmixForecast) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel(
    private val locationProvider: LocationProvider,
    private val weatherRepository: DwdWeatherRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading

            val location = locationProvider.getCurrentLocation()
                ?: settingsRepository.getLastLocation()?.let { (lat, lon) -> GeoLocation(lat, lon) }

            if (location == null) {
                _uiState.value = HomeUiState.Error(
                    "Kein Standort verfügbar. Bitte Standortzugriff in den Einstellungen erlauben.",
                )
                return@launch
            }

            settingsRepository.saveLastLocation(location.lat, location.lon)

            weatherRepository.forecastForLocation(location.lat, location.lon)
                .onSuccess { forecast -> _uiState.value = HomeUiState.Success(forecast) }
                .onFailure { error ->
                    _uiState.value = HomeUiState.Error(error.message ?: "Wetterdaten konnten nicht geladen werden.")
                }
        }
    }
}
