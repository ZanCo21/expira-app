package com.expira.app.ui.processing

import android.graphics.Bitmap
import com.expira.app.domain.ScanEvent
import com.expira.app.domain.ScanFailure
import com.expira.app.domain.ScanOutcome
import com.expira.app.domain.ScanStage

sealed interface StageStatus {
    data object Pending : StageStatus
    data object Running : StageStatus
    data class Done(val durationMs: Long) : StageStatus
}

data class ProcessingUiState(
    val preview: Bitmap? = null,
    val stages: Map<ScanStage, StageStatus> = ScanStage.entries.associateWith { StageStatus.Pending },
    /** Pipeline result once finished; shown in a debug-only card until the result screen (Sprint 3). */
    val outcome: ScanOutcome? = null,
    val failure: ScanFailure? = null,
) {
    /** Applies one pipeline event; pure so it can be unit tested. */
    fun reduce(event: ScanEvent): ProcessingUiState = when (event) {
        is ScanEvent.StageStarted -> copy(stages = stages + (event.stage to StageStatus.Running))
        is ScanEvent.StageFinished -> copy(stages = stages + (event.stage to StageStatus.Done(event.durationMs)))
        is ScanEvent.Completed -> copy(outcome = event.outcome)
        is ScanEvent.Failed -> copy(failure = event.reason, stages = stages.resetRunning())
    }

    private fun Map<ScanStage, StageStatus>.resetRunning() =
        mapValues { (_, status) -> if (status == StageStatus.Running) StageStatus.Pending else status }
}
