package com.vzith.bookstack.data.db.dao

import androidx.room.*
import com.vzith.bookstack.data.db.entity.BookEntity
import kotlinx.coroutines.flow.Flow

/**
 * BookStack Android App - Book DAO (2026-01-05)
 */
@Dao
interface BookDao {

    @Query("SELECT * FROM books ORDER BY name ASC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBook(id: Int): BookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<BookEntity>)

    @Delete
    suspend fun deleteBook(book: BookEntity)

    @Query("DELETE FROM books")
    suspend fun deleteAllBooks()
}
