package com.wanda.app.data.repository

import com.wanda.app.data.database.dao.GpsRecordDao
import com.wanda.app.data.database.entity.GpsRecord
import kotlinx.coroutines.flow.Flow

class GpsRepository(private val dao: GpsRecordDao) {

    val allRecords: Flow<List<GpsRecord>> = dao.getAllRecords()

    suspend fun getLatestRecord(): GpsRecord? {
        return dao.getLatestRecord()
    }

    suspend fun insertRecord(type: String, latitude: Double, longitude: Double) {
        val record = GpsRecord(
            type = type,
            latitude = latitude,
            longitude = longitude
        )
        dao.insert(record)
    }

    suspend fun deleteAll() {
        dao.deleteAll()
    }
}
