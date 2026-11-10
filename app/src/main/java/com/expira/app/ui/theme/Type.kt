package com.expira.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.expira.app.R

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private fun inter(size: Int, weight: FontWeight, lineHeight: Int? = null) = TextStyle(
    fontFamily = Inter,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight?.sp ?: TextStyle.Default.lineHeight,
)

/**
 * Text styles used by the Figma frames, mapped onto Material 3 roles.
 * Medium variants of a role are derived at the call site with `copy(fontWeight = ...)`.
 */
val ExpiraTypography = Typography(
    headlineLarge = inter(28, FontWeight.Bold),          // result date
    headlineMedium = inter(26, FontWeight.Bold),         // tab screen header
    headlineSmall = inter(22, FontWeight.Bold),          // failure title, preview date
    titleLarge = inter(18, FontWeight.SemiBold),         // top bar title, card title
    titleMedium = inter(16, FontWeight.SemiBold),        // section title
    labelLarge = inter(15, FontWeight.SemiBold),         // buttons, list item title
    bodyLarge = inter(15, FontWeight.Normal),            // input text, step label
    bodyMedium = inter(14, FontWeight.Normal, 20),       // descriptions
    bodySmall = inter(13, FontWeight.Normal),            // secondary lines, timings
    labelMedium = inter(13, FontWeight.Medium),          // field labels
    labelSmall = inter(12, FontWeight.SemiBold),         // chips, bottom nav
)
