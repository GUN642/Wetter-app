package de.privat.schmuddelwetter.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import de.privat.schmuddelwetter.R

// Dot-Matrix-Font (Silkscreen) für große Zahlen/Temperaturen – erinnert an das
// Glyph-/Punktraster-Interface, ohne die geschützte Nothing-Hausschrift zu verwenden.
val DotFontFamily = FontFamily(
    Font(R.font.silkscreen_regular, FontWeight.Normal),
    Font(R.font.silkscreen_bold, FontWeight.Bold),
)

// Space Mono als technische Grotesk für Fließtext/Labels im Grid-Look.
val MonoFontFamily = FontFamily(
    Font(R.font.space_mono_regular, FontWeight.Normal),
    Font(R.font.space_mono_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.space_mono_bold, FontWeight.Bold),
)

// Großer, dot-matrix-artiger Temperaturwert (z. B. "7°").
val DotDisplayStyle = TextStyle(
    fontFamily = DotFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 96.sp,
    lineHeight = 96.sp,
    textAlign = TextAlign.Start,
)

val DotHeadlineStyle = TextStyle(
    fontFamily = DotFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 32.sp,
    lineHeight = 36.sp,
)

// Kleine, gesperrte Großbuchstaben-Labels ("STÜNDLICH", "SICHT" …).
val LabelCapsStyle = TextStyle(
    fontFamily = MonoFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 12.sp,
    letterSpacing = 0.18.em,
)

val Typography = Typography(
    displayLarge = DotDisplayStyle,
    headlineMedium = DotHeadlineStyle,
    titleMedium = TextStyle(
        fontFamily = MonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        letterSpacing = 0.05.em,
    ),
    bodyLarge = TextStyle(
        fontFamily = MonoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = MonoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelSmall = LabelCapsStyle,
)
