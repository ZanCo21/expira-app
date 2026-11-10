package com.expira.app.ui.review

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.expira.app.R
import com.expira.app.imaging.NormalizedRect
import com.expira.app.ui.components.ExpiraTopBar
import com.expira.app.ui.theme.ExpiraColors
import kotlin.math.min

/**
 * Layout shared by the review screens: top bar, scrollable content and the two
 * buttons pinned to the bottom (Figma "Pengisi" filler pattern).
 */
@Composable
internal fun ReviewScaffold(
    title: String,
    onBack: () -> Unit,
    buttons: @Composable ColumnScope.() -> Unit,
    contentSpacing: Dp = 14.dp,
    contentAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    // imePadding keeps the pinned buttons above the keyboard (correction screen).
    Column(modifier = Modifier.fillMaxSize().background(ExpiraColors.Background).imePadding()) {
        ExpiraTopBar(title = title, onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(contentSpacing),
            horizontalAlignment = contentAlignment,
            content = content,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = buttons,
        )
    }
}

/** Photo preview with the detected date line outlined in green (Figma "Area Tanggal Terdeteksi"). */
@Composable
internal fun PhotoWithHighlight(image: Bitmap?, highlight: NormalizedRect?, height: Dp) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(14.dp))
            .background(ExpiraColors.Outline),
    ) {
        if (image == null || image.width == 0 || image.height == 0) return@BoxWithConstraints
        Image(
            bitmap = image.asImageBitmap(),
            contentDescription = stringResource(R.string.processing_photo),
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
        if (highlight == null) return@BoxWithConstraints
        // Position of the fitted image inside the box.
        val scale = min(maxWidth.value / image.width, maxHeight.value / image.height)
        val shownWidth = image.width * scale
        val shownHeight = image.height * scale
        val offsetX = (maxWidth.value - shownWidth) / 2
        val offsetY = (maxHeight.value - shownHeight) / 2
        // Padding so the outline frames the text instead of covering it, kept inside the image.
        val left = (offsetX + highlight.left * shownWidth - HIGHLIGHT_PADDING).coerceAtLeast(offsetX)
        val top = (offsetY + highlight.top * shownHeight - HIGHLIGHT_PADDING).coerceAtLeast(offsetY)
        val right = (offsetX + highlight.right * shownWidth + HIGHLIGHT_PADDING).coerceAtMost(offsetX + shownWidth)
        val bottom = (offsetY + highlight.bottom * shownHeight + HIGHLIGHT_PADDING).coerceAtMost(offsetY + shownHeight)
        Box(
            modifier = Modifier
                .offset(x = left.dp, y = top.dp)
                .size(width = (right - left).dp, height = (bottom - top).dp)
                .border(3.dp, ExpiraColors.Success, RoundedCornerShape(8.dp)),
        )
    }
}

private const val HIGHLIGHT_PADDING = 6f

@Composable
internal fun ChipRow(content: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        content()
    }
}
