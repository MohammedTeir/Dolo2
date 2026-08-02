package com.dolo.core.engine

sealed interface EngineInitState {
    data object Idle : EngineInitState
    data class Initializing(val message: String) : EngineInitState
    data class Error(val error: String) : EngineInitState
    data object Success : EngineInitState
}
