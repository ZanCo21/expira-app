package com.expira.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ExpiraColorScheme = lightColorScheme(
    primary = ExpiraColors.Primary,
    onPrimary = ExpiraColors.OnPrimary,
    primaryContainer = ExpiraColors.PrimaryContainer,
    onPrimaryContainer = ExpiraColors.Primary,
    background = ExpiraColors.Background,
    onBackground = ExpiraColors.Text,
    surface = ExpiraColors.Surface,
    onSurface = ExpiraColors.Text,
    onSurfaceVariant = ExpiraColors.TextMuted,
    outline = ExpiraColors.Outline,
    error = ExpiraColors.Danger,
    errorContainer = ExpiraColors.DangerBg,
)

/** The Figma design defines a single light theme, so no dark scheme is provided. */
@Composable
fun ExpiraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ExpiraColorScheme,
        typography = ExpiraTypography,
        content = content,
    )
}
