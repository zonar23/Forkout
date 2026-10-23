package com.example.forkout

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.forkout.ui.ForkoutApp
import com.example.forkout.ui.theme.ForkoutTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // TODO(M1): read the AMOLED preference from DataStore.
            ForkoutTheme {
                ForkoutApp()
            }
        }
    }
}
