package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = EmberBright,
    onPrimary = Color(0xFF1F0C00),
    primaryContainer = Color(0xFF4F2107),
    onPrimaryContainer = Color(0xFFFFDBC9),
    secondary = Color(0xFF7CB7E8),
    onSecondary = Color(0xFF00243C),
    secondaryContainer = Color(0xFF1D354D),
    onSecondaryContainer = Color(0xFFD5E9FF),
    tertiary = PlantEmeraldDark,
    onTertiary = Color(0xFF00291B),
    tertiaryContainer = Color(0xFF0B3F2D),
    onTertiaryContainer = Color(0xFFB5F5DC),
    error = AlertCrimsonDark,
    onError = Color(0xFF3E0003),
    errorContainer = Color(0xFF5A1215),
    onErrorContainer = Color(0xFFFFDAD6),
    background = DarkCanvas,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = DarkOnSurfaceMuted,
    outline = DarkOutline,
    outlineVariant = Color(0xFF223142)
)

private val LightColorScheme = lightColorScheme(
    primary = FoundrySteel,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE9F7),
    onPrimaryContainer = FoundryNavy,
    secondary = EmberOrange,
    onSecondary = Color.White,
    secondaryContainer = EmberLight,
    onSecondaryContainer = Color(0xFF5D2100),
    tertiary = PlantEmerald,
    onTertiary = Color.White,
    tertiaryContainer = PlantEmeraldContainer,
    onTertiaryContainer = Color(0xFF043B27),
    error = AlertCrimson,
    onError = Color.White,
    errorContainer = AlertCrimsonContainer,
    onErrorContainer = Color(0xFF690005),
    background = SteelSurfaceLight,
    onBackground = LightOnSurface,
    surface = SteelCardLight,
    onSurface = LightOnSurface,
    surfaceVariant = SteelSurfaceVariantLight,
    onSurfaceVariant = LightOnSurfaceMuted,
    outline = SteelBorderLight,
    outlineVariant = Color(0xFFE1E8F0)
)

val CciShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun EvonithCciTheme(
    themeMode: String = "SYSTEM",
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode.uppercase()) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemInDarkTheme()
    }
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = CciShapes,
        content = content
    )
}
