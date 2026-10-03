package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// White & Green Light Scheme (Primary Default)
private val LightColorScheme = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = Color.White,
    primaryContainer = GreenContainer,
    onPrimaryContainer = GreenDark,
    secondary = GreenHover,
    onSecondary = Color.White,
    secondaryContainer = GreenBorder,
    onSecondaryContainer = GreenDark,
    tertiary = AmberWarning,
    background = LightWhiteBackground,
    onBackground = LightTextDark,
    surface = LightWhiteCard,
    onSurface = LightTextDark,
    surfaceVariant = LightCardBorder,
    onSurfaceVariant = LightTextMuted,
    error = CrimsonError,
    errorContainer = CrimsonContainer,
    onError = Color.White
)

// Dark/Contrasted Scheme with Green Accents
private val DarkColorScheme = darkColorScheme(
    primary = GreenLight,
    onPrimary = Color(0xFF0B1120),
    primaryContainer = Color(0xFF064E3B),
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = GreenPrimary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF065F46),
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = AmberWarning,
    background = DarkDeepBackground,
    onBackground = DarkTextLight,
    surface = DarkCardSurface,
    onSurface = DarkTextLight,
    surfaceVariant = DarkCardBorder,
    onSurfaceVariant = DarkTextMuted,
    error = CrimsonError,
    errorContainer = CrimsonContainer,
    onError = Color.White
)

@Composable
fun CollegeOutpassTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    CollegeOutpassTheme(darkTheme = darkTheme, content = content)
}
