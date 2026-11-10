package com.expira.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expira.app.ui.theme.ExpiraColors

/** White card with 1dp outline (Figma "Kartu Hasil", "Tahapan", "Saran", ...). */
@Composable
fun OutlinedCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    spacing: Dp = 6.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ExpiraColors.Surface, shape)
            .border(1.dp, ExpiraColors.Outline, shape)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content,
    )
}

/** Figma "Banner" / "Peringatan Buram": warning-bg, Medium 13/18. */
@Composable
fun WarningBanner(
    message: String,
    modifier: Modifier = Modifier,
    verticalPadding: Dp = 12.dp,
) {
    Text(
        text = message,
        style = MaterialTheme.typography.labelMedium.copy(lineHeight = 18.sp),
        color = ExpiraColors.Warning,
        modifier = modifier
            .fillMaxWidth()
            .background(ExpiraColors.WarningBg, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = verticalPadding),
    )
}
