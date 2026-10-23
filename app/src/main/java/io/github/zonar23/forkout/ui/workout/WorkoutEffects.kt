package io.github.zonar23.forkout.ui.workout

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.time.Instant

/** Keeps the display on while this composable is shown. */
@Composable
fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

/** The current time, refreshed a few times per second for timers. */
@Composable
fun rememberNow(intervalMillis: Long = 250): State<Instant> =
    produceState(initialValue = Instant.now()) {
        while (true) {
            value = Instant.now()
            delay(intervalMillis)
        }
    }

/** Short double buzz at the end of a rest. */
fun vibrateRestOver(context: Context) {
    val vibrator = ContextCompat.getSystemService(context, Vibrator::class.java) ?: return
    if (!vibrator.hasVibrator()) return
    vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300), -1))
}
