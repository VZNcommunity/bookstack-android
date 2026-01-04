package com.vzith.bookstack.data.db.dao

import androidx.room.*
import com.vzith.bookstack.data.db.entity.YDocStateEntity

/**
 * BookStack Android App - Y.Doc State DAO (2026-01-05)
 */
@Dao
interface YDocStateDao {

    @Query("SELECT * FROM ydoc_state WHERE pageId = :pageId")
    suspend fun getState(pageId: Int): YDocStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(state: YDocStateEntity)

    @Query("DELETE FROM ydoc_state WHERE pageId = :pageId")
    suspend fun deleteState(pageId: Int)

    @Query("DELETE FROM ydoc_state")
    suspend fun deleteAll()
}
