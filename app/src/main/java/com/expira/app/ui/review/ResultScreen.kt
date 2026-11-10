package com.expira.app.ui.review

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expira.app.R
import com.expira.app.ui.components.ConfidenceChip
import com.expira.app.ui.components.OutlinedCard
import com.expira.app.ui.components.PrimaryButton
import com.expira.app.ui.components.SecondaryButton
import com.expira.app.ui.components.StatusChip
import com.expira.app.ui.components.longDate
import com.expira.app.ui.components.remainingDaysText
import com.expira.app.ui.theme.ExpiraColors

/**
 * Figma "Hasil · Keyakinan Tinggi" (3:118). The product name field and reminder row
 * of the design belong to F-13/F-14 and are added in Sprint 5.
 */
@Composable
fun ResultRoute(
    onBack: () -> Unit,
    onCorrect: () -> Unit,
    onSaved: () -> Unit,
    viewModel: ReviewViewModel = viewModel(factory = ReviewViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ReviewEventEffect(state.event, onSaved = onSaved, onManualInput = onCorrect, onMissing = onBack, viewModel::onEventHandled)

    ReviewScaffold(
        title = stringResource(R.string.result_title),
        onBack = onBack,
        buttons = {
            PrimaryButton(
                text = stringResource(R.string.result_save),
                onClick = viewModel::saveDetected,
                enabled = !state.isSaving,
            )
            SecondaryButton(text = stringResource(R.string.result_correct), onClick = onCorrect)
        },
    ) {
        PhotoWithHighlight(state.image, state.highlight, height = 130.dp)
        val date = state.detectedDate ?: return@ReviewScaffold
        OutlinedCard {
            FieldLabel(stringResource(R.string.result_expiry_label))
            Text(text = longDate(date), style = MaterialTheme.typography.headlineLarge, color = ExpiraColors.Text)
            state.expiry?.let { expiry ->
                Text(
                    text = stringResource(
                        R.string.result_read_line,
                        state.sourceLine,
                        remainingDaysText(expiry.daysRemaining).replaceFirstChar { it.lowercase() },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = ExpiraColors.TextMuted,
                )
            }
            ChipRow {
                state.expiry?.let { StatusChip(it.status) }
                ConfidenceChip(state.score)
            }
        }
    }
}

/** Handles one-shot review events, then marks them handled. */
@Composable
internal fun ReviewEventEffect(
    event: ReviewEvent?,
    onSaved: () -> Unit,
    onManualInput: () -> Unit,
    onMissing: () -> Unit,
    onHandled: () -> Unit,
) {
    LaunchedEffect(event) {
        when (event) {
            ReviewEvent.Saved -> onSaved()
            ReviewEvent.OpenManualInput -> onManualInput()
            ReviewEvent.SessionMissing -> onMissing()
            null -> return@LaunchedEffect
        }
        onHandled()
    }
}

/** Medium 13 muted label used above values. */
@Composable
internal fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = ExpiraColors.TextMuted,
    )
}
