package com.expira.app.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.expira.app.MainActivity
import com.expira.app.R
import com.expira.app.extraction.CalendarDate
import kotlin.math.abs

/** Builds and posts the reminder notification (FR-22). */
class ReminderNotifier(private val context: Context) {

    /** True when notifications may be posted (permission on Android 13+ and not blocked). */
    fun canNotify(): Boolean = hasPermission() && NotificationManagerCompat.from(context).areNotificationsEnabled()

    private fun hasPermission(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

    /** @return false when the notification could not be shown (permission missing). */
    fun show(scanId: Long, productName: String?, expiry: CalendarDate, daysRemaining: Long): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false
        ensureChannel()
        val title = productName?.takeIf { it.isNotBlank() } ?: context.getString(R.string.product_unnamed)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle(title)
            .setContentText(bodyText(expiry, daysRemaining))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openHistoryIntent(scanId))
            .setAutoCancel(true)
            .build()
        return try {
            manager.notify(NOTIFICATION_TAG, scanId.toInt(), notification)
            true
        } catch (e: SecurityException) {
            false // permission revoked between the check and the call
        }
    }

    /** "Kedaluwarsa 12 Agustus 2027 · sisa 7 hari" */
    private fun bodyText(expiry: CalendarDate, daysRemaining: Long): String {
        val months = context.resources.getStringArray(R.array.month_names)
        val date = "${expiry.day} ${months[expiry.month - 1]} ${expiry.year}"
        val days = abs(daysRemaining).toInt()
        val remaining = if (daysRemaining >= 0) {
            context.getString(R.string.days_remaining, days)
        } else {
            context.getString(R.string.days_overdue, days)
        }
        return context.getString(R.string.reminder_notification_body, date, remaining.replaceFirstChar { it.lowercase() })
    }

    private fun openHistoryIntent(scanId: Long): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_OPEN_HISTORY, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context,
            scanId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        manager.createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "expiry_reminders"
        const val NOTIFICATION_TAG = "expiry"
    }
}
