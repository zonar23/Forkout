package io.github.zonar23.forkout.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.AndroidEntryPoint
import io.github.zonar23.forkout.data.repository.RestStateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Fires shortly before and at the end of a rest, also while the screen is locked. */
@AndroidEntryPoint
class RestAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var scheduler: RestTimerScheduler

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_WARNING -> vibrateRestWarning(context)
            ACTION_END -> {
                vibrateRestOver(context)
                // In the app the ring shows the end already; outside it a short notice helps.
                val inForeground = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
                if (inForeground) {
                    scheduler.cancel()
                } else {
                    scheduler.showRestOver(intent.getStringExtra(RestTimerScheduler.EXTRA_NEXT_LABEL))
                }
            }
        }
    }

    companion object {
        const val ACTION_WARNING = "io.github.zonar23.forkout.action.REST_WARNING"
        const val ACTION_END = "io.github.zonar23.forkout.action.REST_END"
    }
}

/** Handles "Skip" in the rest notification. */
@AndroidEntryPoint
class RestActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var scheduler: RestTimerScheduler

    @Inject
    lateinit var restState: RestStateRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SKIP) return
        val setId = intent.getLongExtra(RestTimerScheduler.EXTRA_SET_ID, -1)
        scheduler.cancel()
        if (setId < 0) return
        // Store the skip so the open app hides its ring as well.
        val pending = goAsync()
        scope.launch {
            try {
                restState.skipRestAfter(setId)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_SKIP = "io.github.zonar23.forkout.action.REST_SKIP"

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
