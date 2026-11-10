package com.expira.app.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.expira.app.R
import com.expira.app.ui.theme.ExpiraColors

enum class BottomTab(@DrawableRes val icon: Int, @StringRes val label: Int) {
    Scan(R.drawable.ic_scan, R.string.tab_scan),
    History(R.drawable.ic_history, R.string.tab_history),
    Evaluation(R.drawable.ic_flask, R.string.tab_evaluation),
}

/** Figma component "Navigasi Bawah": 64dp, top outline, three equal tabs. */
@Composable
fun ExpiraBottomNav(
    selected: BottomTab,
    onSelect: (BottomTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().background(ExpiraColors.Surface).navigationBarsPadding()) {
        HorizontalDivider(thickness = 1.dp, color = ExpiraColors.Outline)
        Row(modifier = Modifier.fillMaxWidth().height(63.dp).selectableGroup()) {
            BottomTab.entries.forEach { tab ->
                BottomNavItem(
                    tab = tab,
                    isSelected = tab == selected,
                    onClick = { onSelect(tab) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    tab: BottomTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = if (isSelected) ExpiraColors.Primary else ExpiraColors.TextMuted
    val weight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                painter = painterResource(tab.icon),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = stringResource(tab.label),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = weight),
                color = color,
            )
        }
    }
}
