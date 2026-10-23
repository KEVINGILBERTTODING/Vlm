package com.vlm.features.player.presentation.contract

data class PlayerState(
    val url: String = "",
    val isLoading: Boolean = true
)

sealed interface PlayerIntent {
    data class LoadUrl(val url: String) : PlayerIntent
    object BackClicked : PlayerIntent
}

sealed interface PlayerEffect {
    object NavigateBack : PlayerEffect
}
