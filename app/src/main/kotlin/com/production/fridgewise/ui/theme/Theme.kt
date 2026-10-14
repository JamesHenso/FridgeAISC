package com.production.fridgewise.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = SkyBlue,
    onPrimary = InkBlack,
    primaryContainer = SkyBluePale,
    onPrimaryContainer = InkBlack,
    secondary = GrayText,
    onSecondary = PaperWhite,
    tertiary = SoonYellowBg,
    onTertiary = SoonYellow,
    error = ExpiryRed,
    onError = PaperWhite,
    errorContainer = ExpiryRedBg,
    background = MistBlueGray,
    onBackground = InkBlack,
    surface = PaperWhite,
    onSurface = InkBlack,
    surfaceVariant = ChipGray,
    onSurfaceVariant = GrayText,
    inverseSurface = InkBlack,
    inverseOnSurface = PaperWhite
)

// ponytail: chỉ light theme theo yêu cầu (chữ đen, nền trắng/xám/xanh) — thêm dark khi user đổi ý
@Composable
fun MyAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
