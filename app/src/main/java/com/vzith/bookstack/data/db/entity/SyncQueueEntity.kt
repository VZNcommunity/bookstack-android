package com.vzith.bookstack.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * BookStack Android App - Sync Queue Entity (2026-01-05)
 *
 * Stores pending Y.js updates for offline sync
 */
@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pageId: Int,
    val updateData: ByteArray, // Y.js binary update
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SyncQueueEntity
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
