package com.example.petling.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.petling.data.local.entity.CharacterStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterDao {

    @Query("SELECT * FROM character_state WHERE id = 0")
    fun observe(): Flow<CharacterStateEntity?>

    @Query("SELECT * FROM character_state WHERE id = 0")
    suspend fun get(): CharacterStateEntity?

    @Upsert
    suspend fun upsert(state: CharacterStateEntity)

    @Query("DELETE FROM character_state")
    suspend fun clear()
}
