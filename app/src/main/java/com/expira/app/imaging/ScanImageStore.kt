package com.expira.app.imaging

import android.graphics.Bitmap
import android.net.Uri
import java.io.File

/**
 * Stores captured scan images in the app's private cache (local only).
 * Only the most recent files are kept; history persistence is added in Sprint 3.
 */
class ScanImageStore(cacheDir: File) {

    private val dir = File(cacheDir, DIR_NAME)

    fun save(bitmap: Bitmap): Uri {
        dir.mkdirs()
        val file = File(dir, "scan_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        pruneOldFiles()
        return Uri.fromFile(file)
    }

    private fun pruneOldFiles() {
        dir.listFiles()
            ?.sortedByDescending { it.lastModified() }
            ?.drop(MAX_FILES)
            ?.forEach { it.delete() }
    }

    private companion object {
        const val DIR_NAME = "scans"
        const val JPEG_QUALITY = 95
        const val MAX_FILES = 5
    }
}
