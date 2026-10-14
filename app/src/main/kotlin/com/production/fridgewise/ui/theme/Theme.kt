package com.production.fridgewise.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = LeafGreenDark,
    onPrimary = InkBlack,
    primaryContainer = DarkChip,
    onPrimaryContainer = LeafGreenDark,
    secondary = GrayText,
    tertiary = SoonYellowBg,
    onTertiary = SoonYellow,
    error = Color(0xFFF2B8B5),
    background = DarkBg,
    onBackground = PaperWhite,
    surface = DarkSurface,
    onSurface = PaperWhite,
    surfaceVariant = DarkChip,
    onSurfaceVariant = ChipGray
)

private val LightColorScheme = lightColorScheme(
    primary = LeafGreen,
    onPrimary = PaperWhite,
    primaryContainer = LeafGreenPale,
    onPrimaryContainer = InkBlack,
    secondary = GrayText,
    onSecondary = PaperWhite,
    tertiary = SoonYellowBg,
    onTertiary = SoonYellow,
    error = ExpiryRed,
    onError = PaperWhite,
    errorContainer = ExpiryRedBg,
    background = MistGray,
    onBackground = InkBlack,
    surface = PaperWhite,
    onSurface = InkBlack,
    surfaceVariant = ChipGray,
    onSurfaceVariant = GrayText
)

@Composable
fun MyAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // ponytail: tắt dynamic color để giữ palette trắng/xám/đen/xanh lá, bật lại khi muốn theo hệ thống
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
