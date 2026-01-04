package com.vzith.bookstack.data.db.dao

import androidx.room.*
import com.vzith.bookstack.data.db.entity.SyncQueueEntity

/**
 * BookStack Android App - Sync Queue DAO (2026-01-05)
 */
@Dao
interface SyncQueueDao {

    @Query("SELECT * FROM sync_queue WHERE pageId = :pageId ORDER BY createdAt ASC")
    suspend fun getQueueForPage(pageId: Int): List<SyncQueueEntity>

    @Query("SELECT * FROM sync_queue ORDER BY createdAt ASC")
    suspend fun getAllQueued(): List<SyncQueueEntity>

    @Insert
    suspend fun insert(entry: SyncQueueEntity)

    @Delete
    suspend fun delete(entry: SyncQueueEntity)

    @Query("DELETE FROM sync_queue WHERE pageId = :pageId")
    suspend fun deleteForPage(pageId: Int)

    @Query("UPDATE sync_queue SET retryCount = retryCount + 1 WHERE id = :id")
    suspend fun incrementRetry(id: Long)

    @Query("DELETE FROM sync_queue WHERE retryCount > :maxRetries")
    suspend fun deleteFailedEntries(maxRetries: Int = 5)
}
