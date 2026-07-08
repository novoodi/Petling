package com.example.petling.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.petling.data.local.dao.CaptureDao
import com.example.petling.data.local.dao.CategoryDao
import com.example.petling.data.local.dao.CharacterDao
import com.example.petling.data.local.dao.GrowthSnapshotDao
import com.example.petling.data.local.dao.ScheduleDao
import com.example.petling.data.local.dao.XpLogDao
import com.example.petling.data.local.entity.CaptureEntity
import com.example.petling.data.local.entity.CategoryEntity
import com.example.petling.data.local.entity.CharacterStateEntity
import com.example.petling.data.local.entity.GrowthSnapshotEntity
import com.example.petling.data.local.entity.ScheduleEntity
import com.example.petling.data.local.entity.XpLogEntity

@Database(
    entities = [
        ScheduleEntity::class,
        CharacterStateEntity::class,
        XpLogEntity::class,
        GrowthSnapshotEntity::class,
        CaptureEntity::class,
        CategoryEntity::class,
    ],
    version = 6,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class PetlingDatabase : RoomDatabase() {
    abstract fun scheduleDao(): ScheduleDao
    abstract fun characterDao(): CharacterDao
    abstract fun xpLogDao(): XpLogDao
    abstract fun growthSnapshotDao(): GrowthSnapshotDao
    abstract fun captureDao(): CaptureDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        const val NAME = "petling.db"
    }
}
