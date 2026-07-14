package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = Color(0xFFADC6FF),         // Bright Soft Blue
    secondary = Color(0xFF3B4858),       // Dark container
    tertiary = Color(0xFFD6E3FF),         // Highlight text/accent
    background = Color(0xFF111318),       // Charcoal background
    surface = Color(0xFF1B1D22),          // Slightly lighter card
    surfaceVariant = Color(0xFF23252A),   // Lighter card
    onPrimary = Color(0xFF002F64),
    onSecondary = Color(0xFFE2E2E9),
    onTertiary = Color(0xFF001B3E),
    onBackground = Color(0xFFE2E2E9),     // Soft white
    onSurface = Color(0xFFE2E2E9),
    onSurfaceVariant = Color(0xFFC4C6D0)  // Muted gray
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Color(0xFF435E91),         // Deep Slate Blue
    secondary = Color(0xFFE0E2EC),       // Soft light container
    tertiary = Color(0xFF001B3E),         // Dark Accent
    background = Color(0xFFFDFBFF),       // Clean light background
    surface = Color(0xFFFFFFFF),          // White card
    surfaceVariant = Color(0xFFF2F0F4),   // Soft grey surface
    onPrimary = Color.White,
    onSecondary = Color(0xFF001B3E),
    onTertiary = Color.White,
    onBackground = Color(0xFF1B1B1F),     // Charcoal black
    onSurface = Color(0xFF1B1B1F),
    onSurfaceVariant = Color(0xFF44474E)  // Muted Slate
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(), // Dynamically adapt to system settings
  dynamicColor: Boolean = false, // Use our cohesive professional palette
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
