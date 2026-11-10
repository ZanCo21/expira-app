package com.expira.app.ui.reminder

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.expira.app.R
import com.expira.app.ui.theme.ExpiraColors

/**
 * PRD risk mitigation for F-13: a short guide to exclude the app from battery
 * optimisation, so reminders are not delayed. Shown only while the app is still
 * optimised; the state is re-checked when the user returns from Settings.
 */
@Composable
fun BatteryOptimizationHint(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var isOptimized by remember { mutableStateOf(BatteryOptimization.isOptimized(context)) }
    var showGuide by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        isOptimized = BatteryOptimization.isOptimized(context)
        onPauseOrDispose { }
    }
    if (!isOptimized) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ExpiraColors.PrimaryContainer, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResource(R.string.battery_hint),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal, lineHeight = 17.sp),
            color = ExpiraColors.Text,
        )
        Text(
            text = stringResource(R.string.battery_hint_action),
            style = MaterialTheme.typography.labelMedium,
            color = ExpiraColors.Primary,
            modifier = Modifier.clickable(role = Role.Button) { showGuide = true },
        )
    }

    if (showGuide) {
        BatteryGuideDialog(
            onOpenSettings = {
                showGuide = false
                BatteryOptimization.openSettings(context)
            },
            onDismiss = { showGuide = false },
        )
    }
}

@Composable
private fun BatteryGuideDialog(onOpenSettings: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.battery_guide_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(R.string.battery_guide_step1, R.string.battery_guide_step2, R.string.battery_guide_step3)
                    .forEachIndexed { index, step -> GuideStep(number = index + 1, text = stringResource(step)) }
                Text(
                    text = stringResource(R.string.battery_guide_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = ExpiraColors.TextMuted,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onOpenSettings) { Text(stringResource(R.string.battery_guide_open)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.battery_guide_later)) }
        },
        containerColor = ExpiraColors.Surface,
    )
}

@Composable
private fun GuideStep(number: Int, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "$number.",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = ExpiraColors.Primary,
        )
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = ExpiraColors.Text)
    }
}

/** Battery optimisation status and settings shortcut (Doze exists on every supported API, 24+). */
object BatteryOptimization {

    fun isOptimized(context: Context): Boolean {
        val power = context.getSystemService(PowerManager::class.java) ?: return false
        return !power.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Opens the system list of battery-optimised apps. This needs no special permission,
     * unlike ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, which Play restricts. Falls back to
     * the app's own settings page on devices without that screen.
     */
    fun openSettings(context: Context) {
        val list = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        val appDetails = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
        for (intent in listOf(list, appDetails)) {
            try {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            } catch (e: ActivityNotFoundException) {
                continue
            }
        }
    }
}
