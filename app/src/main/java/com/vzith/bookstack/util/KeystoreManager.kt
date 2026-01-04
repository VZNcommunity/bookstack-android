package com.vzith.bookstack.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * BookStack Android App - Keystore Manager (2026-01-05)
 *
 * Secure storage for sensitive data using Android Keystore:
 * - BookStack API tokens (tokenId:tokenSecret)
 * - Server URL configuration
 * - Encrypted SharedPreferences
 */
class KeystoreManager(context: Context) {

    companion object {
        private const val PREFS_FILE_NAME = "bookstack_secure_prefs"
        private const val KEY_TOKEN_ID = "bookstack_token_id"
        private const val KEY_TOKEN_SECRET = "bookstack_token_secret"
        private const val KEY_SERVER_URL = "bookstack_server_url"
        private const val KEY_SYNC_SERVER_URL = "sync_server_url"
    }

    private val masterKey: MasterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // BookStack API Token Storage

    /**
     * Save BookStack API token pair
     */
    fun saveToken(tokenId: String, tokenSecret: String) {
        encryptedPrefs.edit()
            .putString(KEY_TOKEN_ID, tokenId)
            .putString(KEY_TOKEN_SECRET, tokenSecret)
            .apply()
    }

    /**
     * Get BookStack token ID
     */
    fun getTokenId(): String? {
        return encryptedPrefs.getString(KEY_TOKEN_ID, null)
    }

    /**
     * Get BookStack token secret
     */
    fun getTokenSecret(): String? {
        return encryptedPrefs.getString(KEY_TOKEN_SECRET, null)
    }

    /**
     * Get formatted auth header value: "Token {tokenId}:{tokenSecret}"
     */
    fun getAuthHeader(): String? {
        val id = getTokenId()
        val secret = getTokenSecret()
        return if (id != null && secret != null) {
            "Token $id:$secret"
        } else null
    }

    /**
     * Check if token is configured
     */
    fun hasToken(): Boolean {
        return getTokenId() != null && getTokenSecret() != null
    }

    /**
     * Clear token
     */
    fun clearToken() {
        encryptedPrefs.edit()
            .remove(KEY_TOKEN_ID)
            .remove(KEY_TOKEN_SECRET)
            .apply()
    }

    // Server URL Storage

    /**
     * Save BookStack server URL (e.g., "http://100.78.187.47:8080")
     */
    fun saveServerUrl(url: String) {
        encryptedPrefs.edit().putString(KEY_SERVER_URL, url).apply()
    }

    /**
     * Get BookStack server URL
     */
    fun getServerUrl(): String? {
        return encryptedPrefs.getString(KEY_SERVER_URL, null)
    }

    /**
     * Save sync server URL (e.g., "ws://100.78.187.47:3032")
     */
    fun saveSyncServerUrl(url: String) {
        encryptedPrefs.edit().putString(KEY_SYNC_SERVER_URL, url).apply()
    }

    /**
     * Get sync server URL
     */
    fun getSyncServerUrl(): String? {
        return encryptedPrefs.getString(KEY_SYNC_SERVER_URL, null)
    }

    /**
     * Check if server is configured
     */
    fun isConfigured(): Boolean {
        return hasToken() && getServerUrl() != null
    }

    /**
     * Clear all stored data
     */
    fun clearAll() {
        encryptedPrefs.edit().clear().apply()
    }
}
