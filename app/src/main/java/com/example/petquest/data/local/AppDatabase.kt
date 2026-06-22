package com.example.petquest.data.local

import android.content.Context
import androidx.room.*
import com.example.petquest.data.model.*

@Database(entities = [PetEntity::class, TaskEntity::class, AchievementEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun petQuestDao(): PetQuestDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "petquest_db")
                    .build().also { INSTANCE = it }
            }
    }
}