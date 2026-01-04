package com.vzith.bookstack.data.repository

import com.vzith.bookstack.BookStackApplication
import com.vzith.bookstack.data.api.BookStackApiClient
import com.vzith.bookstack.data.db.BookStackDatabase
import com.vzith.bookstack.data.db.entity.BookEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * BookStack Android App - Book Repository (2026-01-05)
 *
 * Offline-first repository with API sync
 */
class BookRepository {

    private val database = BookStackDatabase.getDatabase(BookStackApplication.instance)
    private val bookDao = database.bookDao()

    /**
     * Get all books as Flow (observes local DB)
     */
    fun getAllBooks(): Flow<List<BookEntity>> = bookDao.getAllBooks()

    /**
     * Refresh books from API
     */
    suspend fun refreshBooks(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val api = BookStackApiClient.getService()
                ?: return@withContext Result.failure(Exception("Server not configured"))

            val response = api.getBooks()
            if (response.isSuccessful) {
                val books = response.body()?.data?.map { book ->
                    BookEntity(
                        id = book.id,
                        name = book.name,
                        slug = book.slug,
                        description = book.description,
                        createdAt = book.created_at,
                        updatedAt = book.updated_at
                    )
                } ?: emptyList()

                bookDao.insertBooks(books)
                Result.success(Unit)
            } else {
                Result.failure(Exception("API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get single book
     */
    suspend fun getBook(id: Int): BookEntity? = bookDao.getBook(id)
}
