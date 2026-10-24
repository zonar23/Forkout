package io.github.zonar23.forkout.timer

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class RestTimerSchedulerTest {

    private val now = Instant.parse("2026-10-24T10:00:00Z")
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val scheduler = RestTimerScheduler(context, Clock.fixed(now, ZoneOffset.UTC))
    private val alarms = shadowOf(context.getSystemService(AlarmManager::class.java))
    private val notifications = shadowOf(context.getSystemService(NotificationManager::class.java))

    @Before
    fun setUp() = scheduler.createChannels()

    private fun rest(seconds: Int, next: String? = "Latzug") = RestInfo(
        afterSetId = 7,
        endsAt = now.plusSeconds(seconds.toLong()),
        totalSeconds = seconds,
        nextExerciseName = next,
        nextSetNumber = 2,
    )

    private fun alarmTimes() = alarms.scheduledAlarms.map { it.triggerAtMs }.sorted()

    @Test
    fun start_showsCountdownAndSchedulesWarningAndEnd() {
        scheduler.start(rest(90))

        assertEquals(listOf(now.plusSeconds(80).toEpochMilli(), now.plusSeconds(90).toEpochMilli()), alarmTimes())
        val notification = notifications.getNotification(RestTimerScheduler.NOTIFICATION_REST)
        assertNotNull(notification)
        assertEquals(now.plusSeconds(90).toEpochMilli(), notification.`when`)
        assertTrue(notification.extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN))
        assertEquals("Als Nächstes: Latzug, Satz 2", notification.extras.getString(Notification.EXTRA_TEXT))
        assertEquals("Überspringen", notification.actions.single().title)
        assertEquals(Notification.VISIBILITY_PUBLIC, notification.visibility)
    }

    @Test
    fun shortRest_hasNoWarning() {
        scheduler.start(rest(15))

        assertEquals(listOf(now.plusSeconds(15).toEpochMilli()), alarmTimes())
    }

    @Test
    fun newRest_replacesRunningOne() {
        scheduler.start(rest(90))
        scheduler.start(rest(60))

        assertEquals(listOf(now.plusSeconds(50).toEpochMilli(), now.plusSeconds(60).toEpochMilli()), alarmTimes())
        assertEquals(1, notifications.allNotifications.size)
    }

    @Test
    fun cancel_removesAlarmsAndNotification() {
        scheduler.start(rest(90))

        scheduler.cancel()

        assertTrue(alarms.scheduledAlarms.isEmpty())
        assertNull(notifications.getNotification(RestTimerScheduler.NOTIFICATION_REST))
    }

    @Test
    fun restOver_replacesCountdownByShortNotice() {
        scheduler.start(rest(90))

        scheduler.showRestOver("Latzug, Satz 2")

        assertNull(notifications.getNotification(RestTimerScheduler.NOTIFICATION_REST))
        val notice = notifications.getNotification(RestTimerScheduler.NOTIFICATION_REST_OVER)
        assertEquals("Pause vorbei", notice.extras.getString(Notification.EXTRA_TITLE))
        assertEquals("Weiter mit Latzug, Satz 2", notice.extras.getString(Notification.EXTRA_TEXT))
    }

    @Test
    fun nextRest_removesOutdatedRestOverNotice() {
        scheduler.start(rest(90))
        scheduler.showRestOver("Latzug, Satz 2")

        scheduler.start(rest(60))

        assertNull(notifications.getNotification(RestTimerScheduler.NOTIFICATION_REST_OVER))
        assertNotNull(notifications.getNotification(RestTimerScheduler.NOTIFICATION_REST))
    }

    @Test
    fun restInThePast_schedulesNothing() {
        scheduler.start(rest(90).copy(endsAt = now.minusSeconds(1)))

        assertTrue(alarms.scheduledAlarms.isEmpty())
        assertNull(notifications.getNotification(RestTimerScheduler.NOTIFICATION_REST))
    }
}
