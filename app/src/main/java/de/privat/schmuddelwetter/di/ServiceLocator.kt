package de.privat.schmuddelwetter.di

import android.content.Context
import de.privat.schmuddelwetter.data.dwd.DwdWeatherApi
import de.privat.schmuddelwetter.data.dwd.DwdWeatherRepository
import de.privat.schmuddelwetter.data.location.LocationProvider
import de.privat.schmuddelwetter.data.metar.AirportRepository
import de.privat.schmuddelwetter.data.metar.AviationWeatherApi
import de.privat.schmuddelwetter.data.metar.AviationWeatherRepository
import de.privat.schmuddelwetter.data.settings.SettingsRepository
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Einfacher, handgeschriebener Service-Locator statt Hilt/Dagger – für eine private
 * Einzelnutzer-App reicht das aus und hält den Build schlank.
 */
class ServiceLocator(context: Context) {
    private val appContext = context.applicationContext

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            // Die DWD-Sammeldatei ist ~36 MB groß; als Absicherung gegen ein
            // hängendes Netzwerk gibt es trotzdem eine harte Gesamt-Obergrenze.
            .callTimeout(3, TimeUnit.MINUTES)
            .build()
    }

    val dwdWeatherApi: DwdWeatherApi by lazy { DwdWeatherApi(okHttpClient) }
    val dwdWeatherRepository: DwdWeatherRepository by lazy { DwdWeatherRepository(dwdWeatherApi) }

    val airportRepository: AirportRepository by lazy { AirportRepository(appContext) }
    val aviationWeatherApi: AviationWeatherApi by lazy { AviationWeatherApi(okHttpClient) }
    val aviationWeatherRepository: AviationWeatherRepository by lazy {
        AviationWeatherRepository(aviationWeatherApi)
    }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }
    val locationProvider: LocationProvider by lazy { LocationProvider(appContext) }
}
