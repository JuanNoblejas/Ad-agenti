package com.wanda.app.geofencing

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.wanda.app.R
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
        Log.d(TAG, "🟢 ¡onReceive de GeofenceBroadcastReceiver ACTIVADO! Google Play Services ha enviado un evento.")
        
        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent == null) {
            Log.e(TAG, "🔴 GeofencingEvent es null")
            return
        }

        if (geofencingEvent.hasError()) {
            Log.e(TAG, "🔴 Error en el evento de Geofencing. Código de error: ${geofencingEvent.errorCode}")
            return
        }

        val transitionType = geofencingEvent.geofenceTransition
        val triggeringGeofences = geofencingEvent.triggeringGeofences

        Log.d(TAG, "ℹ️ Tipo de transición detectada: $transitionType (1 = Entrada, 2 = Salida)")

        if (triggeringGeofences.isNullOrEmpty()) {
            Log.w(TAG, "⚠️ No se encontraron geovallas asociadas al evento")
            return
        }

        val triggeringLocation = geofencingEvent.triggeringLocation
        val latitude = triggeringLocation?.latitude ?: 0.0
        val longitude = triggeringLocation?.longitude ?: 0.0

        when (transitionType) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                Log.d(TAG, "🏠 Transición detectada: ENTRADA en ($latitude, $longitude)")
                handleTransition(context, "ENTRADA", latitude, longitude)
            }
            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                Log.d(TAG, "🚶 Transición detectada: SALIDA en ($latitude, $longitude)")
                handleTransition(context, "SALIDA", latitude, longitude)
            }
            else -> {
                Log.w(TAG, "⚠️ Tipo de transición desconocida: $transitionType")
            }
        }
    }

    private fun handleTransition(
        context: Context,
        type: String,
        latitude: Double,
        longitude: Double
    ) {
        Log.d(TAG, "💾 handleTransition: guardando registro de $type en la base de datos e intentando mostrar notificación")
        val application = context.applicationContext as WandaApplication
        val gpsRepository = application.gpsRepository

        CoroutineScope(Dispatchers.IO).launch {
            gpsRepository.insertRecord(type, latitude, longitude)
        }

        showNotification(context, type)
    }

    private fun showNotification(context: Context, type: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val (title, message, iconRes) = when (type) {
            "ENTRADA" -> Triple(
                "Has llegado a casa",
                "WANDA ha registrado tu llegada. ¡Bienvenido/a!",
                R.drawable.ic_enter
            )
            "SALIDA" -> Triple(
                "Has salido de casa",
                "WANDA ha registrado tu salida. ¡Ten cuidado!",
                R.drawable.ic_exit
            )
            else -> return
        }

        val notification = NotificationCompat.Builder(context, "events_channel")
            .setSmallIcon(iconRes)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
