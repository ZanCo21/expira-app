package com.expira.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ScanEntity::class], version = 1, exportSchema = true)
abstract class ExpiraDatabase : RoomDatabase() {
    abstract fun scanDao(): ScanDao

    companion object {
        private const val NAME = "expira.db"

        fun create(context: Context): ExpiraDatabase =
            Room.databaseBuilder(context, ExpiraDatabase::class.java, NAME).build()
    }
}
