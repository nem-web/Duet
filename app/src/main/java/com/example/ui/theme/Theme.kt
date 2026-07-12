package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val RoseLight = lightColorScheme(
    primary = RosePrimaryLight,
    secondary = RoseSecondaryLight,
    tertiary = RoseTertiaryLight,
    background = RoseBackgroundLight,
    surface = RoseSurfaceLight,
    onSurface = RoseOnSurfaceLight,
    surfaceVariant = RoseSurfaceLight
)

private val RoseDark = darkColorScheme(
    primary = RosePrimaryDark,
    secondary = RoseSecondaryDark,
    tertiary = RoseTertiaryDark,
    background = RoseBackgroundDark,
    surface = RoseSurfaceDark,
    onSurface = RoseOnSurfaceDark,
    surfaceVariant = RoseSurfaceDark
)

private val SunsetLight = lightColorScheme(
    primary = SunsetPrimaryLight,
    secondary = SunsetSecondaryLight,
    background = SunsetBackgroundLight,
    surface = SunsetSurfaceLight,
    onSurface = SunsetOnSurfaceLight,
    surfaceVariant = SunsetSurfaceLight
)

private val SunsetDark = darkColorScheme(
    primary = SunsetPrimaryDark,
    secondary = SunsetSecondaryDark,
    background = SunsetBackgroundDark,
    surface = SunsetSurfaceDark,
    onSurface = SunsetOnSurfaceDark,
    surfaceVariant = SunsetSurfaceDark
)

private val LavenderLight = lightColorScheme(
    primary = LavenderPrimaryLight,
    secondary = LavenderSecondaryLight,
    background = LavenderBackgroundLight,
    surface = LavenderSurfaceLight,
    onSurface = LavenderOnSurfaceLight,
    surfaceVariant = LavenderSurfaceLight
)

private val LavenderDark = darkColorScheme(
    primary = LavenderPrimaryDark,
    secondary = LavenderSecondaryDark,
    background = LavenderBackgroundDark,
    surface = LavenderSurfaceDark,
    onSurface = LavenderOnSurfaceDark,
    surfaceVariant = LavenderSurfaceDark
)

private val OceanLight = lightColorScheme(
    primary = OceanPrimaryLight,
    secondary = OceanSecondaryLight,
    background = OceanBackgroundLight,
    surface = OceanSurfaceLight,
    onSurface = OceanOnSurfaceLight,
    surfaceVariant = OceanSurfaceLight
)

private val OceanDark = darkColorScheme(
    primary = OceanPrimaryDark,
    secondary = OceanSecondaryDark,
    background = OceanBackgroundDark,
    surface = OceanSurfaceDark,
    onSurface = OceanOnSurfaceDark,
    surfaceVariant = OceanSurfaceDark
)

@Composable
fun DuetTheme(
    themeId: String = "warm_rose",
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeId) {
        "sunset_glow" -> if (darkTheme) SunsetDark else SunsetLight
        "lavender_dream" -> if (darkTheme) LavenderDark else LavenderLight
        "ocean_breeze" -> if (darkTheme) OceanDark else OceanLight
        else -> if (darkTheme) RoseDark else RoseLight // "warm_rose" as default
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
