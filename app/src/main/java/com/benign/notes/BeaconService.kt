package com.benign.notes

import android.app.Service
import android.content.Intent
import android.os.IBinder

class BeaconService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        BeaconWorker.schedule(applicationContext)
        stopSelf()
        return START_NOT_STICKY
    }
    override fun onBind(intent: Intent?): IBinder? = null
}