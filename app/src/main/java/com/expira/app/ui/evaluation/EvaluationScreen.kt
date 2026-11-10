package com.expira.app.ui.evaluation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.expira.app.R
import com.expira.app.ui.components.ScreenHeader

/** Figma "Mode Evaluasi". Configuration picker and test runner arrive in Sprint 4. */
@Composable
fun EvaluationScreen(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().statusBarsPadding()) {
        ScreenHeader(
            title = stringResource(R.string.evaluation_title),
            subtitle = stringResource(R.string.evaluation_subtitle),
        )
    }
}
