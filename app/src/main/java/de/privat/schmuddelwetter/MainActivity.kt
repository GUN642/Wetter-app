package de.privat.schmuddelwetter

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import de.privat.schmuddelwetter.data.settings.ThemeMode
import de.privat.schmuddelwetter.ui.navigation.SchmuddelwetterNavGraph
import de.privat.schmuddelwetter.ui.theme.SchmuddelwetterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val serviceLocator = (application as SchmuddelwetterApp).serviceLocator

        setContent {
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions(),
            ) { /* Standort wird bei Bedarf über LocationProvider erneut geprüft. */ }

            LaunchedEffect(Unit) {
                permissionLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                )
            }

            val themeMode by serviceLocator.settingsRepository.themeModeFlow.collectAsState(initial = ThemeMode.SYSTEM)
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            SchmuddelwetterTheme(darkTheme = darkTheme) {
                SchmuddelwetterNavGraph(serviceLocator = serviceLocator)
            }
        }
    }
}
