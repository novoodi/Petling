package com.example.petling.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.petling.data.local.entity.GrowthSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GrowthSnapshotDao {

    @Insert
    suspend fun insert(snapshot: GrowthSnapshotEntity): Long

    @Query("SELECT * FROM growth_snapshots ORDER BY achievedAt ASC, id ASC")
    fun observeAll(): Flow<List<GrowthSnapshotEntity>>

    @Query("DELETE FROM growth_snapshots")
    suspend fun clear()
}
