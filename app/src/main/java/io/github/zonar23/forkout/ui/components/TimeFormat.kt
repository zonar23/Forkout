package io.github.zonar23.forkout.ui.components

object TimeFormat {

    /** Formats seconds as minutes and seconds, e.g. 90 → "1:30", 45 → "0:45". */
    fun formatMinutesSeconds(totalSeconds: Int): String {
        val seconds = totalSeconds.coerceAtLeast(0)
        return "%d:%02d".format(seconds / 60, seconds % 60)
    }

    /** Formats an elapsed time, with hours only when needed: "12:05" or "1:02:03". */
    fun formatElapsed(totalSeconds: Long): String {
        val seconds = totalSeconds.coerceAtLeast(0)
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val rest = seconds % 60
        return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, rest) else "%d:%02d".format(minutes, rest)
    }
}
