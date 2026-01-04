package com.vzith.bookstack.data.db.dao

import androidx.room.*
import com.vzith.bookstack.data.db.entity.ChapterEntity
import kotlinx.coroutines.flow.Flow

/**
 * BookStack Android App - Chapter DAO (2026-01-05)
 */
@Dao
interface ChapterDao {

    @Query("SELECT * FROM chapters WHERE bookId = :bookId ORDER BY priority ASC")
    fun getChaptersByBook(bookId: Int): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE id = :id")
    suspend fun getChapter(id: Int): ChapterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: ChapterEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>)

    @Delete
    suspend fun deleteChapter(chapter: ChapterEntity)

    @Query("DELETE FROM chapters WHERE bookId = :bookId")
    suspend fun deleteChaptersByBook(bookId: Int)
}
