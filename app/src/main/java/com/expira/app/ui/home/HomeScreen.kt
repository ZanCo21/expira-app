package com.expira.app.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expira.app.R
import com.expira.app.ui.components.PrimaryButton
import com.expira.app.ui.components.ScanRow
import com.expira.app.ui.components.ScanRowUi
import com.expira.app.ui.components.ScreenHeader
import com.expira.app.ui.components.SecondaryButton
import com.expira.app.ui.theme.ExpiraColors
import com.expira.app.ui.theme.ExpiraTheme

@Composable
fun HomeRoute(
    onTakePhoto: () -> Unit,
    onImagePicked: (Uri) -> Unit,
    onSeeAll: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val recent by viewModel.recent.collectAsStateWithLifecycle()
    HomeScreen(recent = recent, onTakePhoto = onTakePhoto, onImagePicked = onImagePicked, onSeeAll = onSeeAll)
}

/** Figma "Beranda" (3:2). "Pemindaian terakhir" is hidden while the history is empty. */
@Composable
fun HomeScreen(
    recent: List<ScanRowUi>,
    onTakePhoto: () -> Unit,
    onImagePicked: (Uri) -> Unit,
    onSeeAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onImagePicked(uri)
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenHeader(
            title = stringResource(R.string.home_title),
            subtitle = stringResource(R.string.home_subtitle),
        )
        ScanCard(
            onTakePhoto = onTakePhoto,
            onPickGallery = {
                pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        if (recent.isNotEmpty()) {
            RecentScans(
                items = recent,
                onSeeAll = onSeeAll,
                modifier = Modifier.padding(20.dp),
            )
        }
    }
}

@Composable
private fun RecentScans(items: List<ScanRowUi>, onSeeAll: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.home_recent),
                style = MaterialTheme.typography.titleMedium,
                color = ExpiraColors.Text,
            )
            Text(
                text = stringResource(R.string.home_see_all),
                style = MaterialTheme.typography.labelMedium,
                color = ExpiraColors.Primary,
                modifier = Modifier.clickable(role = Role.Button, onClick = onSeeAll),
            )
        }
        items.forEach { ScanRow(it) }
    }
}

@Composable
private fun ScanCard(
    onTakePhoto: () -> Unit,
    onPickGallery: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ExpiraColors.PrimaryContainer, RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(48.dp).background(ExpiraColors.Primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_camera),
                contentDescription = null,
                tint = ExpiraColors.OnPrimary,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = stringResource(R.string.home_scan_title),
            style = MaterialTheme.typography.titleLarge,
            color = ExpiraColors.Text,
        )
        Text(
            text = stringResource(R.string.home_scan_body),
            style = MaterialTheme.typography.bodyMedium,
            color = ExpiraColors.TextMuted,
        )
        PrimaryButton(text = stringResource(R.string.home_take_photo), onClick = onTakePhoto)
        SecondaryButton(text = stringResource(R.string.home_pick_gallery), onClick = onPickGallery)
    }
}

@Preview(widthDp = 360, heightDp = 736, showBackground = true, backgroundColor = 0xFFF3F6FB)
@Composable
private fun HomeScreenPreview() {
    ExpiraTheme { HomeScreen(recent = emptyList(), onTakePhoto = {}, onImagePicked = {}, onSeeAll = {}) }
}
