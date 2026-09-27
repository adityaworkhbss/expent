package com.aditya.expent.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager: WorkManager
        get() = WorkManager.getInstance(context)

    private val networkConstraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun scheduleInitialSync() {
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(networkConstraints)
            .setInputData(workDataOf(KEY_FULL_REFRESH to true))
            .build()

        workManager.enqueueUniqueWork(
            INITIAL_SYNC_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }

    fun schedulePeriodicSync() {
        val periodicRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(networkConstraints)
            .setInitialDelay(15, TimeUnit.MINUTES)
            .setInputData(workDataOf(KEY_FULL_REFRESH to true))
            .build()

        workManager.enqueueUniquePeriodicWork(
            PERIODIC_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )
    }

    fun enqueueCategorySync() = triggerOneTimeSync("category_sync")
    fun enqueueTransactionSync() = triggerOneTimeSync("transaction_sync")
    fun enqueueAccountSync() = triggerOneTimeSync("account_sync")
    fun enqueueBudgetSync() = triggerOneTimeSync("budget_sync")
    fun enqueueExpenseSync() = triggerOneTimeSync("expense_sync")
    fun enqueueCustomizationSync() = triggerOneTimeSync("customization_sync")
    fun enqueueOnboardingSync() = triggerOneTimeSync("onboarding_sync")

    private fun triggerOneTimeSync(name: String, fullRefresh: Boolean = false) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(networkConstraints)
            .setInputData(workDataOf(KEY_FULL_REFRESH to fullRefresh))
            .build()

        workManager.enqueueUniqueWork(
            name,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    companion object {
        const val KEY_FULL_REFRESH = "key_full_refresh"
        const val INITIAL_SYNC_WORK_NAME = "expent_initial_sync"
        const val PERIODIC_SYNC_WORK_NAME = "expent_periodic_sync"
    }
}
