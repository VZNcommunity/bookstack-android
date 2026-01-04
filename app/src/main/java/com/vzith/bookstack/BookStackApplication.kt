package com.vzith.bookstack

import android.app.Application
import com.vzith.bookstack.util.KeystoreManager

/**
 * BookStack Android App - Application Class (2026-01-05)
 */
class BookStackApplication : Application() {

    lateinit var keystoreManager: KeystoreManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        keystoreManager = KeystoreManager(this)
    }

    companion object {
        lateinit var instance: BookStackApplication
            private set
    }
}
