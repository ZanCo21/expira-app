package com.expira.app.ui.review

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expira.app.R
import com.expira.app.extraction.CharCorrection
import com.expira.app.ui.components.Chip
import com.expira.app.ui.components.ConfidenceChip
import com.expira.app.ui.components.OutlinedCard
import com.expira.app.ui.components.PrimaryButton
import com.expira.app.ui.components.SecondaryButton
import com.expira.app.ui.components.WarningBanner
import com.expira.app.ui.components.longDate
import com.expira.app.ui.theme.ExpiraColors

/** Figma "Hasil · Keyakinan Sedang" (4:20). */
@Composable
fun ConfirmRoute(
    onBack: () -> Unit,
    onManualInput: (prefill: Boolean) -> Unit,
    onSaved: () -> Unit,
    viewModel: ReviewViewModel = viewModel(factory = ReviewViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ReviewEventEffect(
        state.event,
        onSaved = onSaved,
        onManualInput = { onManualInput(false) },
        onMissing = onBack,
        onHandled = viewModel::onEventHandled,
    )

    ReviewScaffold(
        title = stringResource(R.string.confirm_title),
        onBack = onBack,
        buttons = {
            PrimaryButton(
                text = stringResource(R.string.confirm_accept),
                onClick = viewModel::confirmSelection,
                enabled = !state.isSaving,
            )
            SecondaryButton(text = stringResource(R.string.confirm_manual), onClick = { onManualInput(true) })
        },
    ) {
        WarningBanner(stringResource(R.string.confirm_banner))
        val date = state.detectedDate ?: return@ReviewScaffold
        OutlinedCard {
            FieldLabel(stringResource(R.string.confirm_read_label))
            Text(text = longDate(date), style = MaterialTheme.typography.headlineLarge, color = ExpiraColors.Text)
            Text(
                text = rawLineText(state.sourceLine, state.corrections),
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                color = ExpiraColors.TextMuted,
            )
            ChipRow {
                Chip(
                    text = stringResource(R.string.confirm_needs_check),
                    contentColor = ExpiraColors.Warning,
                    containerColor = ExpiraColors.WarningBg,
                )
                ConfidenceChip(state.score)
            }
        }
        Text(
            text = stringResource(R.string.confirm_other_candidates),
            style = MaterialTheme.typography.labelLarge,
            color = ExpiraColors.Text,
        )
        OutlinedCard(spacing = 14.dp, modifier = Modifier.selectableGroup()) {
            state.options.forEachIndexed { index, option ->
                RadioRow(label = option.format(), selected = state.selected == index, onClick = { viewModel.select(index) })
            }
            RadioRow(
                label = stringResource(manualOptionLabel(state.options.size)),
                selected = state.isManualSelected,
                onClick = { viewModel.select(state.options.size) },
            )
        }
    }
}

/** "Mentah: EXP 1O.O3.27 → huruf O dikoreksi menjadi 0" */
@Composable
private fun rawLineText(sourceLine: String, corrections: List<CharCorrection>): String {
    val fixes = corrections.map { it.from to it.to }.distinct()
    if (fixes.isEmpty()) return stringResource(R.string.confirm_raw, sourceLine)
    val described = fixes.map { (from, to) -> stringResource(R.string.confirm_correction, from.toString(), to.toString()) }
    return stringResource(R.string.confirm_raw_corrected, sourceLine, described.joinToString(", "))
}

/** Figma text is "Bukan keduanya" for two options; adapted for one or three options. */
private fun manualOptionLabel(optionCount: Int): Int = when (optionCount) {
    0, 1 -> R.string.confirm_manual_option_one
    2 -> R.string.confirm_manual_option_two
    else -> R.string.confirm_manual_option_many
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .border(2.dp, if (selected) ExpiraColors.Primary else ExpiraColors.Outline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Box(modifier = Modifier.size(10.dp).background(ExpiraColors.Primary, CircleShape))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = ExpiraColors.Text,
        )
    }
}
