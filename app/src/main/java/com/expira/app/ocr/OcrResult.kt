package com.expira.app.ocr

/**
 * Engine-independent OCR output. Plain Kotlin types only, so the extraction
 * layer can consume it in JVM unit tests.
 */
data class OcrResult(
    val text: String,
    val lines: List<OcrLine>,
) {
    val isEmpty: Boolean get() = text.isBlank()

    companion object {
        val EMPTY = OcrResult(text = "", lines = emptyList())
    }
}

data class OcrLine(
    val text: String,
    val box: OcrBox?,
    /** Engine confidence in 0..1, or null when the engine does not report it. */
    val confidence: Float?,
)

data class OcrBox(val left: Int, val top: Int, val right: Int, val bottom: Int)
