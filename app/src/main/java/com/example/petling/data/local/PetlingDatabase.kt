package com.example.petling.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.petling.data.local.dao.PriceDao
import com.example.petling.data.local.entity.PriceEntryEntity
import com.example.petling.data.local.entity.PriceProductEntity

@Database(
    entities = [
        PriceProductEntity::class,
        PriceEntryEntity::class,
    ],
    version = 9,
    exportSchema = true,
)
abstract class PetlingDatabase : RoomDatabase() {
    abstract fun priceDao(): PriceDao

    companion object {
        const val NAME = "petling.db"
    }
}
