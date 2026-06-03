package com.wanda.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.wanda.app.data.database.WandaDatabase
import com.wanda.app.data.repository.GpsRepository
import com.wanda.app.data.repository.ShoppingRepository
import com.wanda.app.utils.PreferencesManager

class WandaApplication : Application() {

    lateinit var database: WandaDatabase
    lateinit var shoppingRepository: ShoppingRepository
    lateinit var gpsRepository: GpsRepository
    lateinit var preferencesManager: PreferencesManager

    override fun onCreate() {
        super.onCreate()

        database = WandaDatabase.getDatabase(this)
        shoppingRepository = ShoppingRepository(database.shoppingDao())
        gpsRepository = GpsRepository(database.gpsRecordDao())
        preferencesManager = PreferencesManager(this)

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val trackingChannel = NotificationChannel(
                TRACKING_CHANNEL_ID,
                "Rastreo de Ubicación",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Canal para notificaciones de rastreo de ubicación"
            }

            val eventsChannel = NotificationChannel(
                EVENTS_CHANNEL_ID,
                "Eventos de Movimiento",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Canal para notificaciones de eventos de movimiento"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(trackingChannel)
            notificationManager.createNotificationChannel(eventsChannel)
        }
    }

    companion object {
        const val TRACKING_CHANNEL_ID = "tracking_channel"
        const val EVENTS_CHANNEL_ID = "events_channel"
    }
}
