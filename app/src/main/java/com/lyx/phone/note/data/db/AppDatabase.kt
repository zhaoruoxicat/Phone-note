package com.lyx.phone.note.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [NumberNoteEntity::class, NoteCategoryEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun numberNoteDao(): NumberNoteDao
    abstract fun noteCategoryDao(): NoteCategoryDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        private val migration1To2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS note_categories (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "name TEXT NOT NULL, " +
                        "isBuiltIn INTEGER NOT NULL, " +
                        "sortOrder INTEGER NOT NULL, " +
                        "createdAt INTEGER NOT NULL)"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_note_categories_name ON note_categories(name)")
            }
        }

        private val migration2To3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE number_notes ADD COLUMN rejectIncomingCall INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "local_call_note.db"
                ).addMigrations(migration1To2, migration2To3).build().also { instance = it }
            }
    }
}
