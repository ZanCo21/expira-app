package com.expira.app.ui.processing

import com.expira.app.domain.ScanEvent
import com.expira.app.domain.ScanFailure
import com.expira.app.domain.ScanOutcome
import com.expira.app.domain.ScanStage
import com.expira.app.domain.VariantResult
import com.expira.app.extraction.ExtractionResult
import com.expira.app.imaging.PreprocessConfig
import com.expira.app.ocr.OcrResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProcessingUiStateTest {

    @Test
    fun `all stages start pending`() {
        val state = ProcessingUiState()
        ScanStage.entries.forEach { assertEquals(StageStatus.Pending, state.stages[it]) }
    }

    @Test
    fun `stage moves from running to done with duration`() {
        val state = ProcessingUiState()
            .reduce(ScanEvent.StageStarted(ScanStage.OCR))
            .also { assertEquals(StageStatus.Running, it.stages[ScanStage.OCR]) }
            .reduce(ScanEvent.StageFinished(ScanStage.OCR, 480))
        assertEquals(StageStatus.Done(480), state.stages[ScanStage.OCR])
        assertEquals(StageStatus.Pending, state.stages[ScanStage.EXTRACT])
    }

    @Test
    fun `completion exposes the outcome`() {
        val variant = VariantResult(
            config = PreprocessConfig.P0,
            appliedSteps = emptySet(),
            preprocessMs = 0,
            ocrMs = 10,
            extractMs = 1,
            ocr = OcrResult("EXP 12.08.27", emptyList()),
            extraction = ExtractionResult.EMPTY,
        )
        val outcome = ScanOutcome(null, listOf(variant), null, preprocessMs = 0, ocrMs = 10, extractMs = 1, totalMs = 12)
        val state = ProcessingUiState().reduce(ScanEvent.Completed(outcome))
        assertEquals("EXP 12.08.27", state.outcome?.rawText)
        assertNull(state.failure)
    }

    @Test
    fun `failure stops the running stage`() {
        val state = ProcessingUiState()
            .reduce(ScanEvent.StageFinished(ScanStage.PREPROCESS, 3))
            .reduce(ScanEvent.StageStarted(ScanStage.OCR))
            .reduce(ScanEvent.Failed(ScanFailure.OCR_FAILED))
        assertEquals(ScanFailure.OCR_FAILED, state.failure)
        assertEquals(StageStatus.Pending, state.stages[ScanStage.OCR])
        assertEquals(StageStatus.Done(3), state.stages[ScanStage.PREPROCESS])
    }
}
