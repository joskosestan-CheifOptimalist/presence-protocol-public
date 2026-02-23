package com.presenceprotocol.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF33C4FF),
    secondary = Color(0xFF6EE7C9),
    tertiary = Color(0xFFFFC970),
    background = Color(0xFF0B0F19),
    surface = Color(0xFF101828),
    error = Color(0xFFFF7660)
)

@Composable
fun PresenceTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) {
        DarkColors
    } else {
        DarkColors
    }
    MaterialTheme(
        colorScheme = colors,
        typography = MaterialTheme.typography,
        content = content
    )
}
