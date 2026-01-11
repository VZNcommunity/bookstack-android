package com.vzith.bookstack.data.db.dao

import androidx.room.*
import com.vzith.bookstack.data.db.entity.PageEntity
import kotlinx.coroutines.flow.Flow

/**
 * BookStack Android App - Page DAO (2026-01-05)
 */
@Dao
interface PageDao {

    @Query("SELECT * FROM pages WHERE bookId = :bookId ORDER BY priority ASC")
    fun getPagesByBook(bookId: Int): Flow<List<PageEntity>>

    @Query("SELECT * FROM pages WHERE chapterId = :chapterId ORDER BY priority ASC")
    fun getPagesByChapter(chapterId: Int): Flow<List<PageEntity>>

    @Query("SELECT * FROM pages WHERE id = :id")
    suspend fun getPage(id: Int): PageEntity?

    @Query("SELECT * FROM pages WHERE locallyModified = 1")
    suspend fun getLocallyModifiedPages(): List<PageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: PageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPages(pages: List<PageEntity>)

    @Update
    suspend fun updatePage(page: PageEntity)

    @Query("UPDATE pages SET html = :html, locallyModified = 1 WHERE id = :id")
    suspend fun updatePageHtml(id: Int, html: String)

    @Delete
    suspend fun deletePage(page: PageEntity)

    // Delete by ID (2026-01-11)
    @Query("DELETE FROM pages WHERE id = :id")
    suspend fun deletePageById(id: Int)

    @Query("DELETE FROM pages WHERE bookId = :bookId")
    suspend fun deletePagesByBook(bookId: Int)
}
