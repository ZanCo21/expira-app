package com.expira.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.expira.app.R
import com.expira.app.domain.ExpiryStatus
import com.expira.app.extraction.CalendarDate
import com.expira.app.ui.theme.ExpiraColors

/** Display data for one history row. */
data class ScanRowUi(
    val id: Long,
    val expiry: CalendarDate,
    val status: ExpiryStatus,
    val wasCorrected: Boolean,
)

/**
 * Figma history row ("Baris ..." on Beranda and Riwayat). The product name arrives
 * with F-14 in Sprint 5; until then the title is "Produk tanpa nama" (PRD FR-22).
 */
@Composable
fun ScanRow(item: ScanRowUi, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(ExpiraColors.Surface, shape)
            .border(1.dp, ExpiraColors.Outline, shape)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.product_unnamed),
                style = MaterialTheme.typography.labelLarge,
                color = ExpiraColors.Text,
            )
            val expiry = stringResource(R.string.expiry_short, item.expiry.format())
            Text(
                text = if (item.wasCorrected) stringResource(R.string.history_corrected, expiry) else expiry,
                style = MaterialTheme.typography.bodySmall,
                color = ExpiraColors.TextMuted,
            )
        }
        StatusChip(item.status)
    }
}
