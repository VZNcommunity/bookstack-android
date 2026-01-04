package com.vzith.bookstack.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * BookStack Android App - Chapter Entity (2026-01-05)
 */
@Entity(tableName = "chapters")
data class ChapterEntity(
    @PrimaryKey val id: Int,
    val bookId: Int,
    val name: String,
    val slug: String,
    val description: String?,
    val priority: Int,
    val createdAt: String,
    val updatedAt: String,
    val lastSyncedAt: Long = System.currentTimeMillis()
)
