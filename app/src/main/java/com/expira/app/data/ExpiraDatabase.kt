package com.expira.app.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ScanEntity::class, EvaluationLogEntity::class],
    version = 3,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
)
abstract class ExpiraDatabase : RoomDatabase() {
    abstract fun scanDao(): ScanDao

    abstract fun evaluationDao(): EvaluationDao

    companion object {
        const val NAME = "expira.db"

        fun create(context: Context): ExpiraDatabase =
            Room.databaseBuilder(context, ExpiraDatabase::class.java, NAME).build()
    }
}
