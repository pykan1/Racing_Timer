package com.example.racing.data.local.repositoryImpl

import com.example.racing.data.local.repository.CircleRepository
import com.example.racing.data.local.repository.DriverRepository
import com.example.racing.data.local.repository.RaceDriverCrossRefRepository
import com.example.racing.data.local.repository.RaceRepository
import com.example.racing.data.mapper.toUI
import com.example.racing.data.models.CircleDriverCrossRef
import com.example.racing.data.models.CircleModel
import com.example.racing.data.models.DriverModel
import com.example.racing.data.models.RaceDriverCrossRefModel
import com.example.racing.data.models.RaceModel
import com.example.racing.domain.models.CircleUI
import com.example.racing.domain.models.DriverCircleUI
import com.example.racing.domain.models.DriverUI
import com.example.racing.domain.models.RaceDetailUI
import com.example.racing.domain.models.RaceUI
import com.example.racing.ext.getCurrentTimeInMillis
import javax.inject.Inject

class RaceRepositoryImpl @Inject constructor(
    private val circleRepository: CircleRepository,
    private val driverRepository: DriverRepository,
    private val raceRepository: RaceRepository,
    private val raceDriverCrossRefRepository: RaceDriverCrossRefRepository
) {

    suspend fun getRaceDetail(raceId: Long): RaceDetailUI {
        val race = raceRepository.getRaceWithDriversById(raceId)
        val laps = circleRepository.getLapsForRace(raceId)
        val circleDrivers = mutableListOf<CircleDriverCrossRef>()
        laps.forEach {
            val driversCircle = circleRepository.getLapsWithDriversForRace(it.circleId)
            circleDrivers.addAll(driversCircle)
        }
        val drivers = driverRepository.getDrivers()
        return RaceDetailUI(
            raceUI = race.toUI(),
            circles = laps.map { circle ->
                val filterDrivers = circleDrivers.filter { circle.circleId == it.circleId }
                circle.toUI(filterDrivers, drivers)
            },
            drivers = race.drivers.map { it.toUI() }
        )
    }

    suspend fun getRaceById(id: Long): RaceUI {
        return raceRepository.getRaceWithDriversById(id).toUI()
    }

    suspend fun getRaces(): List<RaceUI> {

        return raceRepository.getRaces().map {
            it.toUI()
        }
    }

    suspend fun searchDrivers(query: String): List<DriverUI> {
        return driverRepository.searchDrivers("%$query%").map { it.toUI() }
    }

    suspend fun insertRace(title: String, drivers: List<Long>) {
        val raceId = raceRepository.insertRace(
            RaceModel(
                raceTitle = title,
                createRace = getCurrentTimeInMillis(),
                duration = 0,
                finish = false,
                stackFinish = ""
            )
        )
        drivers.forEach {
            raceDriverCrossRefRepository.insertCrossRef(
                RaceDriverCrossRefModel(
                    raceId = raceId,
                    driverId = it
                )
            )
        }
    }

    suspend fun getDrivers(): List<DriverUI> {
        return driverRepository.getDrivers().map { it.toUI() }
    }

    suspend fun createDriver(driverUI: DriverUI) {
        driverRepository.insertDriver(
            DriverModel(
                name = driverUI.name,
                lastName = driverUI.lastName,
                driverNumber = driverUI.driverNumber
            )
        )
    }

    suspend fun deleteDriver(driverUI: DriverUI) {
        driverRepository.delete(
            DriverModel(
                driverId = driverUI.driverId,
                name = driverUI.name,
                lastName = driverUI.lastName,
                driverNumber = driverUI.driverNumber
            )
        )
    }

    suspend fun saveRace(
        race: RaceUI,
        circles: List<CircleUI>,
        selectUsers: List<DriverCircleUI>,
        raceStack: List<Long>
    ) {
        raceRepository.updateRace(
            RaceModel(
                raceId = race.raceId,
                createRace = race.createRace,
                raceTitle = race.raceTitle,
                duration = race.duration,
                finish = true,
                stackFinish = raceStack.joinToString(separator = ", ")
            )
        )
        circles.forEach {
            val circleModel = CircleModel(
                raceId = race.raceId,
                isPenalty = it.isPenalty,
                finishPenaltyDrivers = it.finishPenaltyDrivers.joinToString(" "),
            )
            val circleId = circleRepository.insertLap(
                circleModel
            )
            it.drivers.forEach {
                circleRepository.insertCircleDriver(
                    CircleDriverCrossRef(
                        circleId = circleId,
                        driverId = it.driverId,
                        duration = it.duration,
                        useDuration = it.useDuration
                    )
                )
            }
        }
        selectUsers.forEach {
            raceDriverCrossRefRepository.insertCrossRef(
                RaceDriverCrossRefModel(
                    raceId = race.raceId,
                    driverId = it.driverId
                )
            )
        }

    }

}