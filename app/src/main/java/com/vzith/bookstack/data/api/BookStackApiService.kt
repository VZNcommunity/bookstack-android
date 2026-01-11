package com.vzith.bookstack.data.api

import retrofit2.Response
import retrofit2.http.*

/**
 * BookStack Android App - API Service (2026-01-05)
 *
 * BookStack REST API client using Retrofit
 * API docs: https://demo.bookstackapp.com/api/docs
 */
interface BookStackApiService {

    // Books (2026-01-11: Added pagination support)
    @GET("api/books")
    suspend fun getBooks(
        @Query("offset") offset: Int = 0,
        @Query("count") count: Int = 100
    ): Response<BookListResponse>

    @GET("api/books/{id}")
    suspend fun getBook(@Path("id") id: Int): Response<BookDetailResponse>

    // Chapters
    @GET("api/chapters")
    suspend fun getChapters(): Response<ChapterListResponse>

    @GET("api/chapters/{id}")
    suspend fun getChapter(@Path("id") id: Int): Response<ChapterDetailResponse>

    // Pages
    @GET("api/pages")
    suspend fun getPages(): Response<PageListResponse>

    @GET("api/pages/{id}")
    suspend fun getPage(@Path("id") id: Int): Response<PageDetailResponse>

    @PUT("api/pages/{id}")
    suspend fun updatePage(
        @Path("id") id: Int,
        @Body body: PageUpdateRequest
    ): Response<PageDetailResponse>

    // Search (2026-01-11)
    @GET("api/search")
    suspend fun search(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("count") count: Int = 20
    ): Response<SearchResponse>

    // Page CRUD (2026-01-11)
    @POST("api/pages")
    suspend fun createPage(@Body body: CreatePageRequest): Response<PageDetailResponse>

    @DELETE("api/pages/{id}")
    suspend fun deletePage(@Path("id") id: Int): Response<Unit>

    // Page Revisions (2026-01-11)
    @GET("api/pages/{id}/revisions")
    suspend fun getPageRevisions(@Path("id") pageId: Int): Response<RevisionListResponse>

    @GET("api/pages/{pageId}/revisions/{revisionId}")
    suspend fun getRevision(
        @Path("pageId") pageId: Int,
        @Path("revisionId") revisionId: Int
    ): Response<RevisionDetailResponse>

    // Page Export (2026-01-11)
    @GET("api/pages/{id}/export/html")
    suspend fun exportPageHtml(@Path("id") id: Int): Response<String>

    @GET("api/pages/{id}/export/pdf")
    suspend fun exportPagePdf(@Path("id") id: Int): Response<okhttp3.ResponseBody>

    @GET("api/pages/{id}/export/markdown")
    suspend fun exportPageMarkdown(@Path("id") id: Int): Response<String>

    @GET("api/pages/{id}/export/plaintext")
    suspend fun exportPagePlaintext(@Path("id") id: Int): Response<String>
}

// Data classes for API responses

data class BookListResponse(
    val data: List<BookSummary>,
    val total: Int
)

data class BookSummary(
    val id: Int,
    val name: String,
    val slug: String,
    val description: String?,
    val created_at: String,
    val updated_at: String
)

data class BookDetailResponse(
    val id: Int,
    val name: String,
    val slug: String,
    val description: String?,
    val contents: List<ContentItem>,
    val created_at: String,
    val updated_at: String
)

data class ContentItem(
    val id: Int,
    val name: String,
    val slug: String,
    val type: String, // "chapter" or "page"
    val pages: List<PageSummary>? = null // Only for chapters
)

data class ChapterListResponse(
    val data: List<ChapterSummary>,
    val total: Int
)

data class ChapterSummary(
    val id: Int,
    val book_id: Int,
    val name: String,
    val slug: String,
    val description: String?,
    val priority: Int,
    val created_at: String,
    val updated_at: String
)

data class ChapterDetailResponse(
    val id: Int,
    val book_id: Int,
    val name: String,
    val slug: String,
    val description: String?,
    val pages: List<PageSummary>,
    val created_at: String,
    val updated_at: String
)

data class PageListResponse(
    val data: List<PageSummary>,
    val total: Int
)

data class PageSummary(
    val id: Int,
    val book_id: Int,
    val chapter_id: Int?,
    val name: String,
    val slug: String,
    val priority: Int,
    val draft: Boolean,
    val created_at: String,
    val updated_at: String
)

data class PageDetailResponse(
    val id: Int,
    val book_id: Int,
    val chapter_id: Int?,
    val name: String,
    val slug: String,
    val html: String,
    val markdown: String?,
    val priority: Int,
    val draft: Boolean,
    val created_at: String,
    val updated_at: String
)

data class PageUpdateRequest(
    val name: String? = null,
    val html: String? = null,
    val markdown: String? = null
)

// Search API (2026-01-11)
data class SearchResponse(
    val data: List<SearchResult>,
    val total: Int
)

data class SearchResult(
    val id: Int,
    val name: String,
    val slug: String,
    val type: String, // "page", "chapter", "book", "bookshelf"
    val url: String,
    val preview_html: SearchPreview?,
    val book_id: Int? = null,
    val chapter_id: Int? = null
)

data class SearchPreview(
    val name: String?,
    val content: String?
)

// Page creation (2026-01-11)
data class CreatePageRequest(
    val book_id: Int? = null,
    val chapter_id: Int? = null,
    val name: String,
    val html: String? = null,
    val markdown: String? = null
)

// Page Revisions (2026-01-11)
data class RevisionListResponse(
    val data: List<RevisionSummary>,
    val total: Int
)

data class RevisionSummary(
    val id: Int,
    val page_id: Int,
    val name: String,
    val created_by: UserSummary?,
    val revision_number: Int,
    val created_at: String,
    val updated_at: String,
    val summary: String?
)

data class RevisionDetailResponse(
    val id: Int,
    val page_id: Int,
    val name: String,
    val html: String,
    val created_by: UserSummary?,
    val revision_number: Int,
    val created_at: String,
    val updated_at: String
)

data class UserSummary(
    val id: Int,
    val name: String
)
