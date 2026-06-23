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

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create new pets table with virtue column instead of personality
        db.execSQL("""
            CREATE TABLE pets_new (
                id          INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name        TEXT    NOT NULL,
                type        TEXT    NOT NULL,
                virtue      TEXT    NOT NULL,
                bondPoints  INTEGER NOT NULL DEFAULT 0,
                bondLevel   INTEGER NOT NULL DEFAULT 1,
                isVerified  INTEGER NOT NULL DEFAULT 0,
                photoUri    TEXT
            )
        """.trimIndent())

        // Copy rows, mapping old Personality values to the nearest Virtue
        db.execSQL("""
            INSERT INTO pets_new (id, name, type, virtue, bondPoints, bondLevel, isVerified, photoUri)
            SELECT id, name, type,
                CASE personality
                    WHEN 'CURIOUS'     THEN 'WISDOM'
                    WHEN 'OBSERVANT'   THEN 'WISDOM'
                    WHEN 'PLAYFUL'     THEN 'DILIGENCE'
                    WHEN 'LAZY'        THEN 'TEMPERANCE'
                    WHEN 'SHY'         THEN 'TEMPERANCE'
                    WHEN 'FRIENDLY'    THEN 'COMPASSION'
                    WHEN 'MISCHIEVOUS' THEN 'COURAGE'
                    ELSE 'WISDOM'
                END,
                bondPoints, bondLevel, isVerified, photoUri
            FROM pets
        """.trimIndent())

        db.execSQL("DROP TABLE pets")
        db.execSQL("ALTER TABLE pets_new RENAME TO pets")
    }
}

@Database(
    entities = [PetEntity::class, TaskEntity::class, AchievementEntity::class],
    version = 5,
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
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
