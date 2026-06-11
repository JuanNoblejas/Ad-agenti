package com.wanda.app.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gps_records")
data class GpsRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String,           // "EXIT" or "ENTRY"
    val timestamp: Long = System.currentTimeMillis(),
    val latitude: Double,
    val longitude: Double
)
