package com.example.petling.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.petling.data.local.entity.PriceEntryEntity
import com.example.petling.data.local.entity.PriceProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PriceDao {

    @Insert
    suspend fun insertProduct(product: PriceProductEntity): Long

    @Update
    suspend fun updateProduct(product: PriceProductEntity)

    @Insert
    suspend fun insertEntry(entry: PriceEntryEntity): Long

    @Query("SELECT * FROM price_products ORDER BY createdAt DESC")
    fun observeProducts(): Flow<List<PriceProductEntity>>

    @Query("SELECT * FROM price_entries ORDER BY createdAt DESC")
    fun observeAllEntries(): Flow<List<PriceEntryEntity>>

    @Query("SELECT * FROM price_entries WHERE productId = :productId ORDER BY createdAt DESC")
    fun observeEntries(productId: Long): Flow<List<PriceEntryEntity>>

    @Query("SELECT * FROM price_products WHERE id = :id")
    suspend fun getProduct(id: Long): PriceProductEntity?

    @Query("SELECT * FROM price_products WHERE id = :id")
    fun observeProduct(id: Long): Flow<PriceProductEntity?>

    @Query("SELECT * FROM price_products WHERE barcode = :barcode LIMIT 1")
    suspend fun findByBarcode(barcode: String): PriceProductEntity?

    @Query("SELECT * FROM price_products WHERE normalizedName = :normalizedName LIMIT 1")
    suspend fun findByNormalizedName(normalizedName: String): PriceProductEntity?

    @Query("SELECT * FROM price_entries WHERE productId = :productId ORDER BY createdAt DESC LIMIT 1")
    suspend fun latestEntry(productId: Long): PriceEntryEntity?

    @Query("DELETE FROM price_entries WHERE id = :entryId")
    suspend fun deleteEntry(entryId: Long)

    @Query("DELETE FROM price_entries WHERE productId = :productId")
    suspend fun deleteEntriesFor(productId: Long)

    @Query("DELETE FROM price_products WHERE id = :productId")
    suspend fun deleteProduct(productId: Long)

    @Query("SELECT * FROM price_entries WHERE productId = :productId")
    suspend fun entriesFor(productId: Long): List<PriceEntryEntity>

    /** 최근 사용 매장(최신 기록순, 빈 값 제외) — 확인 화면 매장 칩. */
    @Query(
        "SELECT storeName FROM price_entries WHERE storeName IS NOT NULL AND storeName != '' " +
            "GROUP BY storeName ORDER BY MAX(createdAt) DESC LIMIT :limit"
    )
    suspend fun recentStoreNames(limit: Int): List<String>

    @Query("SELECT * FROM price_entries ORDER BY createdAt DESC LIMIT 1")
    suspend fun latestEntryOverall(): PriceEntryEntity?
}
