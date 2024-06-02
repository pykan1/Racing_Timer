package com.example.racing.data.local.repository

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.racing.data.models.RaceModel
import com.example.racing.data.models.RaceWithDriversModel

@Dao
interface RaceRepository {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRace(raceModel: RaceModel): Long

    @Query("SELECT * FROM race WHERE raceId = :id")
    suspend fun getRaceWithDriversById(id: Long): RaceWithDriversModel

    @Query("SELECT * FROM race WHERE raceId = :id")
    suspend fun getRaceById(id: Long): RaceModel

    @Query("Select * from race")
    suspend fun getRaces(): List<RaceModel>

    @Update
    suspend fun updateRace(raceModel: RaceModel)

    @Delete
    suspend fun deleteRace(raceModel: RaceModel)

}