package com.vzith.bookstack.data.api

import com.vzith.bookstack.BookStackApplication
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * BookStack Android App - API Client (2026-01-05)
 *
 * Retrofit client with authentication interceptor
 */
object BookStackApiClient {

    private var retrofit: Retrofit? = null
    private var apiService: BookStackApiService? = null

    /**
     * Auth interceptor that adds "Authorization: Token {id}:{secret}" header
     */
    private val authInterceptor = Interceptor { chain ->
        val keystoreManager = BookStackApplication.instance.keystoreManager
        val authHeader = keystoreManager.getAuthHeader()

        val request = if (authHeader != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", authHeader)
                .addHeader("Content-Type", "application/json")
                .build()
        } else {
            chain.request()
        }

        chain.proceed(request)
    }

    /**
     * Get or create the API service
     */
    fun getService(): BookStackApiService? {
        val keystoreManager = BookStackApplication.instance.keystoreManager
        val serverUrl = keystoreManager.getServerUrl() ?: return null

        // Rebuild if URL changed
        if (retrofit?.baseUrl()?.toString()?.trimEnd('/') != serverUrl.trimEnd('/')) {
            rebuild(serverUrl)
        }

        return apiService
    }

    /**
     * Force rebuild the client with new URL
     */
    fun rebuild(baseUrl: String) {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val url = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        retrofit = Retrofit.Builder()
            .baseUrl(url)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit?.create(BookStackApiService::class.java)
    }

    /**
     * Clear cached client (call when credentials change)
     */
    fun clear() {
        retrofit = null
        apiService = null
    }
}
