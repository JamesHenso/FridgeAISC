package com.production.fridgewise.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Green40,
    onPrimary = Cream,
    primaryContainer = Green90,
    onPrimaryContainer = Green10,
    secondary = Sage40,
    onSecondary = Cream,
    secondaryContainer = Sage90,
    onSecondaryContainer = Sage10,
    tertiary = Amber40,
    onTertiary = Cream,
    tertiaryContainer = Amber90,
    onTertiaryContainer = Amber10,
    error = Red40,
    onError = Cream,
    errorContainer = Red90,
    onErrorContainer = Red10,
    background = Cream,
    onBackground = Ink,
    surface = Cream,
    onSurface = Ink,
    surfaceVariant = CreamHighest,
    onSurfaceVariant = InkSurfaceVariant,
    outline = InkOutline,
    outlineVariant = InkOutlineVariant,
    scrim = Ink.copy(alpha = 1f),
    inverseSurface = InkInverseSurface,
    inverseOnSurface = InkInverseOnSurface,
    inversePrimary = Green80,
    surfaceTint = Green40,
    surfaceBright = Cream,
    surfaceDim = CreamDim,
    surfaceContainerLowest = androidx.compose.ui.graphics.Color.White,
    surfaceContainerLow = CreamLow,
    surfaceContainer = CreamContainer,
    surfaceContainerHigh = CreamHigh,
    surfaceContainerHighest = CreamHighest
)

private val DarkColors = darkColorScheme(
    primary = Green80,
    onPrimary = Green20,
    primaryContainer = Green30,
    onPrimaryContainer = Green90,
    secondary = Sage80,
    onSecondary = Sage20,
    secondaryContainer = Sage30,
    onSecondaryContainer = Sage90,
    tertiary = Amber80,
    onTertiary = Amber20,
    tertiaryContainer = Amber30,
    onTertiaryContainer = Amber90,
    error = Red80,
    onError = Red20,
    errorContainer = Red30,
    onErrorContainer = Red90,
    background = Night,
    onBackground = NightInk,
    surface = Night,
    onSurface = NightInk,
    surfaceVariant = NightHighest,
    onSurfaceVariant = NightInkSurfaceVariant,
    outline = NightOutline,
    outlineVariant = NightInkSurfaceVariant.copy(alpha = 0.35f),
    scrim = androidx.compose.ui.graphics.Color.Black,
    inverseSurface = NightInk,
    inverseOnSurface = NightHighest,
    inversePrimary = Green40,
    surfaceTint = Green80,
    surfaceBright = NightBright,
    surfaceDim = Night,
    surfaceContainerLowest = NightLow,
    surfaceContainerLow = NightLow,
    surfaceContainer = NightContainer,
    surfaceContainerHigh = NightHigh,
    surfaceContainerHighest = NightHighest
)

@Composable
fun FridgeWiseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = FridgeWiseTypography,
        shapes = FridgeWiseShapes,
        content = content
    )
}
