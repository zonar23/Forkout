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

/** Seconds before the end of a rest at which a short warning buzz comes. */
const val REST_WARNING_SECONDS = 10

/** Very short tick when a set is checked off. */
fun vibrateTick(context: Context) = vibrate(context, VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))

/** Single short buzz shortly before the rest ends. */
fun vibrateRestWarning(context: Context) =
    vibrate(context, VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))

/** Clear double buzz at the end of a rest. */
fun vibrateRestOver(context: Context) =
    vibrate(context, VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300), -1))

private fun vibrate(context: Context, effect: VibrationEffect) {
    val vibrator = ContextCompat.getSystemService(context, Vibrator::class.java) ?: return
    if (vibrator.hasVibrator()) vibrator.vibrate(effect)
}
