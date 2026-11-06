package com.example.androidreminderapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = ForestDark,
    onPrimary = Color(0xFF17372E),
    primaryContainer = Color(0xFF294B40),
    onPrimaryContainer = Color(0xFFD3EBDD),
    secondary = SageDark,
    onSecondary = Color(0xFF243A32),
    secondaryContainer = Color(0xFF334B41),
    onSecondaryContainer = Color(0xFFD1E5D9),
    tertiary = AmberDark,
    onTertiary = Color(0xFF48290A),
    background = PageDark,
    onBackground = Color(0xFFE0E9E3),
    surface = SurfaceDark,
    onSurface = Color(0xFFE0E9E3),
    surfaceVariant = Color(0xFF293630),
    onSurfaceVariant = Color(0xFFBECBC2),
    outline = Color(0xFF87968D)
)

private val LightColorScheme = lightColorScheme(
    primary = Forest,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9EDE4),
    onPrimaryContainer = Color(0xFF183D32),
    secondary = Sage,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE1EBE5),
    onSecondaryContainer = Color(0xFF2D463B),
    tertiary = Amber,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE9D1),
    onTertiaryContainer = Color(0xFF61380F),
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
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
