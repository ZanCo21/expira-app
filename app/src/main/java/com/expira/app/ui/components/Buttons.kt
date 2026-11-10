package com.expira.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.expira.app.ui.theme.ExpiraColors

private val ButtonShape = RoundedCornerShape(24.dp)
private val ButtonHeight = 48.dp
private val ButtonPadding = PaddingValues(horizontal = 20.dp)

/** Figma component "Tombol/Utama". */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = ButtonShape,
        contentPadding = ButtonPadding,
        colors = ButtonDefaults.buttonColors(
            containerColor = ExpiraColors.Primary,
            contentColor = ExpiraColors.OnPrimary,
        ),
        modifier = modifier.fillMaxWidth().height(ButtonHeight),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Figma component "Tombol/Sekunder". */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = ButtonShape,
        contentPadding = ButtonPadding,
        border = BorderStroke(1.dp, ExpiraColors.Outline),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = ExpiraColors.Surface,
            contentColor = ExpiraColors.Primary,
        ),
        modifier = modifier.fillMaxWidth().height(ButtonHeight),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}
