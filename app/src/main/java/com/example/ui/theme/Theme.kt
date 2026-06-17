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

private val HighDensityColorScheme =
  lightColorScheme(
    primary = Color(0xFF6750A4),
    secondary = Color(0xFFE8DEF8),
    tertiary = Color(0xFFD0BCFF),
    background = MidnightBg,
    surface = MidnightSurface,
    onBackground = TextLight,
    onSurface = TextLight
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Disable default dynamic coloring to guarantee the custom theme identity
  content: @Composable () -> Unit,
) {
  // Always active High Density color scheme
  val colorScheme = HighDensityColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
