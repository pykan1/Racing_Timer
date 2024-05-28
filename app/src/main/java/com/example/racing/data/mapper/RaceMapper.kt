package com.example.racing.data.mapper

import com.example.racing.data.models.CircleWithDrivers
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
        finish = race.finish
    )
}

fun RaceModel.toUI(): RaceUI {
    val race = this
    return RaceUI(
        raceId = race.raceId,
        raceTitle = race.raceTitle,
        createRace = race.createRace,
        duration = race.duration,
        finish = race.finish
    )
}

fun CircleWithDrivers.toUI(): CircleUI {
    return CircleUI(
        circleId = circle.circleId,
        raceId = circle.raceId,
        isPenalty = circle.isPenalty,
        drivers = drivers.map { it.toUI().toDriverCircleUI() }
    )
}

fun DriverModel.toUI(): DriverUI {
    return DriverUI(
        driverId, name, lastName
    )
}

fun DriverUI.toDriverCircleUI(duration: Long = 0): DriverCircleUI {
    return DriverCircleUI(driverId = driverId, name = name, lastName = lastName, duration = duration)
}