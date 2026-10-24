package io.github.zonar23.forkout.timer

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.ContextCompat

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
