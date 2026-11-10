package com.expira.app.ui.processing

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expira.app.BuildConfig
import com.expira.app.R
import com.expira.app.domain.ScanFailure
import com.expira.app.domain.ScanOutcome
import com.expira.app.domain.ScanStage
import com.expira.app.ui.components.ExpiraTopBar
import com.expira.app.ui.components.StepIndicator
import com.expira.app.ui.components.StepRow
import com.expira.app.ui.theme.ExpiraColors

private val CardShape = RoundedCornerShape(16.dp)

@Composable
fun ProcessingRoute(
    onBack: () -> Unit,
    viewModel: ProcessingViewModel = viewModel(factory = ProcessingViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProcessingScreen(state = state, onBack = onBack)
}

/** Figma "Memproses" (3:85). */
@Composable
fun ProcessingScreen(
    state: ProcessingUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().background(ExpiraColors.Background)) {
        ExpiraTopBar(title = stringResource(R.string.processing_title), onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            PhotoPreview(state)
            StagesCard(state.stages)
            state.failure?.let { FailureText(it) }
            Text(
                text = stringResource(R.string.processing_offline_note),
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                color = ExpiraColors.TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            if (BuildConfig.DEBUG) state.outcome?.let { DebugOutcomeCard(it) }
        }
    }
}

@Composable
private fun PhotoPreview(state: ProcessingUiState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(ExpiraColors.Outline),
        contentAlignment = Alignment.Center,
    ) {
        val preview = state.preview
        if (preview != null) {
            Image(
                bitmap = preview.asImageBitmap(),
                contentDescription = stringResource(R.string.processing_photo),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = stringResource(R.string.processing_photo),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = ExpiraColors.TextMuted,
            )
        }
    }
}

@Composable
private fun StagesCard(stages: Map<ScanStage, StageStatus>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ExpiraColors.Surface, CardShape)
            .border(1.dp, ExpiraColors.Outline, CardShape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScanStage.entries.forEach { stage ->
            val status = stages[stage] ?: StageStatus.Pending
            StepRow(
                label = stringResource(stage.labelRes()),
                trailing = status.trailingText(),
                indicator = status.indicator(),
            )
        }
    }
}

@Composable
private fun StageStatus.trailingText(): String = when (this) {
    is StageStatus.Done -> stringResource(R.string.processing_duration_ms, durationMs)
    StageStatus.Running -> stringResource(R.string.processing_running)
    StageStatus.Pending -> stringResource(R.string.processing_waiting)
}

private fun StageStatus.indicator(): StepIndicator = when (this) {
    is StageStatus.Done -> StepIndicator.Done
    StageStatus.Running -> StepIndicator.Running
    StageStatus.Pending -> StepIndicator.Pending
}

private fun ScanStage.labelRes(): Int = when (this) {
    ScanStage.PREPROCESS -> R.string.stage_preprocess
    ScanStage.OCR -> R.string.stage_ocr
    ScanStage.EXTRACT -> R.string.stage_extract
    ScanStage.VALIDATE -> R.string.stage_validate
}

@Composable
private fun FailureText(failure: ScanFailure) {
    val message = when (failure) {
        ScanFailure.IMAGE_UNREADABLE -> R.string.processing_error_image
        ScanFailure.OCR_FAILED -> R.string.processing_error_ocr
    }
    Text(
        text = stringResource(message),
        style = MaterialTheme.typography.bodyMedium,
        color = ExpiraColors.Danger,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Temporary debug-only view of the pipeline result; replaced by the result screen in Sprint 3. */
@Composable
private fun DebugOutcomeCard(outcome: ScanOutcome) {
    val best = outcome.extraction.best
    val variants = outcome.variants.joinToString { variant ->
        variant.config.name + variant.appliedSteps.joinToString(prefix = "[", postfix = "]")
    }
    val summary = buildString {
        appendLine("Tanggal: ${best?.date?.format() ?: "∅"} (${best?.format ?: "-"}, ${best?.keyword?.kind ?: "-"})")
        appendLine("Varian: ${outcome.chosen?.config ?: "-"} dari $variants")
        appendLine("Kandidat: ${outcome.extraction.candidates.joinToString { it.date.format() }.ifEmpty { "∅" }}")
        appendLine("Blur: ${outcome.blurScore?.let { "%.0f".format(it) } ?: "-"} · Total ${outcome.totalMs} ms")
        append(outcome.rawText.ifBlank { "∅" })
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ExpiraColors.Surface, CardShape)
            .border(1.dp, ExpiraColors.Outline, CardShape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = "DEBUG", style = MaterialTheme.typography.labelMedium, color = ExpiraColors.TextMuted)
        Text(
            text = summary,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = ExpiraColors.Text,
        )
    }
}
