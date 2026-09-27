package com.aditya.expent

import android.app.Application
import com.aditya.expent.data.sync.SyncScheduler
import com.aditya.expent.utils.SessionManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ExpentApplication : Application() {

    @Inject
    lateinit var syncScheduler: SyncScheduler

    @Inject
    lateinit var sessionManager: SessionManager

    override fun onCreate() {
        super.onCreate()
        if (sessionManager.getUser() != null) {
            syncScheduler.schedulePeriodicSync()
            syncScheduler.scheduleInitialSync()
        }
    }
}
