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
    onPrimary = Color(0xFF002E69),
    primaryContainer = Color(0xFF214480), // Deep Elegant Blue Container
    onPrimaryContainer = Color(0xFFD8E2FF),
    secondary = Color(0xFFBFC6DC),       // Soft Slate
    onSecondary = Color(0xFF293041),
    secondaryContainer = Color(0xFF3F4759),
    onSecondaryContainer = Color(0xFFDAE2F9),
    tertiary = Color(0xFFF2B8B5),        // Soft Coral Accent
    onTertiary = Color(0xFF601410),
    tertiaryContainer = Color(0xFF4D381E),
    onTertiaryContainer = Color(0xFFFFE088),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF111318),      // Deep Charcoal
    onBackground = Color(0xFFE2E2E9),    // Soft White Text
    surface = Color(0xFF191C22),         // Slightly elevated card base
    onSurface = Color(0xFFE2E2E9),
    surfaceVariant = Color(0xFF23262E),  // Card variant
    onSurfaceVariant = Color(0xFFC4C6D0), // Muted text
    surfaceContainer = Color(0xFF1E2128),
    surfaceContainerHigh = Color(0xFF282C35),
    surfaceContainerHighest = Color(0xFF333741),
    outline = Color(0xFF474B56),
    outlineVariant = Color(0xFF2C3039)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Color(0xFF2B5EA7),         // Deep Slate Blue
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8E2FF),
    onPrimaryContainer = Color(0xFF001A42),
    secondary = Color(0xFF555F71),       // Soft Slate
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDAE2F9),
    onSecondaryContainer = Color(0xFF121B2C),
    tertiary = Color(0xFF725572),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFED7FC),
    onTertiaryContainer = Color(0xFF2B132C),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF9F9FC),      // Crisp off-white
    onBackground = Color(0xFF1A1C20),
    surface = Color(0xFFFFFFFF),         // Clean white card
    onSurface = Color(0xFF1A1C20),
    surfaceVariant = Color(0xFFF0F1F7),  // Soft tonal container
    onSurfaceVariant = Color(0xFF44474E), // Muted slate text
    surfaceContainer = Color(0xFFF3F4FA),
    surfaceContainerHigh = Color(0xFFECEEF5),
    surfaceContainerHighest = Color(0xFFE6E8EF),
    outline = Color(0xFF757780),
    outlineVariant = Color(0xFFD5D7E1)
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
