package com.vzith.bookstack.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.vzith.bookstack.data.db.dao.*
import com.vzith.bookstack.data.db.entity.*

/**
 * BookStack Android App - Room Database (2026-01-05)
 */
@Database(
    entities = [
        BookEntity::class,
        ChapterEntity::class,
        PageEntity::class,
        SyncQueueEntity::class,
        YDocStateEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class BookStackDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao
    abstract fun chapterDao(): ChapterDao
    abstract fun pageDao(): PageDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun yDocStateDao(): YDocStateDao

    companion object {
        @Volatile
        private var INSTANCE: BookStackDatabase? = null

        fun getDatabase(context: Context): BookStackDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BookStackDatabase::class.java,
                    "bookstack_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
