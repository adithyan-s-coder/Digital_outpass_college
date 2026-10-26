package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// Light Theme Palette
val LightWhiteBackground = Color(0xFFF8FAF9)  // Crisp, clean modern light background
val LightWhiteCard = Color(0xFFFFFFFF)        // Pure white card surface
val LightCardBorder = Color(0xFFE2EBE5)       // Soft, elegant border
val LightTextDark = Color(0xFF0F172A)         // Deep slate-900 high-contrast text
val LightTextMuted = Color(0xFF475569)        // Slate-600 subtitle text

// Dark Theme Palette (Sleek, high-contrast Dark Mode)
val DarkDeepBackground = Color(0xFF0B1120)    // Deep night background
val DarkCardSurface = Color(0xFF1E293B)       // Rich slate-800 card surface
val DarkCardBorder = Color(0xFF334155)        // Subtle dark border
val DarkTextLight = Color(0xFFF8FAFC)         // Crisp white readable text
val DarkTextMuted = Color(0xFF94A3B8)         // Clear secondary text

// White & Green Theme Accents
val GreenPrimary = Color(0xFF10B981)          // Vivid Emerald Green for primary buttons & highlights
val GreenDark = Color(0xFF047857)             // Deep Emerald Green for text & borders
val GreenHover = Color(0xFF059669)            // Rich Forest Green for pressed states
val GreenLight = Color(0xFF34D399)            // Fresh Mint Green
val GreenMintBackground = Color(0xFFF0FDF4)   // Soft tint background
val GreenContainer = Color(0xFFECFDF5)        // Mint chip / pill container
val GreenBorder = Color(0xFFA7F3D0)           // Soft emerald border
val WhitePure = Color(0xFFFFFFFF)             // Pure clean White

// Dynamic Theme-Aware Tokens: seamlessly adapt to Light and Dark mode everywhere
val Slate900: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.background

val Slate800: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surface

val Slate700: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surfaceVariant

val Slate600 = Color(0xFF94A3B8)
val Slate400 = Color(0xFF64748B)

val Slate200: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurface

val Slate100: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurface

val WhiteBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.background

val WhiteCard: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surface

val CardBorderColor: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surfaceVariant

val TextDark: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurface

val TextMuted: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurfaceVariant

val IndigoPrimary = GreenPrimary
val IndigoLight = GreenLight
val IndigoContainer = GreenContainer

val EmeraldSuccess = Color(0xFF10B981)
val EmeraldLight = Color(0xFF34D399)
val EmeraldContainer = Color(0xFFD1FAE5)

val AmberWarning = Color(0xFFD97706)
val AmberContainer = Color(0xFFFEF3C7)

val CrimsonError = Color(0xFFEF4444)
val CrimsonLight = Color(0xFFF87171)
val CrimsonContainer = Color(0xFFFEE2E2)
