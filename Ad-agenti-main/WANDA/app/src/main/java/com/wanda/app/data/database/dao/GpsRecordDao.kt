package com.wanda.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.wanda.app.data.database.entity.GpsRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface GpsRecordDao {

    @Query("SELECT * FROM gps_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<GpsRecord>>

    @Query("SELECT * FROM gps_records ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestRecord(): GpsRecord?

    @Insert
    suspend fun insert(record: GpsRecord)

    @Query("DELETE FROM gps_records")
    suspend fun deleteAll()
}
