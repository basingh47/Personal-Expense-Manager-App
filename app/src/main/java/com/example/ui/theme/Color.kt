package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Professional Polish Theme dynamic colors
val PrimaryEmerald: Color
    @Composable
    get() = MaterialTheme.colorScheme.primary

val PrimaryLightEmerald = Color(0xFF0061A4)  // Accent Blue

val SecondarySage: Color
    @Composable
    get() = MaterialTheme.colorScheme.secondary

val AccentGold: Color
    @Composable
    get() = MaterialTheme.colorScheme.tertiary

val DarkBackground: Color
    @Composable
    get() = MaterialTheme.colorScheme.background

val DarkSurface: Color
    @Composable
    get() = MaterialTheme.colorScheme.surface

val DarkSurfaceVariant: Color
    @Composable
    get() = MaterialTheme.colorScheme.surfaceVariant

val SmoothWhite: Color
    @Composable
    get() = MaterialTheme.colorScheme.onBackground

val MutedText: Color
    @Composable
    get() = MaterialTheme.colorScheme.onSurfaceVariant

val RedExpense = Color(0xFFBA1A1A)           // Professional M3 Red
val GreenIncome = Color(0xFF006C47)          // Professional M3 Green
val BlueCard = Color(0xFF0061A4)             // Professional M3 Blue

