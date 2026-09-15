package de.privat.schmuddelwetter.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.privat.schmuddelwetter.util.WeatherCondition

fun WeatherCondition.toIcon(): ImageVector = when (this) {
    WeatherCondition.CLEAR -> Icons.Filled.WbSunny
    WeatherCondition.PARTLY_CLOUDY -> Icons.Filled.WbCloudy
    WeatherCondition.CLOUDY -> Icons.Filled.Cloud
    WeatherCondition.FOG -> Icons.Filled.BlurOn
    WeatherCondition.DRIZZLE -> Icons.Filled.Grain
    WeatherCondition.RAIN -> Icons.Filled.Umbrella
    WeatherCondition.RAIN_SHOWERS -> Icons.Filled.Umbrella
    WeatherCondition.SLEET -> Icons.Filled.Grain
    WeatherCondition.SNOW -> Icons.Filled.AcUnit
    WeatherCondition.SNOW_SHOWERS -> Icons.Filled.AcUnit
    WeatherCondition.HAIL -> Icons.Filled.Grain
    WeatherCondition.THUNDERSTORM -> Icons.Filled.FlashOn
    WeatherCondition.WIND -> Icons.Filled.Navigation
    WeatherCondition.SANDSTORM -> Icons.Filled.BlurOn
    WeatherCondition.UNKNOWN -> Icons.Filled.HelpOutline
}

@Composable
fun WeatherIcon(
    condition: WeatherCondition,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    tint: Color = LocalContentColor.current,
) {
    Icon(
        imageVector = condition.toIcon(),
        contentDescription = null,
        modifier = modifier.size(size),
        tint = tint,
    )
}
