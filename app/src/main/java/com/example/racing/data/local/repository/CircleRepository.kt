package com.example.racing.data.local.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.racing.data.models.CircleDriverCrossRef
import com.example.racing.data.models.CircleModel
import com.example.racing.data.models.CircleWithDrivers

@Dao
interface CircleRepository {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLap(circleModel: CircleModel): Long

    @Query("SELECT * FROM circle WHERE raceId = :raceId")
    suspend fun getLapsForRace(raceId: Long): List<CircleModel>

    @Query("SELECT * FROM circleDriverCrossRef WHERE circleId = :circleId")
    suspend fun getLapsWithDriversForRace(circleId: Long): List<CircleDriverCrossRef>

    @Query("SELECT * FROM circle WHERE circleId = :lapId")
    suspend fun getLapById(lapId: Long): CircleModel

    @Transaction
    @Query("SELECT * FROM circle WHERE circleId = :lapId")
    suspend fun getCircleWithDriversById(lapId: Long): CircleWithDrivers

    @Insert
    suspend fun insertCircleDriver(circleDriverCrossRef: CircleDriverCrossRef)
}