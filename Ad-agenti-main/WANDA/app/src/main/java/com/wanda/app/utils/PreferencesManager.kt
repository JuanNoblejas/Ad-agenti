package com.wanda.app.utils

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("wanda_prefs", Context.MODE_PRIVATE)

    var homeLatitude: Double
        get() {
            val bits = prefs.getLong(KEY_HOME_LATITUDE, java.lang.Double.doubleToRawLongBits(0.0))
            return java.lang.Double.longBitsToDouble(bits)
        }
        set(value) {
            prefs.edit().putLong(KEY_HOME_LATITUDE, java.lang.Double.doubleToRawLongBits(value)).apply()
        }

    var homeLongitude: Double
        get() {
            val bits = prefs.getLong(KEY_HOME_LONGITUDE, java.lang.Double.doubleToRawLongBits(0.0))
            return java.lang.Double.longBitsToDouble(bits)
        }
        set(value) {
            prefs.edit().putLong(KEY_HOME_LONGITUDE, java.lang.Double.doubleToRawLongBits(value)).apply()
        }

    var geofenceRadius: Float
        get() = prefs.getFloat(KEY_GEOFENCE_RADIUS, 100f)
        set(value) {
            prefs.edit().putFloat(KEY_GEOFENCE_RADIUS, value).apply()
        }

    var isTrackingEnabled: Boolean
        get() = prefs.getBoolean(KEY_TRACKING_ENABLED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_TRACKING_ENABLED, value).apply()
        }

    var isHomeSet: Boolean
        get() = prefs.getBoolean(KEY_HOME_SET, false)
        set(value) {
            prefs.edit().putBoolean(KEY_HOME_SET, value).apply()
        }

    fun setHomeLocation(latitude: Double, longitude: Double) {
        homeLatitude = latitude
        homeLongitude = longitude
        isHomeSet = true
    }

    fun clearHomeLocation() {
        homeLatitude = 0.0
        homeLongitude = 0.0
        geofenceRadius = 100f
        isTrackingEnabled = false
        isHomeSet = false
    }

    companion object {
        private const val KEY_HOME_LATITUDE = "home_latitude"
        private const val KEY_HOME_LONGITUDE = "home_longitude"
        private const val KEY_GEOFENCE_RADIUS = "geofence_radius"
        private const val KEY_TRACKING_ENABLED = "tracking_enabled"
        private const val KEY_HOME_SET = "home_set"
    }
}
