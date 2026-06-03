package com.wanda.app.geofencing

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.wanda.app.R
import com.wanda.app.WandaApplication

class LocationTrackingService : Service() {

    companion object {
        private const val TAG = "LocationTrackingService"
        private const val FOREGROUND_NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java)
            context.stopService(intent)
        }
    }

    private lateinit var geofenceHelper: GeofenceHelper

    override fun onCreate() {
        super.onCreate()
        geofenceHelper = GeofenceHelper(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundNotification()
        setupGeofence()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        geofenceHelper.removeGeofence()
        Log.d(TAG, "Service destroyed, geofence removed")
    }

    private fun startForegroundNotification() {
        val notification = NotificationCompat.Builder(this, "tracking_channel")
            .setSmallIcon(R.drawable.ic_location)
            .setContentTitle("WANDA - Rastreo activo")
            .setContentText("Monitorizando tu ubicación")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(FOREGROUND_NOTIFICATION_ID, notification)
    }

    private fun setupGeofence() {
        val application = applicationContext as WandaApplication
        val preferencesManager = application.preferencesManager

        if (!preferencesManager.isHomeSet) {
            Log.w(TAG, "Home location not set, skipping geofence setup")
            return
        }

        val latitude = preferencesManager.homeLatitude
        val longitude = preferencesManager.homeLongitude
        val radius = preferencesManager.geofenceRadius

        geofenceHelper.addGeofence(
            lat = latitude,
            lng = longitude,
            radius = radius,
            onSuccess = {
                Log.d(TAG, "Geofence added successfully at ($latitude, $longitude) with radius $radius m")
            },
            onFailure = { exception ->
                Log.e(TAG, "Failed to add geofence: ${exception.message}", exception)
            }
        )
    }
}
