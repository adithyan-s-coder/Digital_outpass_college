package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// White & Green Theme Palette
val GreenPrimary = Color(0xFF10B981)         // Vivid Emerald Green for primary buttons & highlights
val GreenDark = Color(0xFF047857)            // Deep Emerald Green for text & borders
val GreenHover = Color(0xFF059669)           // Rich Forest Green for pressed states
val GreenLight = Color(0xFF34D399)           // Fresh Mint Green
val GreenMintBackground = Color(0xFFF0FDF4)  // Soft tint background
val GreenContainer = Color(0xFFECFDF5)       // Mint chip / pill container
val GreenBorder = Color(0xFFA7F3D0)          // Soft emerald border

val WhitePure = Color(0xFFFFFFFF)            // Pure clean White
val WhiteBackground = Color(0xFFF9FAF9)      // Ultra-clean crisp modern white background
val WhiteCard = Color(0xFFFFFFFF)            // Pure White Card surface
val CardBorderColor = Color(0xFFE2EBE5)      // Crisp soft border
val TextDark = Color(0xFF000000)             // Pure black text for clear readability
val TextMuted = Color(0xFF475569)            // Subtitle text

// Mappings for existing component tokens to adopt the White & Green theme seamlessly:
val Slate900 = WhiteBackground               // Main screen background is crisp white
val Slate800 = WhiteCard                     // Cards and surfaces are pure white
val Slate700 = CardBorderColor               // Borders are clean light borders
val Slate600 = Color(0xFF94A3B8)
val Slate400 = Color(0xFF64748B)
val Slate200 = TextDark                      // High-contrast readable text
val Slate100 = TextDark                      // High-contrast text

val IndigoPrimary = GreenPrimary             // Buttons and highlights are emerald green
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
