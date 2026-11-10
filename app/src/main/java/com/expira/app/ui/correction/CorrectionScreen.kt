package com.expira.app.ui.correction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expira.app.R
import com.expira.app.extraction.ValidationResult
import com.expira.app.ui.components.OutlinedCard
import com.expira.app.ui.components.PrimaryButton
import com.expira.app.ui.components.SecondaryButton
import com.expira.app.ui.components.StatusChip
import com.expira.app.ui.components.longDate
import com.expira.app.ui.components.remainingDaysText
import com.expira.app.ui.review.ChipRow
import com.expira.app.ui.review.FieldLabel
import com.expira.app.ui.review.ReviewScaffold
import com.expira.app.ui.theme.ExpiraColors

/** Figma "Koreksi Manual" (4:91). */
@Composable
fun CorrectionRoute(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: CorrectionViewModel = viewModel(factory = CorrectionViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved, state.sessionMissing) {
        if (state.saved) onSaved() else if (state.sessionMissing) onBack()
    }

    ReviewScaffold(
        title = stringResource(R.string.correction_title),
        onBack = onBack,
        contentSpacing = 16.dp,
        buttons = {
            PrimaryButton(text = stringResource(R.string.correction_save), onClick = viewModel::save, enabled = state.canSave)
            SecondaryButton(text = stringResource(R.string.action_cancel), onClick = onBack)
        },
    ) {
        Text(
            text = stringResource(R.string.correction_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = ExpiraColors.TextMuted,
        )
        OcrTextBox(state.ocrText)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DateInput(R.string.correction_day, state.day, DateField.DAY, viewModel::onFieldChange, Modifier.weight(1f))
            DateInput(R.string.correction_month, state.month, DateField.MONTH, viewModel::onFieldChange, Modifier.weight(1f))
            DateInput(R.string.correction_year, state.year, DateField.YEAR, viewModel::onFieldChange, Modifier.weight(1f))
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.correction_hint),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = ExpiraColors.TextMuted,
            )
            validationMessage(state.validation)?.let {
                Text(text = it, style = MaterialTheme.typography.labelMedium, color = ExpiraColors.Danger)
            }
        }
        val preview = state.preview
        val expiry = state.expiry
        if (preview != null && expiry != null) {
            OutlinedCard {
                FieldLabel(stringResource(R.string.correction_preview))
                Text(text = longDate(preview), style = MaterialTheme.typography.headlineSmall, color = ExpiraColors.Text)
                ChipRow {
                    StatusChip(expiry.status)
                    Text(
                        text = remainingDaysText(expiry.daysRemaining),
                        style = MaterialTheme.typography.bodySmall,
                        color = ExpiraColors.TextMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun validationMessage(validation: ValidationResult?): String? = when (validation) {
    ValidationResult.InvalidDate -> stringResource(R.string.correction_error_invalid)
    is ValidationResult.OutOfRange -> stringResource(R.string.correction_error_range, validation.minYear, validation.maxYear)
    is ValidationResult.Valid, null -> null
}

@Composable
private fun OcrTextBox(text: String) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ExpiraColors.Surface, shape)
            .border(1.dp, ExpiraColors.Outline, shape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = stringResource(R.string.correction_ocr_label), style = MaterialTheme.typography.labelMedium, color = ExpiraColors.TextMuted)
        Text(
            text = text.ifBlank { stringResource(R.string.correction_ocr_empty) },
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = ExpiraColors.Text,
        )
    }
}

@Composable
private fun DateInput(
    label: Int,
    value: String,
    field: DateField,
    onChange: (DateField, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel(stringResource(label))
        val shape = RoundedCornerShape(12.dp)
        BasicTextField(
            value = value,
            onValueChange = { onChange(field, it) },
            singleLine = true,
            textStyle = MaterialTheme.typography.titleLarge.copy(
                fontSize = 20.sp,
                color = ExpiraColors.Text,
                textAlign = TextAlign.Center,
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = if (field == DateField.YEAR) ImeAction.Done else ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(ExpiraColors.Surface, shape)
                        .border(2.dp, ExpiraColors.Primary, shape),
                    contentAlignment = Alignment.Center,
                ) { inner() }
            },
        )
    }
}
