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
    background = WhiteBackground,
    onBackground = TextDark,
    surface = WhiteCard,
    onSurface = TextDark,
    surfaceVariant = CardBorderColor,
    onSurfaceVariant = TextMuted,
    error = CrimsonError,
    errorContainer = CrimsonContainer,
    onError = Color.White
)

// Dark/Contrasted Scheme with Green Accents
private val DarkColorScheme = darkColorScheme(
    primary = GreenLight,
    onPrimary = Color.Black,
    primaryContainer = GreenDark,
    onPrimaryContainer = Color.White,
    secondary = GreenPrimary,
    onSecondary = Color.White,
    secondaryContainer = GreenBorder,
    onSecondaryContainer = Color.White,
    tertiary = AmberWarning,
    background = WhiteBackground,
    onBackground = TextDark,
    surface = WhiteCard,
    onSurface = TextDark,
    surfaceVariant = CardBorderColor,
    onSurfaceVariant = TextMuted,
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
