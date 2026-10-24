package io.github.zonar23.forkout

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import io.github.zonar23.forkout.timer.RestTimerScheduler
import javax.inject.Inject

@HiltAndroidApp
class ForkoutApplication : Application() {

    @Inject
    lateinit var restTimerScheduler: RestTimerScheduler

    override fun onCreate() {
        super.onCreate()
        restTimerScheduler.createChannels()
    }
}
