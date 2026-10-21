package com.production.fridgewise.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.production.fridgewise.R

/** Plus Jakarta Sans keeps the app friendly without sacrificing legibility at small sizes. */
val FridgeWiseFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold)
)

private fun base(
    weight: FontWeight = FontWeight.Normal,
    size: Int,
    lineHeight: Int,
    tracking: Double = 0.0
) = TextStyle(
    fontFamily = FridgeWiseFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.sp
)

val FridgeWiseTypography = Typography(
    displayLarge = base(FontWeight.Bold, 57, 64, (-0.25)),
    displayMedium = base(FontWeight.Bold, 45, 52, 0.0),
    displaySmall = base(FontWeight.Bold, 36, 44, 0.0),
    headlineLarge = base(FontWeight.Bold, 32, 40, (-0.2)),
    headlineMedium = base(FontWeight.Bold, 28, 36, (-0.1)),
    headlineSmall = base(FontWeight.SemiBold, 24, 32, 0.0),
    titleLarge = base(FontWeight.SemiBold, 22, 28, 0.0),
    titleMedium = base(FontWeight.SemiBold, 16, 24, 0.15),
    titleSmall = base(FontWeight.Medium, 14, 20, 0.1),
    bodyLarge = base(FontWeight.Normal, 16, 24, 0.5),
    bodyMedium = base(FontWeight.Normal, 14, 20, 0.25),
    bodySmall = base(FontWeight.Normal, 12, 16, 0.4),
    labelLarge = base(FontWeight.SemiBold, 14, 20, 0.1),
    labelMedium = base(FontWeight.Medium, 12, 16, 0.5),
    labelSmall = base(FontWeight.Medium, 11, 16, 0.5)
)
