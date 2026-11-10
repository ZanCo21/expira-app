package com.expira.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.expira.app.ui.navigation.ExpiraNavHost
import com.expira.app.ui.theme.ExpiraTheme

class MainActivity : ComponentActivity() {

    /** Set when the app is opened from a reminder notification (opens Riwayat). */
    private val openHistoryRequest = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            ExpiraTheme {
                ExpiraNavHost(
                    openHistory = openHistoryRequest.value,
                    onHistoryOpened = { openHistoryRequest.value = false },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_HISTORY, false) == true) {
            openHistoryRequest.value = true
            intent.removeExtra(EXTRA_OPEN_HISTORY)
        }
    }

    companion object {
        const val EXTRA_OPEN_HISTORY = "open_history"
    }
}
