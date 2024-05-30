package com.example.racing.screen.raceTable

import androidx.lifecycle.viewModelScope
import com.example.racing.data.local.repositoryImpl.RaceRepositoryImpl
import com.example.racing.screen.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class RaceTableViewModel @Inject constructor(private val raceRepositoryImpl: RaceRepositoryImpl) :
    BaseViewModel<RaceTableState>(RaceTableState.InitState) {

    fun loadRace(id: Long) {
        viewModelScope.launch {
            val raceDetail = raceRepositoryImpl.getRaceDetail(id)
            setState(state.value.copy(
                raceDetailUI = raceDetail
            ))
        }
    }

}