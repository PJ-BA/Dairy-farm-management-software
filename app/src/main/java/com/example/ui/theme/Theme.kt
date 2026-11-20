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

private val LightColorScheme = lightColorScheme(
    primary = EmeraldDeep,
    onPrimary = Color.White,
    primaryContainer = EmeraldSoft,
    onPrimaryContainer = EmeraldDeep,
    secondary = AmberWarm,
    onSecondary = Color.White,
    secondaryContainer = AmberContainer,
    onSecondaryContainer = AmberDark,
    tertiary = SagePrimary,
    onTertiary = Color.White,
    tertiaryContainer = SageContainer,
    onTertiaryContainer = EmeraldDeep,
    background = CreamBackground,
    onBackground = InkDark,
    surface = WarmPaper,
    onSurface = InkDark,
    surfaceVariant = LinenSurface,
    onSurfaceVariant = InkMuted,
    outline = LinenBorder,
    outlineVariant = LinenSurface,
    error = LossCrimson,
    onError = Color.White,
    errorContainer = LossCrimsonSoft,
    onErrorContainer = LossCrimson
)

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldLight,
    onPrimary = EmeraldDeep,
    primaryContainer = EmeraldMedium,
    onPrimaryContainer = EmeraldSoft,
    secondary = AmberGold,
    onSecondary = TimberDarkBg,
    secondaryContainer = Color(0xFF4A300D),
    onSecondaryContainer = AmberContainer,
    tertiary = EmeraldLight,
    onTertiary = TimberDarkBg,
    tertiaryContainer = TimberDarkSurfaceHigh,
    onTertiaryContainer = EmeraldSoft,
    background = TimberDarkBg,
    onBackground = CreamText,
    surface = TimberDarkSurface,
    onSurface = CreamText,
    surfaceVariant = TimberDarkSurfaceHigh,
    onSurfaceVariant = SageMutedText,
    outline = TimberBorder,
    outlineVariant = TimberDarkSurfaceHigh,
    error = LossCrimsonLight,
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = LossCrimsonSoft
)

val DairyShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun DairyLedgerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DairyTypography,
        shapes = DairyShapes,
        content = content
    )
}
