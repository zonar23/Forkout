package io.github.zonar23.forkout.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.github.zonar23.forkout.R
import io.github.zonar23.forkout.data.db.relation.SessionOverview
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateFormat {

    /** The app ships German only for now, so dates use German formatting. */
    private val locale: Locale = Locale.GERMAN
    private val dateWithWeekday = DateTimeFormatter.ofPattern("EEE, dd.MM.yyyy", locale)

    /** "Fr., 23.10.2026" */
    fun formatDate(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
        dateWithWeekday.format(instant.atZone(zone))
}

/** "Fr., 23.10.2026 · 45 min · 12 Sätze" for workout lists. */
@Composable
fun SessionOverview.summaryLine(): String {
    val finishedAt = session.finishedAt ?: session.startedAt
    return listOf(
        DateFormat.formatDate(session.startedAt),
        formatDuration(Duration.between(session.startedAt, finishedAt)),
        pluralStringResource(R.plurals.history_sets, completedSets, completedSets),
    ).joinToString(" · ")
}

/** Workout length for lists: "45 min" or "1 h 05 min". */
@Composable
fun formatDuration(duration: Duration): String {
    val totalMinutes = duration.toMinutes().coerceAtLeast(0)
    val hours = totalMinutes / 60
    val minutes = (totalMinutes % 60).toInt()
    return if (hours > 0) {
        stringResource(R.string.duration_hours_minutes, hours.toInt(), minutes)
    } else {
        stringResource(R.string.duration_minutes, minutes)
    }
}
