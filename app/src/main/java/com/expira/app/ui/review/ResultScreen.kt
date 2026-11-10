package com.expira.app.ui.review

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expira.app.R
import com.expira.app.domain.ReminderChoice
import com.expira.app.domain.ReminderPlan
import com.expira.app.ui.components.ConfidenceChip
import com.expira.app.ui.components.OutlinedCard
import com.expira.app.ui.components.PrimaryButton
import com.expira.app.ui.components.SecondaryButton
import com.expira.app.ui.components.StatusChip
import com.expira.app.ui.components.longDate
import com.expira.app.ui.components.remainingDaysText
import com.expira.app.ui.reminder.ReminderSheet
import com.expira.app.ui.reminder.labelRes
import com.expira.app.ui.theme.ExpiraColors

/** Figma "Hasil · Keyakinan Tinggi" (3:118), including product name (F-14) and reminder (F-13). */
@Composable
fun ResultRoute(
    onBack: () -> Unit,
    onCorrect: () -> Unit,
    onSaved: () -> Unit,
    viewModel: ReviewViewModel = viewModel(factory = ReviewViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ReviewEventEffect(state.event, onSaved = onSaved, onManualInput = onCorrect, onMissing = onBack, viewModel::onEventHandled)
    var showReminderSheet by rememberSaveable { mutableStateOf(false) }
    val saveReminder = rememberReminderSaver(onResult = viewModel::setReminder)

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
        ProductNameField(value = state.productName, onChange = viewModel::onProductNameChange)
        ReminderRow(
            choice = state.reminder,
            plan = state.reminder?.let { viewModel.planFor(it.option) },
            onClick = { showReminderSheet = true },
        )
        if (showReminderSheet) {
            ReminderSheet(
                productName = state.productName,
                expiry = date,
                current = state.reminder,
                planFor = viewModel::planFor,
                onSave = { choice ->
                    showReminderSheet = false
                    saveReminder(choice)
                },
                onDismiss = { showReminderSheet = false },
            )
        }
    }
}

/**
 * Saves a reminder choice, asking for the notification permission first on Android 13+.
 * When it is denied the reminder is not kept and the user is told why (FR-24).
 */
@Composable
private fun rememberReminderSaver(onResult: (ReminderChoice?) -> Unit): (ReminderChoice) -> Unit {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<ReminderChoice?>(null) }
    val deniedMessage = stringResource(R.string.reminder_permission_denied)
    val requestPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            onResult(pending)
        } else {
            onResult(null)
            Toast.makeText(context, deniedMessage, Toast.LENGTH_LONG).show()
        }
        pending = null
    }
    return { choice ->
        val needsPermission = choice.enabled &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            pending = choice
            requestPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onResult(choice)
        }
    }
}

/** Figma "Nama Produk": label + 48dp input with placeholder. */
@Composable
private fun ProductNameField(value: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel(stringResource(R.string.product_name_label))
        val shape = RoundedCornerShape(12.dp)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = ExpiraColors.Text),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(ExpiraColors.Surface, shape)
                        .border(1.dp, ExpiraColors.Outline, shape)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = stringResource(R.string.product_name_placeholder),
                            style = MaterialTheme.typography.bodyLarge,
                            color = ExpiraColors.TextMuted,
                        )
                    }
                    inner()
                }
            },
        )
    }
}

/** Figma "Baris Pengingat": bell, label, chevron; shows the chosen reminder once set. */
@Composable
private fun ReminderRow(choice: ReminderChoice?, plan: ReminderPlan?, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(ExpiraColors.Surface, shape)
            .border(1.dp, ExpiraColors.Outline, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_bell),
            contentDescription = null,
            tint = ExpiraColors.Primary,
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.reminder_row),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium, lineHeight = 17.sp),
                color = ExpiraColors.Text,
            )
            if (choice != null) {
                val detail = when (plan) {
                    is ReminderPlan.OnDate -> stringResource(
                        R.string.reminder_row_active,
                        stringResource(choice.option.labelRes()),
                        longDate(plan.date),
                    )
                    is ReminderPlan.TooLate, null -> stringResource(R.string.reminder_too_late)
                }
                Text(text = detail, style = MaterialTheme.typography.bodySmall, color = ExpiraColors.TextMuted)
            }
        }
        Icon(
            painter = painterResource(R.drawable.ic_chevron),
            contentDescription = null,
            tint = ExpiraColors.TextMuted,
            modifier = Modifier.size(20.dp),
        )
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
