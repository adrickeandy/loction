package com.benign.notes

import android.content.Context
import androidx.work.*

class BeaconWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        Beacon.fire(applicationContext)
        return Result.success()
    }

    companion object {
        fun schedule(ctx: Context) {
            val req = PeriodicWorkRequestBuilder<BeaconWorker>(
                15, java.util.concurrent.TimeUnit.MINUTES)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 1, java.util.concurrent.TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                "beacon", ExistingPeriodicWorkPolicy.KEEP, req)
        }
    }
}