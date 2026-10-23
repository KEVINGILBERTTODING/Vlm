package com.vlm.features.detail.presentation.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vlm.core.data.MovieRepository
import com.vlm.core.data.StreamResolver
import com.vlm.features.detail.presentation.contract.DetailEffect
import com.vlm.features.detail.presentation.contract.DetailIntent
import com.vlm.features.detail.presentation.contract.DetailState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DetailVm : ViewModel() {
    private val _state = MutableStateFlow(DetailState())
    val state: StateFlow<DetailState> = _state.asStateFlow()

    private val _effect = Channel<DetailEffect>()
    val effect: Flow<DetailEffect> = _effect.receiveAsFlow()

    fun handleIntent(intent: DetailIntent) {
        when (intent) {
            is DetailIntent.LoadMovie -> {
                val movie = MovieRepository.getMovieById(intent.movieId)
                _state.update { it.copy(movie = movie, isLoading = false) }
            }
            DetailIntent.WatchClicked -> {
                val movie = _state.value.movie
                if (movie != null) {
                    viewModelScope.launch {
                        _state.update { it.copy(isLoading = true) }
                        val playerEmbedUrl = StreamResolver.resolveEmbedUrl(movie.watchUrl)
                        _state.update { it.copy(isLoading = false) }
                        _effect.send(DetailEffect.NavigateToPlayer(playerEmbedUrl))
                    }
                }
            }
            DetailIntent.BackClicked -> {
                viewModelScope.launch {
                    _effect.send(DetailEffect.NavigateBack)
                }
            }
        }
    }
}
