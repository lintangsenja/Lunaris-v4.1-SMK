package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = Color.White,
    secondary = SecondaryLight,
    onSecondary = Color.White,
    tertiary = TertiaryLight,
    background = Color.Transparent,
    surface = SurfaceLight,
    onBackground = SecondaryLight,
    onSurface = SecondaryLight
)

@Composable
fun LunarisTheme(
    darkTheme: Boolean = false, // Forces light mode globally
    dynamicColor: Boolean = false, // Disable dynamic colors to enforce school brand colors
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
