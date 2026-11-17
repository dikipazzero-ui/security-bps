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
    primary = Navy900,
    onPrimary = Color.White,
    primaryContainer = Navy100,
    onPrimaryContainer = Navy950,
    secondary = Gold500,
    onSecondary = Navy950,
    secondaryContainer = GoldContainerLight,
    onSecondaryContainer = GoldOnContainerLight,
    tertiary = Emerald600,
    onTertiary = Color.White,
    background = Slate100,
    onBackground = Slate800,
    surface = Color.White,
    onSurface = Slate800,
    surfaceVariant = Slate50,
    onSurfaceVariant = Slate500,
    outline = Slate200,
    error = Red600,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Gold400,
    onPrimary = Navy950,
    primaryContainer = Navy800,
    onPrimaryContainer = Gold400,
    secondary = Gold500,
    onSecondary = Navy950,
    secondaryContainer = Navy700,
    onSecondaryContainer = Color.White,
    tertiary = Emerald400,
    onTertiary = Navy950,
    background = Navy950,
    onBackground = Color.White,
    surface = Navy900,
    onSurface = Color.White,
    surfaceVariant = Navy800,
    onSurfaceVariant = Slate400,
    outline = Navy700,
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
