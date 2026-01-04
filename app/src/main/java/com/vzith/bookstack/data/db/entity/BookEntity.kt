package com.vzith.bookstack.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * BookStack Android App - Book Entity (2026-01-05)
 */
@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val slug: String,
    val description: String?,
    val createdAt: String,
    val updatedAt: String,
    val lastSyncedAt: Long = System.currentTimeMillis()
)
