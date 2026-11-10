package com.expira.app.ui.camera

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expira.app.R
import com.expira.app.imaging.CapturedFrame
import com.expira.app.imaging.NormalizedRect
import com.expira.app.ui.components.PrimaryButton
import com.expira.app.ui.components.SecondaryButton
import com.expira.app.ui.theme.ExpiraColors

/** Figma "Kamera" (3:63). */
@Composable
fun CameraRoute(
    onClose: () -> Unit,
    onImageReady: (Uri) -> Unit,
    viewModel: CameraViewModel = viewModel(factory = CameraViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(context.hasCameraPermission()) }
    var askedPermission by rememberSaveable { mutableStateOf(false) }
    val requestPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
        askedPermission = true
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onImageReady(uri)
    }
    val openGallery = {
        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    LaunchedEffect(Unit) {
        if (!hasPermission && !askedPermission) requestPermission.launch(Manifest.permission.CAMERA)
    }
    LaunchedEffect(state.capturedImage) {
        state.capturedImage?.let {
            viewModel.onNavigated()
            onImageReady(it)
        }
    }
    LightStatusBarIcons()

    Column(modifier = Modifier.fillMaxSize().background(ExpiraColors.CameraBg)) {
        if (hasPermission) {
            CameraContent(
                state = state,
                onClose = onClose,
                onToggleFlash = viewModel::toggleFlash,
                onOpenGallery = openGallery,
                onCaptureStarted = viewModel::onCaptureStarted,
                onCaptured = viewModel::onFrameCaptured,
                onCaptureFailed = viewModel::onCaptureFailed,
            )
        } else {
            PermissionDeniedContent(
                onClose = onClose,
                onRequestPermission = { requestPermission.launch(Manifest.permission.CAMERA) },
                onOpenGallery = openGallery,
            )
        }
    }
}

@Composable
private fun CameraContent(
    state: CameraUiState,
    onClose: () -> Unit,
    onToggleFlash: () -> Unit,
    onOpenGallery: () -> Unit,
    onCaptureStarted: () -> Unit,
    onCaptured: (CapturedFrame, NormalizedRect) -> Unit,
    onCaptureFailed: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        }
    }
    DisposableEffect(lifecycleOwner) {
        controller.bindToLifecycle(lifecycleOwner)
        onDispose { controller.unbind() }
    }
    LaunchedEffect(state.isFlashOn) {
        controller.imageCaptureFlashMode =
            if (state.isFlashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
    }
    var previewBounds by remember { mutableStateOf(Rect.Zero) }
    var frameBounds by remember { mutableStateOf(Rect.Zero) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    this.controller = controller
                }
            },
            modifier = Modifier.fillMaxSize().onGloballyPositioned { previewBounds = it.boundsInRoot() },
        )
        Column(modifier = Modifier.fillMaxSize()) {
            CameraTopRow(onClose = onClose) { FlashButton(state.isFlashOn, onToggleFlash) }
            AimArea(
                captureFailed = state.captureFailed,
                onFramePositioned = { frameBounds = it },
                modifier = Modifier.weight(1f),
            )
            CameraControls(
                enabled = !state.isCapturing,
                onOpenGallery = onOpenGallery,
                onShutter = {
                    val guide = guideFraction(frameBounds, previewBounds) ?: return@CameraControls
                    onCaptureStarted()
                    controller.captureFrame(
                        context = context,
                        onCaptured = { onCaptured(it, guide) },
                        onError = onCaptureFailed,
                    )
                },
            )
        }
    }
}

@Composable
private fun CameraTopRow(onClose: () -> Unit, flash: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(
                painter = painterResource(R.drawable.ic_x),
                contentDescription = stringResource(R.string.camera_close),
                tint = ExpiraColors.Surface,
                modifier = Modifier.size(24.dp),
            )
        }
        flash?.invoke()
    }
}

@Composable
private fun FlashButton(isFlashOn: Boolean, onToggleFlash: () -> Unit) {
    IconButton(onClick = onToggleFlash) {
        Icon(
            painter = painterResource(R.drawable.ic_zap),
            contentDescription = stringResource(
                if (isFlashOn) R.string.camera_flash_on else R.string.camera_flash_off,
            ),
            // Figma only shows the off state; the on state reuses the warning-bg token.
            tint = if (isFlashOn) ExpiraColors.WarningBg else ExpiraColors.Surface,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun AimArea(
    captureFailed: Boolean,
    onFramePositioned: (Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(width = 300.dp, height = 150.dp)
                .border(3.dp, ExpiraColors.Surface, RoundedCornerShape(14.dp))
                .onGloballyPositioned { onFramePositioned(it.boundsInRoot()) },
        )
        Text(
            text = stringResource(R.string.camera_hint),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium, lineHeight = 17.sp),
            color = ExpiraColors.Surface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        if (captureFailed) CameraBanner(stringResource(R.string.camera_capture_failed))
    }
}

/** Figma "Peringatan Buram" banner style; the blur warning itself arrives in Sprint 2. */
@Composable
private fun CameraBanner(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.labelMedium.copy(lineHeight = 18.sp),
        color = ExpiraColors.Warning,
        modifier = Modifier
            .fillMaxWidth()
            .background(ExpiraColors.WarningBg, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    )
}

@Composable
private fun CameraControls(enabled: Boolean, onOpenGallery: () -> Unit, onShutter: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(120.dp)
            .padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onOpenGallery,
            modifier = Modifier.size(48.dp).border(1.dp, ExpiraColors.Surface, CircleShape),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_image),
                contentDescription = stringResource(R.string.camera_gallery),
                tint = ExpiraColors.Surface,
                modifier = Modifier.size(22.dp),
            )
        }
        IconButton(
            onClick = onShutter,
            enabled = enabled,
            modifier = Modifier.size(72.dp).border(4.dp, ExpiraColors.Surface, CircleShape),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(ExpiraColors.Surface.copy(alpha = if (enabled) 1f else 0.5f), CircleShape),
            )
        }
        Spacer(modifier = Modifier.size(48.dp))
    }
}

@Composable
private fun PermissionDeniedContent(
    onClose: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenGallery: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        CameraTopRow(onClose = onClose)
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.camera_permission_message),
                style = MaterialTheme.typography.bodyMedium,
                color = ExpiraColors.Surface,
                textAlign = TextAlign.Center,
            )
            PrimaryButton(text = stringResource(R.string.camera_permission_grant), onClick = onRequestPermission)
            SecondaryButton(text = stringResource(R.string.home_pick_gallery), onClick = onOpenGallery)
        }
    }
}

/** The camera screen is dark, so status bar icons switch to light while it is shown. */
@Composable
private fun LightStatusBarIcons() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = view.context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previous = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = false
        onDispose { previous?.let { controller.isAppearanceLightStatusBars = it } }
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun Context.hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
