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
            // INITIAL_TRIGGER_ENTER: notifica si ya estás dentro al registrar la geovalla
            // INITIAL_TRIGGER_EXIT: notifica si ya estás fuera al registrar la geovalla
            .setInitialTrigger(
                GeofencingRequest.INITIAL_TRIGGER_ENTER or
                GeofencingRequest.INITIAL_TRIGGER_EXIT
            )
            .addGeofence(geofence)
            .build()
    }

    fun getGeofencePendingIntent(): PendingIntent {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java).apply {
            action = ACTION_GEOFENCE_EVENT  // referencia al companion object
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

        Log.d("GeofenceHelper", "Intentando añadir geofence: $GEOFENCE_ID en lat: $lat, lng: $lng, radio: $radius")
        geofencingClient.addGeofences(request, pendingIntent)
            .addOnSuccessListener { 
                Log.d("GeofenceHelper", "Geofence añadida exitosamente")
                onSuccess() 
            }
            .addOnFailureListener { exception -> 
                Log.e("GeofenceHelper", "Error al añadir geofence: ${exception.message}", exception)
                onFailure(exception) 
            }
    }

    fun removeGeofence(onSuccess: (() -> Unit)? = null, onFailure: ((Exception) -> Unit)? = null) {
        geofencingClient.removeGeofences(listOf(GEOFENCE_ID))
            .addOnSuccessListener {
                Log.d("GeofenceHelper", "Geofence '$GEOFENCE_ID' eliminada correctamente")
                onSuccess?.invoke()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error al eliminar geofence: ${exception.message}", exception)
                onFailure?.invoke(exception)
            }
    }

    companion object {
        private const val TAG = "GeofenceHelper"
        // CRÍTICO: acción explícita para el PendingIntent.
        // Sin esto, Android 12+ descarta el broadcast de geofencing silenciosamente.
        const val ACTION_GEOFENCE_EVENT = "com.wanda.app.ACTION_GEOFENCE_EVENT"
    }
}
