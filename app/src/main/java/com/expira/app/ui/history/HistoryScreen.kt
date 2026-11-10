package com.expira.app.ui.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.expira.app.R
import com.expira.app.ui.components.ScreenHeader

/** Figma "Riwayat". Filters and the history list arrive with Room in Sprint 3. */
@Composable
fun HistoryScreen(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().statusBarsPadding()) {
        ScreenHeader(title = stringResource(R.string.history_title))
    }
}
