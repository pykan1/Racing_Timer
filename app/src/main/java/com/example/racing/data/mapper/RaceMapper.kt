package com.example.racing.data.mapper

import com.example.racing.data.models.CircleDriverCrossRef
import com.example.racing.data.models.CircleModel
import com.example.racing.data.models.DriverModel
import com.example.racing.data.models.RaceModel
import com.example.racing.data.models.RaceWithDriversModel
import com.example.racing.domain.models.CircleUI
import com.example.racing.domain.models.DriverCircleUI
import com.example.racing.domain.models.DriverUI
import com.example.racing.domain.models.RaceUI

fun RaceWithDriversModel.toUI(): RaceUI {
    return RaceUI(
        raceId = race.raceId,
        raceTitle = race.raceTitle,
        createRace = race.createRace,
        duration = race.duration,
        finish = race.finish,
        stackFinish = race.stackFinish.split(", ").map { it.toLongOrNull() ?: 0 }
    )
}

fun RaceModel.toUI(): RaceUI {
    val race = this
    println(race.stackFinish)
    return RaceUI(
        raceId = race.raceId,
        raceTitle = race.raceTitle,
        createRace = race.createRace,
        duration = race.duration,
        finish = race.finish,
        stackFinish = stackFinish.split(", ").map { it.toLongOrNull() ?: 0 }
    )
}

//fun CircleWithDrivers.toUI(): CircleUI {
//    println(drivers)
//    return CircleUI(
//        circleId = circle.circleId,
//        raceId = circle.raceId,
//        isPenalty = circle.isPenalty,
//        drivers = emptyList()
//    )
//}

fun DriverModel.toUI(): DriverUI {
    return DriverUI(
        driverId, driverNumber, name, lastName
    )
}

fun CircleModel.toUI(
    driversCircle: List<CircleDriverCrossRef>,
    drivers: List<DriverModel>
): CircleUI {
    return CircleUI(
        circleId = circleId,
        raceId = raceId,
        isPenalty = isPenalty,
        drivers = driversCircle.map { it.toUI(drivers) },
        finishPenaltyDrivers = finishPenaltyDrivers.split(" ").map { it.toLongOrNull() ?: -1 },
    )
}

fun CircleDriverCrossRef.toUI(drivers: List<DriverModel>): DriverCircleUI {
    val driver = drivers.find { it.driverId == driverId }
    return DriverCircleUI(
        driverId = driverId,
        driverNumber = driver?.driverNumber ?: 0,
        name = driver?.name.orEmpty(),
        lastName = driver?.lastName.orEmpty(),
        duration = duration,
        useDuration = useDuration
    )
}


fun DriverUI.toDriverCircleUI(duration: Long = 0, useDuration: Boolean= true): DriverCircleUI {
    return DriverCircleUI(
        driverId = driverId,
        driverNumber = driverNumber,
        name = name,
        lastName = lastName,
        duration = duration,
        useDuration = useDuration
    )
}