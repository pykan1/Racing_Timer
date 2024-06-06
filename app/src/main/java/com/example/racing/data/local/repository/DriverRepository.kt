package com.example.racing.data.local.repository

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.racing.data.models.DriverModel
import com.example.racing.data.models.RaceDriverCrossRefModel

@Dao
interface DriverRepository {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDriver(driver: DriverModel): Long

    @Query("SELECT * FROM driver WHERE driverId = :id")
    suspend fun getDriverById(id: Long): DriverModel

    @Query("SELECT * FROM driver WHERE name LIKE :query OR lastName LIKE :query")
    suspend fun searchDrivers(query: String): List<DriverModel>


    @Query("SELECT * FROM driver")
    suspend fun getDrivers(): List<DriverModel>

    @Delete
    suspend fun delete(driverModel: DriverModel)

    @Query("SELECT * FROM racedrivercrossrefmodel where raceId = :raceId")
    suspend fun getDriversByRaceId(raceId: Long) : List<RaceDriverCrossRefModel>
}