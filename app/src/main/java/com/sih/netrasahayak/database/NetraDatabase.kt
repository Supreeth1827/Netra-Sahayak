package com.sih.netrasahayak.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ScreeningEntity::class],
    version = 1,
    exportSchema = false
)
abstract class NetraDatabase : RoomDatabase() {

    abstract fun screeningDao(): ScreeningDao

    companion object {
        private const val DATABASE_NAME = "netra_sahayak.db"

        @Volatile
        private var instance: NetraDatabase? = null

        fun getInstance(context: Context): NetraDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    NetraDatabase::class.java,
                    DATABASE_NAME
                )
                    // Student project: a schema change simply rebuilds the table.
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
