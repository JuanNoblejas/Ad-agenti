package com.wanda.app.geofencing

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class GeofenceHelper(private val context: Context) {

    private val geofencingClient: GeofencingClient =
        LocationServices.getGeofencingClient(context)

    private val GEOFENCE_ID = "HOME_GEOFENCE"

    fun createGeofence(lat: Double, lng: Double, radius: Float): Geofence {
        return Geofence.Builder()
            .setRequestId(GEOFENCE_ID)
            .setCircularRegion(lat, lng, radius)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT
            )
            .build()
    }

    fun getGeofencingRequest(geofence: Geofence): GeofencingRequest {
        return GeofencingRequest.Builder()
            // INITIAL_TRIGGER_ENTER: notifies if you are already inside when registering the geofence
            // INITIAL_TRIGGER_EXIT: notifies if you are already outside when registering the geofence
            .setInitialTrigger(
                GeofencingRequest.INITIAL_TRIGGER_ENTER or
                GeofencingRequest.INITIAL_TRIGGER_EXIT
            )
            .addGeofence(geofence)
            .build()
    }

    fun getGeofencePendingIntent(): PendingIntent {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java).apply {
            action = ACTION_GEOFENCE_EVENT  // reference to the companion object
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            flags
        )
    }

    @SuppressLint("MissingPermission")
    fun addGeofence(
        lat: Double,
        lng: Double,
        radius: Float,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val geofence = createGeofence(lat, lng, radius)
        val request = getGeofencingRequest(geofence)
        val pendingIntent = getGeofencePendingIntent()

        Log.d("GeofenceHelper", "Attempting to add geofence: $GEOFENCE_ID at lat: $lat, lng: $lng, radius: $radius")
        geofencingClient.addGeofences(request, pendingIntent)
            .addOnSuccessListener { 
                Log.d("GeofenceHelper", "Geofence added successfully")
                onSuccess() 
            }
            .addOnFailureListener { exception -> 
                Log.e("GeofenceHelper", "Error adding geofence: ${exception.message}", exception)
                onFailure(exception) 
            }
    }

    fun removeGeofence(onSuccess: (() -> Unit)? = null, onFailure: ((Exception) -> Unit)? = null) {
        geofencingClient.removeGeofences(listOf(GEOFENCE_ID))
            .addOnSuccessListener {
                Log.d("GeofenceHelper", "Geofence '$GEOFENCE_ID' removed successfully")
                onSuccess?.invoke()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error removing geofence: ${exception.message}", exception)
                onFailure?.invoke(exception)
            }
    }

    companion object {
        private const val TAG = "GeofenceHelper"
        // CRITICAL: explicit action for the PendingIntent.
        // Without this, Android 12+ silently discards the geofencing broadcast.
        const val ACTION_GEOFENCE_EVENT = "com.wanda.app.ACTION_GEOFENCE_EVENT"
    }
}
