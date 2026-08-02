package com.dolo.dolo.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dolo.core.engine.EngineInitState
import com.dolo.core.engine.EngineInitializer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val engineInitializer: EngineInitializer
) : ViewModel() {

    val initState: StateFlow<EngineInitState> = engineInitializer.initState

    fun initializeEngine() {
        viewModelScope.launch {
            engineInitializer.initialize()
        }
    }
}
