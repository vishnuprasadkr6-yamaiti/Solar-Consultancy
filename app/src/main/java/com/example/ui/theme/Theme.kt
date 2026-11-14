package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = SolarGreenPrimary,
    onPrimary = SolarGreenOnPrimary,
    primaryContainer = SolarGreenPrimaryContainer,
    onPrimaryContainer = SolarGreenOnPrimaryContainer,
    secondary = SolarSageSecondary,
    onSecondary = SolarSageOnSecondary,
    secondaryContainer = SolarSageSecondaryContainer,
    onSecondaryContainer = SolarSageOnSecondaryContainer,
    tertiary = SolarAmberTertiary,
    onTertiary = SolarAmberOnTertiary,
    tertiaryContainer = SolarAmberTertiaryContainer,
    onTertiaryContainer = SolarAmberOnTertiaryContainer,
    background = WarmOffWhiteBackground,
    onBackground = CharcoalOnSurface,
    surface = CleanWhiteSurface,
    onSurface = CharcoalOnSurface,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = MutedCharcoalOnSurfaceVariant,
    surfaceContainerLowest = CleanWhiteSurface,
    surfaceContainerLow = WarmOffWhiteBackground,
    surfaceContainer = WarmSurfaceVariant,
    outline = SubtleBorderOutline,
    outlineVariant = SubtleOutlineVariant,
    error = ErrorRed,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun SolarDeskTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
