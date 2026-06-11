package com.wanda.app.geofencing

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.wanda.app.WandaApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "GeofenceBroadcastRcvr"
        private const val NOTIFICATION_ID = 2001
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "🟢 onReceive TRIGGERED - action received: ${intent.action}")

        // goAsync() tells Android that this BroadcastReceiver needs more time
        // to complete its work (asynchronous operations). Without this, Android may
        // kill the process before we finish saving to the database.
        val pendingResult = goAsync()

        try {
            val geofencingEvent = GeofencingEvent.fromIntent(intent)

            if (geofencingEvent == null) {
                Log.e(TAG, "🔴 GeofencingEvent is null - intent does not contain geofence data")
                pendingResult.finish()
                return
            }

            if (geofencingEvent.hasError()) {
                Log.e(TAG, "🔴 Geofencing error. Code: ${geofencingEvent.errorCode}")
                pendingResult.finish()
                return
            }

            val transitionType = geofencingEvent.geofenceTransition
            val triggeringGeofences = geofencingEvent.triggeringGeofences

            Log.d(TAG, "ℹ️ Transition detected: $transitionType (1=Entry, 2=Exit)")

            if (triggeringGeofences.isNullOrEmpty()) {
                Log.w(TAG, "⚠️ No geofences associated with the event")
                pendingResult.finish()
                return
            }

            val triggeringLocation = geofencingEvent.triggeringLocation
            val latitude = triggeringLocation?.latitude ?: 0.0
            val longitude = triggeringLocation?.longitude ?: 0.0

            when (transitionType) {
                Geofence.GEOFENCE_TRANSITION_ENTER -> {
                    Log.d(TAG, "🏠 ENTRY detected at ($latitude, $longitude)")
                    // Save to DB and notify on background thread
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            handleTransition(context, "ENTRY", latitude, longitude)
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error in handleTransition ENTRY: ${e.message}", e)
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
                Geofence.GEOFENCE_TRANSITION_EXIT -> {
                    Log.d(TAG, "🚶 EXIT detected at ($latitude, $longitude)")
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            handleTransition(context, "EXIT", latitude, longitude)
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error in handleTransition EXIT: ${e.message}", e)
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
                else -> {
                    Log.w(TAG, "⚠️ Unknown transition type: $transitionType")
                    pendingResult.finish()
                }
            }
        } catch (e: Exception) {
            // Catch any unexpected exception so the app does NOT crash
            Log.e(TAG, "❌ Unexpected exception in onReceive: ${e.message}", e)
            pendingResult.finish()
        }
    }

    private suspend fun handleTransition(
        context: Context,
        type: String,
        latitude: Double,
        longitude: Double
    ) {
        Log.d(TAG, "💾 Saving '$type' record to database...")

        try {
            val application = context.applicationContext as WandaApplication
            application.gpsRepository.insertRecord(type, latitude, longitude)
            Log.d(TAG, "✅ '$type' record saved to DB successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving to DB: ${e.message}", e)
        }

        // Show notification on the main thread (required for NotificationManager)
        try {
            showNotification(context, type)
            Log.d(TAG, "🔔 '$type' notification shown successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error showing notification: ${e.message}", e)
        }
    }

    private fun showNotification(context: Context, type: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val (title, message) = when (type) {
            "ENTRY" -> Pair(
                "You have arrived home 🏠",
                "WANDA has recorded your arrival. Welcome back!"
            )
            "EXIT" -> Pair(
                "You have left home 🚶",
                "WANDA has recorded your departure. Take care!"
            )
            else -> return
        }

        // We use android.R.drawable.ic_menu_mylocation: a system Android icon
        // that is ALWAYS available and compatible with notifications (monochrome).
        // Custom icons with hardcoded colors can crash the app.
        val notification = NotificationCompat.Builder(context, "events_channel")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
