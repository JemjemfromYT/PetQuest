package com.example.petquest.data.local

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.petquest.data.model.*

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN date TEXT NOT NULL DEFAULT ''")
    }
}

@Database(
    entities = [PetEntity::class, TaskEntity::class, AchievementEntity::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun petQuestDao(): PetQuestDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "petquest_database"
                )
                    .addMigrations(MIGRATION_3_4)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
