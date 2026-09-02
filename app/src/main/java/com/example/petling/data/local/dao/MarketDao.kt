package com.example.petling.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.petling.data.local.entity.MarketMedianEntity
import com.example.petling.data.local.entity.MarketProductEntity
import com.example.petling.data.local.entity.MarketStoreEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MarketDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStores(stores: List<MarketStoreEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProducts(products: List<MarketProductEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMedians(medians: List<MarketMedianEntity>)

    @Query("DELETE FROM market_stores")
    suspend fun clearStores()

    @Query("DELETE FROM market_products")
    suspend fun clearProducts()

    @Query("SELECT * FROM market_products")
    suspend fun allProducts(): List<MarketProductEntity>

    @Query("SELECT * FROM market_products WHERE id = :id")
    suspend fun product(id: Long): MarketProductEntity?

    @Query("SELECT * FROM market_stores")
    suspend fun allStores(): List<MarketStoreEntity>

    @Query("SELECT * FROM market_medians WHERE goodId = :goodId ORDER BY day")
    suspend fun medians(goodId: Long): List<MarketMedianEntity>

    @Query("SELECT * FROM market_medians WHERE goodId = :goodId ORDER BY day")
    fun observeMedians(goodId: Long): Flow<List<MarketMedianEntity>>

    @Query("SELECT COUNT(*) FROM market_products")
    suspend fun productCount(): Int

    @Query("SELECT MAX(day) FROM market_medians")
    suspend fun latestDay(): String?

    /** 시세 검색: 이름 부분 일치(공백 제거 이름도 함께). */
    @Query(
        "SELECT * FROM market_products WHERE name LIKE '%' || :query || '%' " +
            "OR normalizedName LIKE '%' || :normalized || '%' ORDER BY name LIMIT :limit"
    )
    suspend fun searchProducts(query: String, normalized: String, limit: Int): List<MarketProductEntity>

    @Query("SELECT * FROM market_medians WHERE day = (SELECT MAX(day) FROM market_medians)")
    suspend fun latestMedians(): List<MarketMedianEntity>

    /** 매장 직접 입력 자동완성: 참가격 판매점 이름 부분 일치. */
    @Query("SELECT name FROM market_stores WHERE name LIKE '%' || :query || '%' ORDER BY name LIMIT :limit")
    suspend fun searchStoreNames(query: String, limit: Int): List<String>
}
