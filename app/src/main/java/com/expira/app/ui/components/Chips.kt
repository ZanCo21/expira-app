package com.expira.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.expira.app.R
import com.expira.app.domain.ExpiryStatus
import com.expira.app.ui.theme.ExpiraColors
import kotlin.math.roundToInt

private val ChipShape = RoundedCornerShape(12.dp)

/** Figma chip components (Chip/Aman etc.): px12 py4, radius 12, SemiBold 12. */
@Composable
fun Chip(text: String, contentColor: Color, containerColor: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = contentColor,
        modifier = modifier
            .background(containerColor, ChipShape)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}

/** Aman / Mendekati / Kedaluwarsa; colours are fixed status tokens. */
@Composable
fun StatusChip(status: ExpiryStatus, modifier: Modifier = Modifier) {
    val (label, fg, bg) = when (status) {
        ExpiryStatus.SAFE -> Triple(R.string.status_safe, ExpiraColors.Success, ExpiraColors.SuccessBg)
        ExpiryStatus.NEAR -> Triple(R.string.status_near, ExpiraColors.Warning, ExpiraColors.WarningBg)
        ExpiryStatus.EXPIRED -> Triple(R.string.status_expired, ExpiraColors.Danger, ExpiraColors.DangerBg)
    }
    Chip(text = stringResource(label), contentColor = fg, containerColor = bg, modifier = modifier)
}

/** "Keyakinan 94%" on primary-container. */
@Composable
fun ConfidenceChip(score: Double, modifier: Modifier = Modifier) {
    Chip(
        text = stringResource(R.string.confidence_percent, (score * 100).roundToInt()),
        contentColor = ExpiraColors.Primary,
        containerColor = ExpiraColors.PrimaryContainer,
        modifier = modifier,
    )
}
