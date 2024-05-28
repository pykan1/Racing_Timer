package com.example.racing.domain.models


data class RaceUI(
    val raceId: Long = 1,
    val raceTitle: String,
    val createRace: Long,
    val duration: Long,
    val finish: Boolean
) {
    companion object {
        val Default = RaceUI(
            raceId = 0,
            raceTitle = "",
            createRace = 0,
            duration = 0,
            finish = false
        )
    }
}