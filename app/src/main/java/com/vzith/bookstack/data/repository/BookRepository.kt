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
 * Updated: 2026-01-11 - Added pagination support
 *
 * Offline-first repository with API sync
 */
class BookRepository {

    companion object {
        private const val PAGE_SIZE = 50
    }

    private val database = BookStackDatabase.getDatabase(BookStackApplication.instance)
    private val bookDao = database.bookDao()

    /**
     * Get all books as Flow (observes local DB)
     */
    fun getAllBooks(): Flow<List<BookEntity>> = bookDao.getAllBooks()

    /**
     * Refresh books from API with pagination support (2026-01-11)
     */
    suspend fun refreshBooks(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val api = BookStackApiClient.getService()
                ?: return@withContext Result.failure(Exception("Server not configured"))

            var offset = 0
            var hasMore = true
            val allBooks = mutableListOf<BookEntity>()

            // Fetch all pages
            while (hasMore) {
                val response = api.getBooks(offset = offset, count = PAGE_SIZE)
                if (response.isSuccessful) {
                    val body = response.body()
                    val books = body?.data?.map { book ->
                        BookEntity(
                            id = book.id,
                            name = book.name,
                            slug = book.slug,
                            description = book.description,
                            createdAt = book.created_at,
                            updatedAt = book.updated_at
                        )
                    } ?: emptyList()

                    allBooks.addAll(books)

                    // Check if there are more pages
                    val total = body?.total ?: 0
                    offset += PAGE_SIZE
                    hasMore = offset < total
                } else {
                    return@withContext Result.failure(Exception("API error: ${response.code()}"))
                }
            }

            // Insert all books at once for better performance
            if (allBooks.isNotEmpty()) {
                bookDao.insertBooks(allBooks)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get single book
     */
    suspend fun getBook(id: Int): BookEntity? = bookDao.getBook(id)

    /**
     * Search across all content (2026-01-11)
     */
    suspend fun search(query: String): Result<List<com.vzith.bookstack.data.api.SearchResult>> =
        withContext(Dispatchers.IO) {
            try {
                val api = BookStackApiClient.getService()
                    ?: return@withContext Result.failure(Exception("Server not configured"))

                val response = api.search(query)
                if (response.isSuccessful) {
                    Result.success(response.body()?.data ?: emptyList())
                } else {
                    Result.failure(Exception("Search failed: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
