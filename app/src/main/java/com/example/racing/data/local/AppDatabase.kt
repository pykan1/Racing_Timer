package com.example.racing.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.racing.data.local.repository.CircleRepository
import com.example.racing.data.local.repository.DriverRepository
import com.example.racing.data.local.repository.RaceDriverCrossRefRepository
import com.example.racing.data.local.repository.RaceRepository
import com.example.racing.data.models.CircleDriverCrossRef
import com.example.racing.data.models.CircleModel
import com.example.racing.data.models.DriverModel
import com.example.racing.data.models.RaceDriverCrossRefModel
import com.example.racing.data.models.RaceModel

@Database(
    entities = [RaceModel::class, CircleModel::class, DriverModel::class, RaceDriverCrossRefModel::class, CircleDriverCrossRef::class],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun raceDao(): RaceRepository
    abstract fun lapDao(): CircleRepository
    abstract fun driverDao(): DriverRepository
    abstract fun raceDriverCrossRefDao(): RaceDriverCrossRefRepository
}