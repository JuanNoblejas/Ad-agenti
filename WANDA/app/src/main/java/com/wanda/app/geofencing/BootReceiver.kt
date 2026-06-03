package com.wanda.app.geofencing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.wanda.app.utils.PreferencesManager

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Boot completed received")

            val preferencesManager = PreferencesManager(context)

            if (preferencesManager.isTrackingEnabled) {
                Log.d(TAG, "Tracking was enabled, restarting LocationTrackingService")
                LocationTrackingService.start(context)
            } else {
                Log.d(TAG, "Tracking was not enabled, skipping service restart")
            }
        }
    }
}
