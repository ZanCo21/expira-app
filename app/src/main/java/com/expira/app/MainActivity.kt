package com.expira.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.expira.app.ui.navigation.ExpiraNavHost
import com.expira.app.ui.theme.ExpiraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExpiraTheme {
                ExpiraNavHost()
            }
        }
    }
}
