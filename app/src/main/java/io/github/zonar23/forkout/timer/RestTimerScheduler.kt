package io.github.zonar23.forkout.timer

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.zonar23.forkout.MainActivity
import io.github.zonar23.forkout.R
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** What the rest notification shows. */
data class RestInfo(
    /** The set after which this rest runs. */
    val afterSetId: Long,
    val endsAt: Instant,
    val totalSeconds: Int,
    /** Exercise of the next open set, or null after the last set. */
    val nextExerciseName: String?,
    /** 1-based number of the next open set within its exercise. */
    val nextSetNumber: Int = 1,
)

/**
 * Keeps the user informed about a rest while the app is in the background or the screen is
 * locked: a notification with a system countdown, plus alarms that vibrate shortly before
 * and at the end. Needs no foreground service, because the system draws the countdown.
 */
@Singleton
class RestTimerScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: Clock,
) {
    private val alarmManager: AlarmManager? = ContextCompat.getSystemService(context, AlarmManager::class.java)
    private val notifications = NotificationManagerCompat.from(context)

    /** Registers the notification channels; called once at app start. */
    fun createChannels() {
        val manager = ContextCompat.getSystemService(context, NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_REST,
                context.getString(R.string.notification_channel_rest),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.notification_channel_rest_description)
                setShowBadge(false)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_REST_OVER,
                context.getString(R.string.notification_channel_rest_over),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.notification_channel_rest_over_description)
                // The alarm receiver vibrates itself, also when notifications are not allowed.
                enableVibration(false)
                setSound(null, null)
                setShowBadge(false)
            }
        )
    }

    /** Shows the countdown and schedules the warning and end alarms. Replaces a running rest. */
    fun start(info: RestInfo) {
        val remainingMillis = info.endsAt.toEpochMilli() - clock.millis()
        if (remainingMillis <= 0) {
            cancel()
            return
        }
        // A notice from the previous rest is outdated once the next one starts.
        notifications.cancel(NOTIFICATION_REST_OVER)
        showCountdown(info, remainingMillis)
        val warningAt = info.endsAt.minusSeconds(REST_WARNING_SECONDS.toLong())
        if (info.totalSeconds > REST_WARNING_SECONDS * 2 && warningAt.isAfter(clock.instant())) {
            setAlarm(warningAt, alarmIntent(RestAlarmReceiver.ACTION_WARNING, info))
        } else {
            alarmManager?.cancel(alarmIntent(RestAlarmReceiver.ACTION_WARNING, info))
        }
        setAlarm(info.endsAt, alarmIntent(RestAlarmReceiver.ACTION_END, info))
    }

    /** Removes countdown, notice and both alarms, e.g. after skipping or finishing the workout. */
    fun cancel() {
        alarmManager?.cancel(alarmIntent(RestAlarmReceiver.ACTION_WARNING, null))
        alarmManager?.cancel(alarmIntent(RestAlarmReceiver.ACTION_END, null))
        notifications.cancel(NOTIFICATION_REST)
        notifications.cancel(NOTIFICATION_REST_OVER)
    }

    /** Called by the end alarm: replaces the countdown by a short "rest over" notice. */
    fun showRestOver(nextLabel: String?) {
        notifications.cancel(NOTIFICATION_REST)
        if (!notifications.areNotificationsEnabled()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_REST_OVER)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(context.getString(R.string.notification_rest_over_title))
            .setContentText(nextLabel?.let { context.getString(R.string.notification_rest_over_text, it) })
            .setContentIntent(openAppIntent())
            .setAutoCancel(true)
            .setTimeoutAfter(REST_OVER_VISIBLE_MILLIS)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        notifyIfAllowed(NOTIFICATION_REST_OVER, notification)
    }

    private fun showCountdown(info: RestInfo, remainingMillis: Long) {
        if (!notifications.areNotificationsEnabled()) return
        val skip = PendingIntent.getBroadcast(
            context,
            REQUEST_SKIP,
            Intent(context, RestActionReceiver::class.java)
                .setAction(RestActionReceiver.ACTION_SKIP)
                .putExtra(EXTRA_SET_ID, info.afterSetId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_REST)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(context.getString(R.string.notification_rest_title))
            .setContentText(nextLabel(info)?.let { context.getString(R.string.notification_rest_next, it) })
            // The system counts down to "when" by itself, also on the lock screen.
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setShowWhen(true)
            .setWhen(info.endsAt.toEpochMilli())
            .setTimeoutAfter(remainingMillis)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openAppIntent())
            .addAction(0, context.getString(R.string.workout_rest_skip), skip)
            .build()
        notifyIfAllowed(NOTIFICATION_REST, notification)
    }

    @Suppress("MissingPermission") // Checked by areNotificationsEnabled() and the catch below.
    private fun notifyIfAllowed(id: Int, notification: android.app.Notification) {
        try {
            notifications.notify(id, notification)
        } catch (e: SecurityException) {
            // Notification permission was revoked in the meantime; vibration still works.
        }
    }

    private fun setAlarm(at: Instant, intent: PendingIntent) {
        val manager = alarmManager ?: return
        val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()
        if (exactAllowed) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), intent)
        } else {
            // Without exact alarms the system may delay the buzz slightly; the countdown stays exact.
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), intent)
        }
    }

    /** Same request code and action for scheduling and cancelling; extras do not affect matching. */
    private fun alarmIntent(action: String, info: RestInfo?): PendingIntent = PendingIntent.getBroadcast(
        context,
        if (action == RestAlarmReceiver.ACTION_WARNING) REQUEST_WARNING else REQUEST_END,
        Intent(context, RestAlarmReceiver::class.java)
            .setAction(action)
            .putExtra(EXTRA_NEXT_LABEL, info?.let(::nextLabel)),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** "Beinpresse, Satz 2" */
    private fun nextLabel(info: RestInfo): String? = info.nextExerciseName?.let {
        context.getString(R.string.workout_next_set_label, it, info.nextSetNumber)
    }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        REQUEST_OPEN,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        const val CHANNEL_REST = "rest_timer"
        const val CHANNEL_REST_OVER = "rest_over"
        const val NOTIFICATION_REST = 1
        const val NOTIFICATION_REST_OVER = 2
        const val EXTRA_SET_ID = "set_id"
        const val EXTRA_NEXT_LABEL = "next_label"


        private const val REQUEST_WARNING = 1
        private const val REQUEST_END = 2
        private const val REQUEST_SKIP = 3
        private const val REQUEST_OPEN = 4
        private const val REST_OVER_VISIBLE_MILLIS = 60_000L
    }
}
