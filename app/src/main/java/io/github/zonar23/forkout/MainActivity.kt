package io.github.zonar23.forkout

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.zonar23.forkout.data.repository.SettingsRepository
import io.github.zonar23.forkout.data.repository.UserSettings
import io.github.zonar23.forkout.ui.ForkoutApp
import io.github.zonar23.forkout.ui.theme.ForkoutTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by settingsRepository.settings.collectAsStateWithLifecycle(UserSettings())
            ForkoutTheme(amoled = settings.amoledBlack) {
                ForkoutApp()
            }
        }
    }
}
