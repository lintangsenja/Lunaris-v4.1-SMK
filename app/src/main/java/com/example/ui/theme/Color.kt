package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset

// Indigo & Slate theme for High Density Gudang SMANSABOB
val IndigoPrimary = Color(0xFF4F46E5)
val IndigoDark = Color(0xFF312E81)
val SlateAccent = Color(0xFF334155)
val LightSlateBg = Color(0xFFF7F9FC)

// Pastel Premium Theme Colors
val PastelSoftBlue = Color(0xFFD0E9FF)
val PastelLavender = Color(0xFFEAE2FC)
val SoftCream = Color(0xFFFFFDF9)

// Contrast & Label Colors
val DeepPurpleText = Color(0xFF3B0764)
val SoftGoldText = Color(0xFFCA8A04)
val CarbonBlackText = Color(0xFF18181B)

// Glassmorphism transparent colors
val GlassWhite = Color(0xCCFFFFFF) // 80% opacity white
val GlassWhiteMore = Color(0xE6FFFFFF) // 90% opacity white for higher contrast
val GlassBorder = Color(0x66FFFFFF) // soft white border
val GlassLavender = Color(0xD9E9E2FC) // 85% opacity Lavender for KPI cards
val BrightGold = Color(0xFFEAB308) // Vibrant Gold/Yellow for borders

val PrimaryLight = Color(0xFF5B21B6) // Deep Purple as primary
val SecondaryLight = Color(0xFF18181B) // Carbon Black
val TertiaryLight = Color(0xFFCA8A04) // Soft Gold

val PrimaryDark = Color(0xFFC084FC) // Pastel Purple
val SecondaryDark = Color(0xFFE2E8F0) 
val TertiaryDark = Color(0xFFFDE047) 

val BackgroundDark = Color(0xFF110C1A)
val SurfaceDark = Color(0xFF1F1A29)
val BackgroundLight = Color(0xFFFAF9F6)
val SurfaceLight = Color(0xFFFFFFFF)

fun Modifier.pastelGradientBackground(isDark: Boolean = false): Modifier = this.drawBehind {
    val colors = listOf(
        Color(0xFFF5EEFF), // Soft Lavender Purple
        Color(0xFFE0F2FE), // Light Sea Blue
        Color(0xFFE0F7F6)  // Soft Aqua Sea
    )

    // 1. Draw main linear gradient (Lavender purple mix sea blue and soft aqua)
    val mainBrush = Brush.linearGradient(
        colors = colors,
        start = Offset(0f, 0f),
        end = Offset(size.width, size.height)
    )
    drawRect(brush = mainBrush)

    val glow1 = Color(0x4DF4EEFF)
    val glow2 = Color(0x4DE0F2FE)
    val glow3 = Color(0x3338BDF8)

    // 2. Add subtle low-opacity radial glows in corners
    // Top-left soft lavender glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(glow1, Color.Transparent),
            center = Offset(0f, 0f),
            radius = size.width * 0.7f
        ),
        center = Offset(0f, 0f),
        radius = size.width * 0.7f
    )
    // Bottom-right soft sea blue glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(glow2, Color.Transparent),
            center = Offset(size.width, size.height),
            radius = size.width * 0.8f
        ),
        center = Offset(size.width, size.height),
        radius = size.width * 0.8f
    )
    // Top-right subtle sky blue / marine glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(glow3, Color.Transparent),
            center = Offset(size.width, 0f),
            radius = size.width * 0.6f
        ),
        center = Offset(size.width, 0f),
        radius = size.width * 0.6f
    )
}



