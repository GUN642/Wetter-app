package de.privat.schmuddelwetter.ui.aviation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.privat.schmuddelwetter.data.location.LocationProvider
import de.privat.schmuddelwetter.data.metar.AirportRepository
import de.privat.schmuddelwetter.data.metar.AviationWeatherRepository
import de.privat.schmuddelwetter.data.metar.model.Airport
import de.privat.schmuddelwetter.data.metar.model.Metar
import de.privat.schmuddelwetter.data.metar.model.Taf
import de.privat.schmuddelwetter.data.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class AviationUiState(
    val query: String = "",
    val suggestions: List<Airport> = emptyList(),
    val selectedIcao: String? = null,
    val isLoading: Boolean = false,
    val metar: Metar? = null,
    val taf: Taf? = null,
    val errorMessage: String? = null,
)

class AviationViewModel(
    private val airportRepository: AirportRepository,
    private val aviationWeatherRepository: AviationWeatherRepository,
    private val locationProvider: LocationProvider,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AviationUiState())
    val uiState: StateFlow<AviationUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val favoriteIcao = settingsRepository.favoriteAirportFlow.first()
            val icao = favoriteIcao ?: locationProvider.getCurrentLocation()
                ?.let { airportRepository.findNearest(it.lat, it.lon)?.icao }
            if (icao != null) selectAirport(icao)
        }
    }

    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(
            query = query,
            suggestions = if (query.isBlank()) emptyList() else airportRepository.search(query).take(8),
        )
    }

    fun selectAirport(icao: String) {
        val normalized = icao.trim().uppercase()
        if (!normalized.matches(Regex("^[A-Z]{4}$"))) {
            _uiState.value = _uiState.value.copy(errorMessage = "Bitte einen gültigen 4-stelligen ICAO-Code eingeben.")
            return
        }

        _uiState.value = _uiState.value.copy(
            selectedIcao = normalized,
            query = normalized,
            suggestions = emptyList(),
            isLoading = true,
            errorMessage = null,
        )

        viewModelScope.launch {
            val metarResult = aviationWeatherRepository.getMetar(normalized)
            val tafResult = aviationWeatherRepository.getTaf(normalized)
            val metar = metarResult.getOrNull()
            val taf = tafResult.getOrNull()
            val error = if (metar == null && taf == null) {
                metarResult.exceptionOrNull()?.message ?: "Keine Daten für $normalized gefunden."
            } else {
                null
            }
            _uiState.value = _uiState.value.copy(isLoading = false, metar = metar, taf = taf, errorMessage = error)
        }
    }

    fun useNearestAirport() {
        viewModelScope.launch {
            val location = locationProvider.getCurrentLocation()
            val airport = location?.let { airportRepository.findNearest(it.lat, it.lon) }
            if (airport != null) {
                selectAirport(airport.icao)
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = "Standort nicht verfügbar.")
            }
        }
    }

    fun markCurrentAsFavorite() {
        val icao = _uiState.value.selectedIcao ?: return
        viewModelScope.launch { settingsRepository.setFavoriteAirport(icao) }
    }
}
