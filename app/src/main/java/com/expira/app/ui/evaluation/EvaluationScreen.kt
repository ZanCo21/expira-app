package com.expira.app.ui.evaluation

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expira.app.R
import com.expira.app.evaluation.EvaluationLog
import com.expira.app.evaluation.ExportLogsUseCase
import com.expira.app.imaging.PreprocessConfig
import com.expira.app.ui.components.Chip
import com.expira.app.ui.components.OutlinedCard
import com.expira.app.ui.components.PrimaryButton
import com.expira.app.ui.components.ScreenHeader
import com.expira.app.ui.components.SecondaryButton
import com.expira.app.ui.theme.ExpiraColors

/** Figma "Mode Evaluasi" (5:147, from the PNG export). */
@Composable
fun EvaluationScreen(
    modifier: Modifier = Modifier,
    viewModel: EvaluationViewModel = viewModel(factory = EvaluationViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // OpenDocument (Files app) keeps the original file name, unlike the photo picker,
    // so log rows can be traced back to the dataset image.
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.onImagePicked(uri, context.displayNameOf(uri))
    }
    val createCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(ExportLogsUseCase.MIME_TYPE)) { uri ->
        if (uri != null) viewModel.export(uri)
    }
    MessageToast(state.message, viewModel::onMessageShown)

    Column(modifier = modifier.fillMaxSize().statusBarsPadding()) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
        ) {
            ScreenHeader(
                title = stringResource(R.string.evaluation_title),
                subtitle = stringResource(R.string.evaluation_subtitle),
            )
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ConfigCard(selected = state.selected, onSelect = viewModel::select)
                state.selectedLog?.let { ResultCard(it) }
            }
        }
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PrimaryButton(
                text = state.runningConfig?.let { stringResource(R.string.evaluation_running, it.name) }
                    ?: stringResource(R.string.evaluation_run),
                onClick = { pickImage.launch(arrayOf(IMAGE_MIME_TYPE)) },
                enabled = !state.isRunning,
            )
            SecondaryButton(
                text = stringResource(R.string.evaluation_export),
                onClick = { createCsv.launch(ExportLogsUseCase.DEFAULT_FILE_NAME) },
                enabled = !state.isExporting && !state.isRunning,
            )
        }
    }

    state.groundTruthInput?.let { input ->
        GroundTruthDialog(
            input = input,
            onChange = viewModel::onGroundTruthChange,
            onConfirm = viewModel::startRun,
            onDismiss = viewModel::dismissGroundTruth,
        )
    }
}

@Composable
private fun ConfigCard(selected: PreprocessConfig, onSelect: (PreprocessConfig) -> Unit) {
    OutlinedCard(spacing = 12.dp) {
        Text(
            text = stringResource(R.string.evaluation_config_label),
            style = MaterialTheme.typography.labelMedium,
            color = ExpiraColors.TextMuted,
        )
        Row(modifier = Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PreprocessConfig.entries.forEach { config ->
                ConfigPill(config, isSelected = config == selected, onClick = { onSelect(config) }, Modifier.weight(1f))
            }
        }
        Text(
            text = stringResource(selected.descriptionRes()),
            style = MaterialTheme.typography.bodySmall,
            color = ExpiraColors.Text,
        )
    }
}

@Composable
private fun ConfigPill(config: PreprocessConfig, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(24.dp)
    val base = modifier
        .height(48.dp)
        .clip(shape)
        .selectable(selected = isSelected, onClick = onClick, role = Role.RadioButton)
    val styled = if (isSelected) {
        base.background(ExpiraColors.Primary, shape)
    } else {
        base.background(ExpiraColors.Surface, shape).border(1.dp, ExpiraColors.Outline, shape)
    }
    Row(modifier = styled, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = config.name,
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) ExpiraColors.OnPrimary else ExpiraColors.Text,
        )
    }
}

@Composable
private fun ResultCard(log: EvaluationLog) {
    OutlinedCard(spacing = 10.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.evaluation_last_result),
                style = MaterialTheme.typography.labelLarge,
                color = ExpiraColors.Text,
            )
            if (log.comparison.exactMatch) {
                Chip(stringResource(R.string.evaluation_match), ExpiraColors.Success, ExpiraColors.SuccessBg)
            } else {
                Chip(stringResource(R.string.evaluation_mismatch), ExpiraColors.Danger, ExpiraColors.DangerBg)
            }
        }
        val firstLine = log.rawOcrText.lineSequence().firstOrNull { it.isNotBlank() }?.trim()
        val dash = stringResource(R.string.evaluation_none)
        ResultRow(stringResource(R.string.evaluation_raw_text), log.detectedLine ?: firstLine ?: dash)
        ResultRow(stringResource(R.string.evaluation_extracted), log.extracted?.format() ?: dash)
        ResultRow(stringResource(R.string.evaluation_ground_truth), log.groundTruth.format())
        ResultRow(stringResource(R.string.stage_preprocess_short), stringResource(R.string.processing_duration_ms, log.preprocessMs))
        ResultRow(stringResource(R.string.stage_ocr_short), stringResource(R.string.processing_duration_ms, log.ocrMs))
        ResultRow(stringResource(R.string.evaluation_extract_validate), stringResource(R.string.processing_duration_ms, log.extractValidateMs))
        ResultRow(stringResource(R.string.evaluation_total), stringResource(R.string.processing_duration_ms, log.totalMs), emphasize = true)
    }
}

@Composable
private fun ResultRow(label: String, value: String, emphasize: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = ExpiraColors.TextMuted,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (emphasize) FontWeight.Bold else FontWeight.SemiBold),
            color = ExpiraColors.Text,
        )
    }
}

@Composable
private fun GroundTruthDialog(
    input: GroundTruthInput,
    onChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.evaluation_gt_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.evaluation_gt_body, input.imageName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ExpiraColors.TextMuted,
                )
                OutlinedTextField(
                    value = input.text,
                    onValueChange = onChange,
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.evaluation_gt_placeholder)) },
                    isError = input.showError,
                    supportingText = if (input.showError) {
                        { Text(stringResource(R.string.evaluation_gt_error)) }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.evaluation_gt_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
        containerColor = ExpiraColors.Surface,
    )
}

@Composable
private fun MessageToast(message: EvaluationMessage?, onShown: () -> Unit) {
    val context = LocalContext.current
    val text = when (message) {
        EvaluationMessage.RUN_FAILED -> stringResource(R.string.processing_error_image)
        EvaluationMessage.EXPORT_DONE -> stringResource(R.string.evaluation_export_done)
        EvaluationMessage.EXPORT_FAILED -> stringResource(R.string.evaluation_export_failed)
        null -> null
    }
    LaunchedEffect(message) {
        if (text != null) {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
            onShown()
        }
    }
}

private const val IMAGE_MIME_TYPE = "image/*"

private fun PreprocessConfig.descriptionRes(): Int = when (this) {
    PreprocessConfig.P0 -> R.string.evaluation_desc_p0
    PreprocessConfig.P1 -> R.string.evaluation_desc_p1
    PreprocessConfig.P2 -> R.string.evaluation_desc_p2
    PreprocessConfig.P3 -> R.string.evaluation_desc_p3
}

private fun Context.displayNameOf(uri: Uri): String =
    runCatching {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull() ?: uri.lastPathSegment.orEmpty()
