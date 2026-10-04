package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val JarvisCyan = Color(0xFF00E5FF)
val JarvisBlue = Color(0xFF2979FF)
val JarvisPurple = Color(0xFF7C4DFF)
val JarvisObsidianBg = Color(0xFF070B14)
val JarvisSurface = Color(0xFF0D1424)
val JarvisSurfaceVariant = Color(0xFF162035)

private val JarvisDarkColorScheme = darkColorScheme(
  primary = JarvisCyan,
  onPrimary = Color(0xFF00363D),
  primaryContainer = Color(0xFF004F58),
  onPrimaryContainer = Color(0xFF80F4FF),
  secondary = JarvisBlue,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFF003E9A),
  onSecondaryContainer = Color(0xFFD6E3FF),
  tertiary = JarvisPurple,
  onTertiary = Color.White,
  tertiaryContainer = Color(0xFF3800B5),
  onTertiaryContainer = Color(0xFFEADBFF),
  background = JarvisObsidianBg,
  onBackground = Color(0xFFE2E8F0),
  surface = JarvisSurface,
  onSurface = Color(0xFFF1F5F9),
  surfaceVariant = JarvisSurfaceVariant,
  onSurfaceVariant = Color(0xFF94A3B8),
  outline = Color(0xFF334155),
  outlineVariant = Color(0xFF1E293B),
  error = Color(0xFFFF5252),
  onError = Color.White,
  errorContainer = Color(0xFF93000A),
  onErrorContainer = Color(0xFFFFDAD6)
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = JarvisDarkColorScheme,
    typography = Typography,
    content = content
  )
}
