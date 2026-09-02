package com.example.petling.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.petling.data.local.dao.MarketDao
import com.example.petling.data.local.dao.PriceDao
import com.example.petling.data.local.entity.MarketMedianEntity
import com.example.petling.data.local.entity.MarketProductEntity
import com.example.petling.data.local.entity.MarketStoreEntity
import com.example.petling.data.local.entity.PriceEntryEntity
import com.example.petling.data.local.entity.PriceProductEntity

@Database(
    entities = [
        PriceProductEntity::class,
        PriceEntryEntity::class,
        MarketStoreEntity::class,
        MarketProductEntity::class,
        MarketMedianEntity::class,
    ],
    version = 10,
    exportSchema = true,
)
abstract class PetlingDatabase : RoomDatabase() {
    abstract fun priceDao(): PriceDao
    abstract fun marketDao(): MarketDao

    companion object {
        const val NAME = "petling.db"
    }
}
