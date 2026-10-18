package com.aditya.expent

import android.app.Application
import com.aditya.expent.data.sync.SyncScheduler
import com.aditya.expent.presentation.widget.AddTransactionWidget
import com.aditya.expent.utils.AppLogger
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
        val user = sessionManager.getUser()
        AppLogger.setUser(user?.id)
        if (user != null) {
            syncScheduler.schedulePeriodicSync()
            syncScheduler.scheduleInitialSync()
        }
        AddTransactionWidget.updateAll(this)
    }
}
