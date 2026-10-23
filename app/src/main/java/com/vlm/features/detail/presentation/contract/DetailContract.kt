package com.vlm.features.detail.presentation.contract

import com.vlm.core.model.Movie

data class DetailState(
    val movie: Movie? = null,
    val isLoading: Boolean = true
)

sealed interface DetailIntent {
    data class LoadMovie(val movieId: String) : DetailIntent
    object WatchClicked : DetailIntent
    object BackClicked : DetailIntent
}

sealed interface DetailEffect {
    data class NavigateToPlayer(val watchUrl: String) : DetailEffect
    object NavigateBack : DetailEffect
}
