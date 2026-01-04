package com.vzith.bookstack.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * BookStack Android App - Y.Doc State Entity (2026-01-05)
 *
 * Persists Y.js document state for offline support
 */
@Entity(tableName = "ydoc_state")
data class YDocStateEntity(
    @PrimaryKey val pageId: Int,
    val stateVector: ByteArray, // Encoded state vector
    val documentState: ByteArray, // Full document state
    val updatedAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as YDocStateEntity
        return pageId == other.pageId
    }

    override fun hashCode(): Int = pageId.hashCode()
}
