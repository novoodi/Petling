package com.example.petling.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.petling.data.local.entity.CaptureEntity
import kotlinx.coroutines.flow.Flow

data class CaptureKeyCount(val categoryKey: String, val cnt: Int)

@Dao
interface CaptureDao {

    @Insert
    suspend fun insert(capture: CaptureEntity): Long

    @Update
    suspend fun update(capture: CaptureEntity)

    @Query("SELECT * FROM captures ORDER BY createdAt DESC, id DESC")
    fun observeAll(): Flow<List<CaptureEntity>>

    @Query("SELECT * FROM captures WHERE categoryKey = :key ORDER BY createdAt DESC, id DESC")
    fun observeByKey(key: String): Flow<List<CaptureEntity>>

    @Query(
        """
        SELECT * FROM captures
        WHERE ocrText LIKE '%' || :q || '%' OR title LIKE '%' || :q || '%'
        ORDER BY createdAt DESC, id DESC
        """,
    )
    fun search(q: String): Flow<List<CaptureEntity>>

    @Query("SELECT * FROM captures WHERE id = :id")
    fun observeById(id: Long): Flow<CaptureEntity?>

    @Query("SELECT * FROM captures WHERE id = :id")
    suspend fun getById(id: Long): CaptureEntity?

    @Delete
    suspend fun delete(capture: CaptureEntity)

    /** 지금까지 캡처한 서로 다른 카테고리 수(다양성 보너스 판정용). */
    @Query("SELECT COUNT(DISTINCT categoryKey) FROM captures")
    suspend fun distinctKeyCount(): Int

    @Query("SELECT categoryKey AS categoryKey, COUNT(*) AS cnt FROM captures GROUP BY categoryKey")
    suspend fun countByKey(): List<CaptureKeyCount>

    @Query("SELECT COUNT(*) FROM captures")
    suspend fun count(): Int

    @Query("DELETE FROM captures")
    suspend fun clear()
}
