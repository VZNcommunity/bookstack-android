package com.vzith.bookstack.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * BookStack Android App - Page Entity (2026-01-05)
 */
@Entity(tableName = "pages")
data class PageEntity(
    @PrimaryKey val id: Int,
    val bookId: Int,
    val chapterId: Int?,
    val name: String,
    val slug: String,
    val html: String,
    val markdown: String?,
    val priority: Int,
    val draft: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val lastSyncedAt: Long = System.currentTimeMillis(),
    val locallyModified: Boolean = false
)
