package com.expira.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.expira.app.R
import com.expira.app.ui.theme.ExpiraColors

enum class StepIndicator { Done, Running, Pending }

/** One row of the "Tahapan" card on the Memproses screen. */
@Composable
fun StepRow(
    label: String,
    trailing: String,
    indicator: StepIndicator,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepDot(indicator)
        val isPending = indicator == StepIndicator.Pending
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (isPending) FontWeight.Normal else FontWeight.Medium,
            ),
            color = if (isPending) ExpiraColors.TextMuted else ExpiraColors.Text,
            modifier = Modifier.weight(1f),
        )
        Text(text = trailing, style = MaterialTheme.typography.bodySmall, color = ExpiraColors.TextMuted)
    }
}

@Composable
private fun StepDot(indicator: StepIndicator) {
    val base = Modifier.size(28.dp)
    when (indicator) {
        StepIndicator.Done -> Box(
            modifier = base.background(ExpiraColors.Success, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_check_16),
                contentDescription = null,
                tint = ExpiraColors.OnPrimary,
                modifier = Modifier.size(16.dp),
            )
        }
        StepIndicator.Running -> Box(
            modifier = base
                .background(ExpiraColors.PrimaryContainer, CircleShape)
                .border(2.dp, ExpiraColors.Primary, CircleShape),
        )
        StepIndicator.Pending -> Box(modifier = base.border(2.dp, ExpiraColors.Outline, CircleShape))
    }
}
