package com.example.racing.data.local.repository

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.example.racing.data.models.RaceDriverCrossRefModel

@Dao
interface RaceDriverCrossRefRepository {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRef(crossRef: RaceDriverCrossRefModel): Long
    @Delete()
    suspend fun deleteCrossRef(crossRef: RaceDriverCrossRefModel)
}