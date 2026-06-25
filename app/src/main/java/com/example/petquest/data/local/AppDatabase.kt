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

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("INSERT OR IGNORE INTO achievements (title, description, isUnlocked) VALUES ('Bond Veteran', 'Reach Bond Level 10 with any pet', 0)")
        db.execSQL("INSERT OR IGNORE INTO achievements (title, description, isUnlocked) VALUES ('Level 10 Companion', 'Reach Bond Level 15 with any pet', 0)")
        db.execSQL("INSERT OR IGNORE INTO achievements (title, description, isUnlocked) VALUES ('Virtue Master', 'Reach Bond Level 20 with any pet', 0)")
        db.execSQL("INSERT OR IGNORE INTO achievements (title, description, isUnlocked) VALUES ('Dedicated Caregiver', 'Reach Bond Level 30 with any pet', 0)")
        db.execSQL("INSERT OR IGNORE INTO achievements (title, description, isUnlocked) VALUES ('Elite Trainer', 'Reach Bond Level 50 with any pet', 0)")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("INSERT OR IGNORE INTO achievements (title, description, isUnlocked) VALUES ('14-Day Streak',       'Maintain a 14-day streak', 0)")
        db.execSQL("INSERT OR IGNORE INTO achievements (title, description, isUnlocked) VALUES ('60-Day Streak',       'Maintain a 60-day streak', 0)")
        db.execSQL("INSERT OR IGNORE INTO achievements (title, description, isUnlocked) VALUES ('100-Day Streak',      'Maintain a 100-day streak', 0)")
        db.execSQL("INSERT OR IGNORE INTO achievements (title, description, isUnlocked) VALUES ('Complete 10 Tasks',   'Complete 10 tasks in total', 0)")
        db.execSQL("INSERT OR IGNORE INTO achievements (title, description, isUnlocked) VALUES ('Complete 500 Tasks',  'Complete 500 tasks in total', 0)")
        db.execSQL("INSERT OR IGNORE INTO achievements (title, description, isUnlocked) VALUES ('Earn 100 Bond Points',  'Accumulate 100 total bond points', 0)")
        db.execSQL("INSERT OR IGNORE INTO achievements (title, description, isUnlocked) VALUES ('Earn 2500 Bond Points', 'Accumulate 2500 total bond points', 0)")
        db.execSQL("INSERT OR IGNORE INTO achievements (title, description, isUnlocked) VALUES ('Earn 5000 Bond Points', 'Accumulate 5000 total bond points', 0)")
        db.execSQL("INSERT OR IGNORE INTO achievements (title, description, isUnlocked) VALUES ('Reach Level 30', 'Reach Trainer Level 30', 0)")
        db.execSQL("INSERT OR IGNORE INTO achievements (title, description, isUnlocked) VALUES ('Reach Level 50', 'Reach Trainer Level 50', 0)")
        db.execSQL("INSERT OR IGNORE INTO achievements (title, description, isUnlocked) VALUES ('Own 10 Pets',    'Own 10 or more pets at once', 0)")
        db.execSQL("INSERT OR IGNORE INTO achievements (title, description, isUnlocked) VALUES ('Verify 10 Pets', 'Verify 10 or more pets', 0)")
        db.execSQL("INSERT OR IGNORE INTO achievements (title, description, isUnlocked) VALUES ('Master Explorer', 'Collect 15 different species', 0)")
    }
}

@Database(
    entities = [PetEntity::class, TaskEntity::class, AchievementEntity::class],
    version = 7,
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
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
