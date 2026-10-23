package io.github.zonar23.forkout.ui.components

object TimeFormat {

    /** Formats seconds as minutes and seconds, e.g. 90 → "1:30", 45 → "0:45". */
    fun formatMinutesSeconds(totalSeconds: Int): String {
        val seconds = totalSeconds.coerceAtLeast(0)
        return "%d:%02d".format(seconds / 60, seconds % 60)
    }
}
