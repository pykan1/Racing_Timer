package com.example.racing.screen.base

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

abstract class BaseViewModel<S : UiState>(initialVal: S) : ViewModel() {

    private val _state: MutableStateFlow<S> = MutableStateFlow(initialVal)
    val state: StateFlow<S>
        get() = _state

    fun setState(newState: S) {
        _state.tryEmit(newState)
    }

}

interface UiState