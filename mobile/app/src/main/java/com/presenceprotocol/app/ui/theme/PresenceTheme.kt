package com.presenceprotocol.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Olive = Color(0xFF3D4A2E)
val OliveLight = Color(0xFF5C6B44)
val OlivePale = Color(0xFFEEF0E8)
val Gold = Color(0xFF8B7340)
val GoldLight = Color(0xFFC4A95A)
val GoldBright = Color(0xFFD4AF5A)
val GoldPale = Color(0xFFFDF6E3)
val Cream = Color(0xFFFAF8F3)
val Dark = Color(0xFF1A1A14)
val Mid = Color(0xFF4A4A40)
val Gray = Color(0xFF888880)

val LayerMobile = Color(0xFF3A6BC4)
val LayerEncounter = Color(0xFF4A8A3A)
val LayerRelay = Color(0xFFD4700A)
val LayerMidnight = Color(0xFF6B4DB0)
val LayerCardano = Color(0xFF3A3A3A)

private val OlivebranchColors = darkColorScheme(
    primary = GoldBright,
    onPrimary = Dark,
    secondary = GoldLight,
    onSecondary = Dark,
    tertiary = OlivePale,
    onTertiary = Dark,
    background = Olive,
    onBackground = Cream,
    surface = OliveLight,
    onSurface = Cream,
    surfaceVariant = OlivePale,
    onSurfaceVariant = Dark,
    error = Color(0xFFC0392B),
    onError = Cream
)

@Composable
fun PresenceTheme(content: @Composable () -> Unit) {
    isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = OlivebranchColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
