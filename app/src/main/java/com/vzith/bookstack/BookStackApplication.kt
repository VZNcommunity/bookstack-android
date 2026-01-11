package com.vzith.bookstack

import android.app.Application
import com.vzith.bookstack.util.KeystoreManager
import com.vzith.bookstack.util.NetworkMonitor

/**
 * BookStack Android App - Application Class (2026-01-05)
 * Updated: 2026-01-11 - Added NetworkMonitor
 */
class BookStackApplication : Application() {

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

    companion object {
        lateinit var instance: BookStackApplication
            private set
    }
}
