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
        Log.d(TAG, "🟢 onReceive ACTIVADO - acción recibida: ${intent.action}")

        // goAsync() le dice a Android que este BroadcastReceiver necesita más tiempo
        // para completar su trabajo (operaciones asíncronas). Sin esto, Android puede
        // matar el proceso antes de que terminemos de guardar en la base de datos.
        val pendingResult = goAsync()

        try {
            val geofencingEvent = GeofencingEvent.fromIntent(intent)

            if (geofencingEvent == null) {
                Log.e(TAG, "🔴 GeofencingEvent es null - el intent no contiene datos de geovalla")
                pendingResult.finish()
                return
            }

            if (geofencingEvent.hasError()) {
                Log.e(TAG, "🔴 Error en Geofencing. Código: ${geofencingEvent.errorCode}")
                pendingResult.finish()
                return
            }

            val transitionType = geofencingEvent.geofenceTransition
            val triggeringGeofences = geofencingEvent.triggeringGeofences

            Log.d(TAG, "ℹ️ Transición detectada: $transitionType (1=Entrada, 2=Salida)")

            if (triggeringGeofences.isNullOrEmpty()) {
                Log.w(TAG, "⚠️ Sin geovallas asociadas al evento")
                pendingResult.finish()
                return
            }

            val triggeringLocation = geofencingEvent.triggeringLocation
            val latitude = triggeringLocation?.latitude ?: 0.0
            val longitude = triggeringLocation?.longitude ?: 0.0

            when (transitionType) {
                Geofence.GEOFENCE_TRANSITION_ENTER -> {
                    Log.d(TAG, "🏠 ENTRADA detectada en ($latitude, $longitude)")
                    // Guardar en BD y notificar en hilo de fondo
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            handleTransition(context, "ENTRADA", latitude, longitude)
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error en handleTransition ENTRADA: ${e.message}", e)
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
                Geofence.GEOFENCE_TRANSITION_EXIT -> {
                    Log.d(TAG, "🚶 SALIDA detectada en ($latitude, $longitude)")
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            handleTransition(context, "SALIDA", latitude, longitude)
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error en handleTransition SALIDA: ${e.message}", e)
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
                else -> {
                    Log.w(TAG, "⚠️ Tipo de transición desconocida: $transitionType")
                    pendingResult.finish()
                }
            }
        } catch (e: Exception) {
            // Capturamos cualquier excepción inesperada para que NO crashee la app
            Log.e(TAG, "❌ Excepción inesperada en onReceive: ${e.message}", e)
            pendingResult.finish()
        }
    }

    private suspend fun handleTransition(
        context: Context,
        type: String,
        latitude: Double,
        longitude: Double
    ) {
        Log.d(TAG, "💾 Guardando registro '$type' en base de datos...")

        try {
            val application = context.applicationContext as WandaApplication
            application.gpsRepository.insertRecord(type, latitude, longitude)
            Log.d(TAG, "✅ Registro '$type' guardado en BD correctamente")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al guardar en BD: ${e.message}", e)
        }

        // Mostrar notificación en el hilo principal (necesario para el NotificationManager)
        try {
            showNotification(context, type)
            Log.d(TAG, "🔔 Notificación '$type' mostrada correctamente")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al mostrar notificación: ${e.message}", e)
        }
    }

    private fun showNotification(context: Context, type: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val (title, message) = when (type) {
            "ENTRADA" -> Pair(
                "Has llegado a casa 🏠",
                "WANDA ha registrado tu llegada. ¡Bienvenido/a!"
            )
            "SALIDA" -> Pair(
                "Has salido de casa 🚶",
                "WANDA ha registrado tu salida. ¡Ten cuidado!"
            )
            else -> return
        }

        // Usamos android.R.drawable.ic_menu_mylocation: un icono del sistema Android
        // que SIEMPRE está disponible y es compatible con notificaciones (monocromático).
        // Los iconos personalizados con colores hardcodeados pueden crashear la app.
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
