package com.expira.app.ui.camera

import android.content.Context
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.compose.ui.geometry.Rect
import androidx.core.content.ContextCompat
import com.expira.app.imaging.CapturedFrame
import com.expira.app.imaging.NormalizedRect
import com.expira.app.imaging.PixelRect

/** Takes a JPEG still and hands back its bytes, viewport crop and rotation. */
internal fun LifecycleCameraController.captureFrame(
    context: Context,
    onCaptured: (CapturedFrame) -> Unit,
    onError: () -> Unit,
) {
    takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val frame = image.use { it.toCapturedFrame() }
                if (frame != null) onCaptured(frame) else onError()
            }

            override fun onError(exception: ImageCaptureException) = onError()
        },
    )
}

private fun ImageProxy.toCapturedFrame(): CapturedFrame? {
    val buffer = planes.firstOrNull()?.buffer ?: return null
    val bytes = ByteArray(buffer.remaining()).also { buffer.get(it) }
    if (bytes.isEmpty()) return null
    return CapturedFrame(
        jpeg = bytes,
        cropRect = PixelRect(cropRect.left, cropRect.top, cropRect.right, cropRect.bottom),
        rotationDegrees = imageInfo.rotationDegrees,
    )
}

/** Position of the on-screen guide frame as fractions of the preview area. */
internal fun guideFraction(frame: Rect, preview: Rect): NormalizedRect? {
    if (preview.width <= 0f || preview.height <= 0f) return null
    fun x(v: Float) = ((v - preview.left) / preview.width).coerceIn(0f, 1f)
    fun y(v: Float) = ((v - preview.top) / preview.height).coerceIn(0f, 1f)
    val left = x(frame.left)
    val top = y(frame.top)
    val right = x(frame.right)
    val bottom = y(frame.bottom)
    return if (left < right && top < bottom) NormalizedRect(left, top, right, bottom) else null
}
