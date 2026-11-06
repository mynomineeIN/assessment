package com.example.androidreminderapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = ForestDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF641B5C),
    onPrimaryContainer = Color(0xFFFFD6F8),
    secondary = SageDark,
    onSecondary = Color(0xFF34243A),
    secondaryContainer = Color(0xFF3B2942),
    onSecondaryContainer = Color(0xFFF4DDF2),
    tertiary = AmberDark,
    onTertiary = Color(0xFF35204B),
    background = PageDark,
    onBackground = Color(0xFFF7EDF8),
    surface = SurfaceDark,
    onSurface = Color(0xFFF7EDF8),
    surfaceVariant = Color(0xFF302438),
    onSurfaceVariant = Color(0xFFC9B9CB),
    outline = Color(0xFF927D97),
    outlineVariant = OutlineSoft,
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF5C2428),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFB91AA4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF9D7F2),
    onPrimaryContainer = Color(0xFF520B48),
    secondary = Color(0xFF69536C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF0DDF1),
    onSecondaryContainer = Color(0xFF34243A),
    tertiary = Color(0xFF695083),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEEDCFF),
    onTertiaryContainer = Color(0xFF35204B),
    background = Page,
    onBackground = Color(0xFF1E2924),
    surface = Color.White,
    onSurface = Color(0xFF1E2924),
    surfaceVariant = Color(0xFFE9EFEB),
    onSurfaceVariant = Color(0xFF58665F),
    outline = Color(0xFF77857D),
    outlineVariant = OutlineSoft
)

private val AppShapes = Shapes(
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(26.dp)
)

@Composable
fun AndroidReminderAppTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
