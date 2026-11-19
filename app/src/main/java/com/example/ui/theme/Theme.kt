package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = Blue900,
    onPrimary = Color.White,
    primaryContainer = IceBlue100,
    onPrimaryContainer = Blue950,
    secondary = Sky500,
    onSecondary = Color.White,
    secondaryContainer = IceBlue100,
    onSecondaryContainer = Blue800Text,
    tertiary = Emerald600,
    onTertiary = Color.White,
    background = IceBlue50,
    onBackground = Slate800,
    surface = Color.White,
    onSurface = Slate800,
    surfaceVariant = IceBlue50,
    onSurfaceVariant = Slate500,
    outline = Sky300,
    error = Red600,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Sky400,
    onPrimary = Blue950,
    primaryContainer = Blue800,
    onPrimaryContainer = Sky300,
    secondary = Sky500,
    onSecondary = Color.White,
    secondaryContainer = Blue700,
    onSecondaryContainer = Color.White,
    tertiary = Emerald400,
    onTertiary = Blue950,
    background = Blue950,
    onBackground = Color.White,
    surface = Blue900,
    onSurface = Color.White,
    surfaceVariant = Blue800,
    onSurfaceVariant = Slate300,
    outline = Blue700,
    error = Red600,
    onError = Color.White
)

private val SecurityShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun SecurityBpsTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SecurityTypography,
        shapes = SecurityShapes,
        content = content
    )
}
