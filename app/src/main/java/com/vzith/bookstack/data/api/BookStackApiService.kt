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

    // Books
    @GET("api/books")
    suspend fun getBooks(): Response<BookListResponse>

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
