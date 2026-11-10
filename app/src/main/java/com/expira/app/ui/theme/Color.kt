package com.expira.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Design tokens from the Figma file (see CLAUDE.md "Token warna").
 * Status colors (success / warning / danger) mark Aman / Mendekati / Kedaluwarsa
 * and must stay fixed regardless of theme.
 */
object ExpiraColors {
    val Primary = Color(0xFF1D4ED8)
    val PrimaryContainer = Color(0xFFDBE7FE)
    val OnPrimary = Color(0xFFFFFFFF)
    val Background = Color(0xFFF3F6FB)
    val Surface = Color(0xFFFFFFFF)
    val Text = Color(0xFF0F1B2D)
    val TextMuted = Color(0xFF5A6A82)
    val Outline = Color(0xFFD3DBE8)

    val Success = Color(0xFF15803D)
    val SuccessBg = Color(0xFFDCFCE7)
    val Warning = Color(0xFFB45309)
    val WarningBg = Color(0xFFFEF3C7)
    val Danger = Color(0xFFB91C1C)
    val DangerBg = Color(0xFFFEE2E2)

    val CameraBg = Color(0xFF0A1220)
}
