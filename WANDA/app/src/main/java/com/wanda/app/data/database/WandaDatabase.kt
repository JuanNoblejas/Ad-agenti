package com.wanda.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.wanda.app.data.database.dao.GpsRecordDao
import com.wanda.app.data.database.dao.ShoppingDao
import com.wanda.app.data.database.entity.GpsRecord
import com.wanda.app.data.database.entity.ShoppingItem

@Database(
    entities = [ShoppingItem::class, GpsRecord::class],
    version = 1,
    exportSchema = false
)
abstract class WandaDatabase : RoomDatabase() {

    abstract fun shoppingDao(): ShoppingDao
    abstract fun gpsRecordDao(): GpsRecordDao

    companion object {
        @Volatile
        private var INSTANCE: WandaDatabase? = null

        fun getDatabase(context: Context): WandaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WandaDatabase::class.java,
                    "wanda_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
