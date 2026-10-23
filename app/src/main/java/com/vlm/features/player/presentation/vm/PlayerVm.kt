package com.vlm.features.player.presentation.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vlm.features.player.presentation.contract.PlayerEffect
import com.vlm.features.player.presentation.contract.PlayerIntent
import com.vlm.features.player.presentation.contract.PlayerState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlayerVm : ViewModel() {
    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val _effect = Channel<PlayerEffect>()
    val effect: Flow<PlayerEffect> = _effect.receiveAsFlow()

    fun handleIntent(intent: PlayerIntent) {
        when (intent) {
            is PlayerIntent.LoadUrl -> {
                _state.update { it.copy(url = intent.url, isLoading = false) }
            }
            PlayerIntent.BackClicked -> {
                viewModelScope.launch {
                    _effect.send(PlayerEffect.NavigateBack)
                }
            }
        }
    }
}
