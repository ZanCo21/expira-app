package com.expira.app.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

interface OcrEngine {
    /** Recognizes Latin text in an upright bitmap. Throws when the engine fails. */
    suspend fun recognize(bitmap: Bitmap): OcrResult
}

/** Google ML Kit Text Recognition v2, bundled Latin model (the only OCR engine). */
class MlKitOcrEngine : OcrEngine {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun recognize(bitmap: Bitmap): OcrResult {
        val image = InputImage.fromBitmap(bitmap, 0)
        val text = suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { continuation.resume(it) }
                .addOnFailureListener { continuation.resumeWithException(it) }
        }
        return text.toOcrResult()
    }

    private fun Text.toOcrResult(): OcrResult = OcrResult(
        text = text,
        lines = textBlocks.flatMap { block ->
            block.lines.map { line ->
                OcrLine(
                    text = line.text,
                    box = line.boundingBox?.let { OcrBox(it.left, it.top, it.right, it.bottom) },
                    confidence = line.confidence,
                )
            }
        },
    )
}
