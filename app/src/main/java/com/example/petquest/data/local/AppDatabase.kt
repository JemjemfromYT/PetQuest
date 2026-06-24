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
        db.execSQL("INSERT OR IGNORE INTO achievements (title, description, isUnlocked) VALUES ('Bond Veteran', 'Prove your dedication by reaching Bond Level 10', 0)")
        db.execSQL("INSERT OR IGNORE INTO achievements (title, description, isUnlocked) VALUES ('Level 10 Companion', 'Your pet trusts you completely — Bond Level 10 reached', 0)")
        db.execSQL("INSERT OR IGNORE INTO achievements (title, description, isUnlocked) VALUES ('Virtue Master', 'Embody your pet''s virtue through consistent daily practice', 0)")
        db.execSQL("INSERT OR IGNORE INTO achievements (title, description, isUnlocked) VALUES ('Dedicated Caregiver', 'Show daily dedication and reach Bond Level 10', 0)")
        db.execSQL("INSERT OR IGNORE INTO achievements (title, description, isUnlocked) VALUES ('Elite Trainer', 'Achieve elite bond status through patience and care', 0)")
    }
}

@Database(
    entities = [PetEntity::class, TaskEntity::class, AchievementEntity::class],
    version = 6,
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
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
