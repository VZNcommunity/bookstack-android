package com.vzith.bookstack

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.vzith.bookstack.util.KeystoreManager
import com.vzith.bookstack.util.NetworkMonitor
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * BookStack Android App - Application Class (2026-01-05)
 * Updated: 2026-01-11 - Added NetworkMonitor, Coil image caching
 */
class BookStackApplication : Application(), ImageLoaderFactory {

    lateinit var keystoreManager: KeystoreManager
        private set

    lateinit var networkMonitor: NetworkMonitor
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        keystoreManager = KeystoreManager(this)
        networkMonitor = NetworkMonitor(this)
    }

    /**
     * Configure Coil ImageLoader with disk and memory caching (2026-01-11)
     */
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // Use 25% of available memory
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100 * 1024 * 1024) // 100 MB disk cache
                    .build()
            }
            .okHttpClient {
                OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .build()
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(true)
            .build()
    }

    companion object {
        lateinit var instance: BookStackApplication
            private set
    }
}
