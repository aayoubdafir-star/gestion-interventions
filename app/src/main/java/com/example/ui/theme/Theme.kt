package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = OcpSkyBlue,
    secondary = OcpLightBlue,
    tertiary = WarningYellow,
    background = DarkBg,
    surface = DarkSurface,
    onPrimary = DarkBg,
    onSecondary = LightText,
    onBackground = LightText,
    onSurface = LightText
)

private val LightColorScheme = lightColorScheme(
    primary = OcpNavy,
    secondary = OcpMediumBlue,
    tertiary = OcpSkyBlue,
    background = OcpWhite,
    surface = OcpWhite,
    onPrimary = OcpWhite,
    onSecondary = OcpNavy,
    onBackground = DarkText,
    onSurface = DarkText
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Force false so it is always white/light theme
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
