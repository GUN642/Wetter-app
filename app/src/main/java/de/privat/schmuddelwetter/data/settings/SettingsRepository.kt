package de.privat.schmuddelwetter.data.settings

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "schmuddelwetter_settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Persistiert kleine Nutzereinstellungen lokal (keine Cloud-Synchronisation, rein privat). */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val LAST_LAT = doublePreferencesKey("last_lat")
        val LAST_LON = doublePreferencesKey("last_lon")
        val FAVORITE_AIRPORT = stringPreferencesKey("favorite_airport_icao")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val favoriteAirportFlow: Flow<String?> = context.dataStore.data.map { it[Keys.FAVORITE_AIRPORT] }

    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        prefs[Keys.THEME_MODE]?.let { raw -> runCatching { ThemeMode.valueOf(raw) }.getOrNull() }
            ?: ThemeMode.SYSTEM
    }

    suspend fun saveLastLocation(lat: Double, lon: Double) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_LAT] = lat
            prefs[Keys.LAST_LON] = lon
        }
    }

    suspend fun getLastLocation(): Pair<Double, Double>? {
        val prefs = context.dataStore.data.first()
        val lat = prefs[Keys.LAST_LAT] ?: return null
        val lon = prefs[Keys.LAST_LON] ?: return null
        return lat to lon
    }

    suspend fun setFavoriteAirport(icao: String?) {
        context.dataStore.edit { prefs ->
            if (icao.isNullOrBlank()) {
                prefs.remove(Keys.FAVORITE_AIRPORT)
            } else {
                prefs[Keys.FAVORITE_AIRPORT] = icao.uppercase()
            }
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }
}
