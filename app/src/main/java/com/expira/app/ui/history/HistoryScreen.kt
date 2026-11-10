package com.expira.app.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expira.app.R
import com.expira.app.ui.components.ScanRow
import com.expira.app.ui.components.ScreenHeader
import com.expira.app.ui.theme.ExpiraColors

/** Figma "Riwayat" (5:43, from the PNG export). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(modifier = modifier.fillMaxSize().statusBarsPadding()) {
        ScreenHeader(title = stringResource(R.string.history_title))
        FilterPills(selected = state.filter, onSelect = viewModel::selectFilter)
        when {
            state.isLoading -> Unit
            state.items.isEmpty() -> EmptyText(
                stringResource(if (state.hasAnyScan) R.string.history_filter_empty else R.string.history_empty),
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(state.items, key = { it.id }) { item ->
                    ScanRow(
                        item = item,
                        modifier = Modifier.combinedClickable(
                            onClick = {},
                            onLongClick = { viewModel.requestDelete(item.id) },
                            onLongClickLabel = stringResource(R.string.history_delete_confirm),
                        ),
                    )
                }
            }
        }
    }
    if (state.pendingDeleteId != null) {
        DeleteDialog(onConfirm = viewModel::confirmDelete, onDismiss = viewModel::cancelDelete)
    }
}

/** Not in Figma; agreed interaction: long press, then confirm (AC-15). */
@Composable
private fun DeleteDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.history_delete_title)) },
        text = { Text(stringResource(R.string.history_delete_body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.history_delete_confirm), color = ExpiraColors.Danger)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
        containerColor = ExpiraColors.Surface,
    )
}

@Composable
private fun FilterPills(selected: HistoryFilter, onSelect: (HistoryFilter) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        HistoryFilter.entries.forEach { filter ->
            FilterPill(
                label = stringResource(filter.labelRes()),
                isSelected = filter == selected,
                onClick = { onSelect(filter) },
            )
        }
    }
}

@Composable
private fun FilterPill(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val base = Modifier
        .height(32.dp)
        .clip(shape)
        .selectable(selected = isSelected, onClick = onClick, role = Role.Tab)
    val styled = if (isSelected) {
        base.background(ExpiraColors.Primary, shape)
    } else {
        base.background(ExpiraColors.Surface, shape).border(1.dp, ExpiraColors.Outline, shape)
    }
    Row(modifier = styled.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
            color = if (isSelected) ExpiraColors.OnPrimary else ExpiraColors.Text,
        )
    }
}

@Composable
private fun EmptyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = ExpiraColors.TextMuted,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 40.dp),
    )
}

private fun HistoryFilter.labelRes(): Int = when (this) {
    HistoryFilter.ALL -> R.string.history_filter_all
    HistoryFilter.SAFE -> R.string.status_safe
    HistoryFilter.NEAR -> R.string.status_near
    HistoryFilter.EXPIRED -> R.string.history_filter_expired
}
