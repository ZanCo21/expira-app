package com.expira.app.ui.review

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.expira.app.R
import com.expira.app.ui.components.OutlinedCard
import com.expira.app.ui.components.PrimaryButton
import com.expira.app.ui.components.SecondaryButton
import com.expira.app.ui.theme.ExpiraColors

/** Figma "Gagal Membaca" (4:57): low confidence or no candidate, the date is not shown. */
@Composable
fun FailedScreen(
    onBack: () -> Unit,
    onRetake: () -> Unit,
    onManualInput: () -> Unit,
) {
    ReviewScaffold(
        title = stringResource(R.string.result_title),
        onBack = onBack,
        contentSpacing = 16.dp,
        contentAlignment = Alignment.CenterHorizontally,
        buttons = {
            PrimaryButton(text = stringResource(R.string.failed_retake), onClick = onRetake)
            SecondaryButton(text = stringResource(R.string.failed_manual), onClick = onManualInput)
        },
    ) {
        Box(
            modifier = Modifier.padding(top = 4.dp).size(72.dp).background(ExpiraColors.DangerBg, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_alert),
                contentDescription = null,
                tint = ExpiraColors.Danger,
                modifier = Modifier.size(32.dp),
            )
        }
        Text(
            text = stringResource(R.string.failed_title),
            style = MaterialTheme.typography.headlineSmall,
            color = ExpiraColors.Text,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.failed_body),
            style = MaterialTheme.typography.bodyMedium,
            color = ExpiraColors.TextMuted,
            textAlign = TextAlign.Center,
        )
        OutlinedCard(spacing = 14.dp) {
            listOf(R.string.failed_tip_light, R.string.failed_tip_closer, R.string.failed_tip_steady).forEach { tip ->
                Tip(stringResource(tip))
            }
        }
    }
}

@Composable
private fun Tip(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_check_18),
            contentDescription = null,
            tint = ExpiraColors.Primary,
            modifier = Modifier.padding(top = 1.dp).size(18.dp),
        )
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = ExpiraColors.Text)
    }
}
