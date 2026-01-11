package com.vzith.bookstack.data.repository

import com.vzith.bookstack.BookStackApplication
import com.vzith.bookstack.data.api.BookStackApiClient
import com.vzith.bookstack.data.api.CreatePageRequest
import com.vzith.bookstack.data.api.PageUpdateRequest
import com.vzith.bookstack.data.db.BookStackDatabase
import com.vzith.bookstack.data.db.entity.PageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * BookStack Android App - Page Repository (2026-01-05)
 *
 * Offline-first repository with API sync
 */
class PageRepository {

    private val database = BookStackDatabase.getDatabase(BookStackApplication.instance)
    private val pageDao = database.pageDao()

    /**
     * Get pages by book as Flow
     */
    fun getPagesByBook(bookId: Int): Flow<List<PageEntity>> = pageDao.getPagesByBook(bookId)

    /**
     * Get single page
     */
    suspend fun getPage(id: Int): PageEntity? = pageDao.getPage(id)

    /**
     * Fetch page from API and cache locally
     */
    suspend fun fetchPage(id: Int): Result<PageEntity> = withContext(Dispatchers.IO) {
        try {
            val api = BookStackApiClient.getService()
                ?: return@withContext Result.failure(Exception("Server not configured"))

            val response = api.getPage(id)
            if (response.isSuccessful) {
                val page = response.body()?.let { p ->
                    PageEntity(
                        id = p.id,
                        bookId = p.book_id,
                        chapterId = p.chapter_id,
                        name = p.name,
                        slug = p.slug,
                        html = p.html,
                        markdown = p.markdown,
                        priority = p.priority,
                        draft = p.draft,
                        createdAt = p.created_at,
                        updatedAt = p.updated_at
                    )
                }

                if (page != null) {
                    pageDao.insertPage(page)
                    Result.success(page)
                } else {
                    Result.failure(Exception("Empty response"))
                }
            } else {
                Result.failure(Exception("API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update page HTML locally
     */
    suspend fun updatePageHtml(id: Int, html: String) {
        pageDao.updatePageHtml(id, html)
    }

    /**
     * Save page to API
     */
    suspend fun savePage(id: Int, html: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val api = BookStackApiClient.getService()
                ?: return@withContext Result.failure(Exception("Server not configured"))

            val response = api.updatePage(id, PageUpdateRequest(html = html))
            if (response.isSuccessful) {
                // Update local cache with server response
                response.body()?.let { p ->
                    val page = PageEntity(
                        id = p.id,
                        bookId = p.book_id,
                        chapterId = p.chapter_id,
                        name = p.name,
                        slug = p.slug,
                        html = p.html,
                        markdown = p.markdown,
                        priority = p.priority,
                        draft = p.draft,
                        createdAt = p.created_at,
                        updatedAt = p.updated_at,
                        locallyModified = false
                    )
                    pageDao.insertPage(page)
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Refresh all pages for a book
     */
    suspend fun refreshPages(bookId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val api = BookStackApiClient.getService()
                ?: return@withContext Result.failure(Exception("Server not configured"))

            val response = api.getBook(bookId)
            if (response.isSuccessful) {
                val pages = mutableListOf<PageEntity>()

                response.body()?.contents?.forEach { item ->
                    if (item.type == "page") {
                        // Fetch full page details
                        fetchPage(item.id)
                    } else if (item.type == "chapter") {
                        item.pages?.forEach { pageSummary ->
                            fetchPage(pageSummary.id)
                        }
                    }
                }

                Result.success(Unit)
            } else {
                Result.failure(Exception("API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Create a new page (2026-01-11)
     */
    suspend fun createPage(
        bookId: Int?,
        chapterId: Int?,
        name: String,
        html: String? = null,
        markdown: String? = null
    ): Result<PageEntity> = withContext(Dispatchers.IO) {
        try {
            val api = BookStackApiClient.getService()
                ?: return@withContext Result.failure(Exception("Server not configured"))

            val request = CreatePageRequest(
                book_id = bookId,
                chapter_id = chapterId,
                name = name,
                html = html,
                markdown = markdown
            )

            val response = api.createPage(request)
            if (response.isSuccessful) {
                val page = response.body()?.let { p ->
                    PageEntity(
                        id = p.id,
                        bookId = p.book_id,
                        chapterId = p.chapter_id,
                        name = p.name,
                        slug = p.slug,
                        html = p.html,
                        markdown = p.markdown,
                        priority = p.priority,
                        draft = p.draft,
                        createdAt = p.created_at,
                        updatedAt = p.updated_at
                    )
                }

                if (page != null) {
                    pageDao.insertPage(page)
                    Result.success(page)
                } else {
                    Result.failure(Exception("Empty response"))
                }
            } else {
                Result.failure(Exception("Create failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete a page (2026-01-11)
     */
    suspend fun deletePage(id: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val api = BookStackApiClient.getService()
                ?: return@withContext Result.failure(Exception("Server not configured"))

            val response = api.deletePage(id)
            if (response.isSuccessful) {
                // Remove from local cache
                pageDao.deletePageById(id)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Delete failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
