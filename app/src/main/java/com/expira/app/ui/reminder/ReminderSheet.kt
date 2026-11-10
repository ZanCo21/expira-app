package com.expira.app.ui.reminder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expira.app.R
import com.expira.app.domain.ReminderChoice
import com.expira.app.domain.ReminderOption
import com.expira.app.domain.ReminderPlan
import com.expira.app.extraction.CalendarDate
import com.expira.app.ui.components.PrimaryButton
import com.expira.app.ui.components.SecondaryButton
import com.expira.app.ui.components.longDate
import com.expira.app.ui.theme.ExpiraColors

/** Figma "Pengingat" bottom sheet (5:118). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderSheet(
    productName: String,
    expiry: CalendarDate,
    current: ReminderChoice?,
    planFor: (ReminderOption) -> ReminderPlan?,
    onSave: (ReminderChoice) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember { mutableStateOf(current ?: ReminderChoice(enabled = true)) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = ExpiraColors.Surface,
        scrimColor = ExpiraColors.CameraBg.copy(alpha = 0.6f),
        dragHandle = { DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.reminder_title), style = MaterialTheme.typography.titleLarge, color = ExpiraColors.Text)
                Text(
                    text = stringResource(
                        R.string.reminder_subtitle,
                        productName.ifBlank { stringResource(R.string.product_unnamed) },
                        longDate(expiry),
                    ),
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 17.sp),
                    color = ExpiraColors.TextMuted,
                )
            }
            EnableRow(enabled = draft.enabled, onChange = { draft = draft.copy(enabled = it) })
            OptionPicker(
                selected = draft.option,
                enabled = draft.enabled,
                onSelect = { draft = draft.copy(option = it) },
            )
            if (draft.enabled) PlanText(planFor(draft.option))
            Text(
                text = stringResource(R.string.reminder_permission_note),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal, lineHeight = 17.sp),
                color = ExpiraColors.TextMuted,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ExpiraColors.Background, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            )
            PrimaryButton(text = stringResource(R.string.reminder_save), onClick = { onSave(draft) })
            SecondaryButton(text = stringResource(R.string.action_cancel), onClick = onDismiss)
        }
    }
}

@Composable
private fun DragHandle() {
    Box(
        modifier = Modifier
            .padding(top = 20.dp, bottom = 16.dp)
            .size(width = 40.dp, height = 4.dp)
            .background(ExpiraColors.Outline, RoundedCornerShape(2.dp)),
    )
}

@Composable
private fun EnableRow(enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.reminder_enable),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = ExpiraColors.Text,
        )
        Switch(
            checked = enabled,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = ExpiraColors.Primary,
                checkedThumbColor = ExpiraColors.Surface,
                checkedBorderColor = ExpiraColors.Primary,
                uncheckedTrackColor = ExpiraColors.Outline,
                uncheckedThumbColor = ExpiraColors.Surface,
                uncheckedBorderColor = ExpiraColors.Outline,
            ),
        )
    }
}

@Composable
private fun OptionPicker(selected: ReminderOption, enabled: Boolean, onSelect: (ReminderOption) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.alpha(if (enabled) 1f else 0.5f)) {
        Text(stringResource(R.string.reminder_when), style = MaterialTheme.typography.labelMedium, color = ExpiraColors.TextMuted)
        Row(modifier = Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReminderOption.entries.forEach { option ->
                val isSelected = option == selected
                val shape = RoundedCornerShape(20.dp)
                val base = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(shape)
                    .selectable(selected = isSelected, enabled = enabled, onClick = { onSelect(option) }, role = Role.RadioButton)
                val styled = if (isSelected) {
                    base.background(ExpiraColors.Primary, shape)
                } else {
                    base.background(ExpiraColors.Surface, shape).border(1.dp, ExpiraColors.Outline, shape)
                }
                Box(modifier = styled, contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(option.labelRes()),
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
                        color = if (isSelected) ExpiraColors.OnPrimary else ExpiraColors.Text,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanText(plan: ReminderPlan?) {
    val (text, color) = when (plan) {
        is ReminderPlan.OnDate -> stringResource(R.string.reminder_will_show, longDate(plan.date)) to ExpiraColors.TextMuted
        is ReminderPlan.TooLate -> stringResource(R.string.reminder_too_late) to ExpiraColors.Warning
        null -> return
    }
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = color)
}

fun ReminderOption.labelRes(): Int = when (this) {
    ReminderOption.H7 -> R.string.reminder_option_h7
    ReminderOption.H3 -> R.string.reminder_option_h3
    ReminderOption.H1 -> R.string.reminder_option_h1
}
