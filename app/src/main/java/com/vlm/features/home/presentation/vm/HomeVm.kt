package com.vlm.features.home.presentation.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vlm.core.data.IdFlixWebScraper
import com.vlm.core.data.MovieRepository
import com.vlm.features.home.presentation.contract.HomeEffect
import com.vlm.features.home.presentation.contract.HomeIntent
import com.vlm.features.home.presentation.contract.HomeState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeVm(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val _effect = Channel<HomeEffect>()
    val effect: Flow<HomeEffect> = _effect.receiveAsFlow()

    init {
        loadMovies()
    }

    fun handleIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.UpdateSearchQuery -> {
                _state.update { it.copy(searchQuery = intent.query) }
                filterMovies()
            }
            is HomeIntent.SelectCategory -> {
                _state.update { it.copy(selectedCategory = intent.category) }
                filterMovies()
            }
            is HomeIntent.MovieClicked -> {
                viewModelScope.launch {
                    _effect.send(HomeEffect.NavigateToDetail(intent.movie.id))
                }
            }
            HomeIntent.Refresh -> {
                loadMovies()
            }
        }
    }

    private fun loadMovies() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val movies = IdFlixWebScraper.fetchMovies(getApplication())
            MovieRepository.setMovies(movies)
            _state.update {
                it.copy(
                    movies = movies,
                    filteredMovies = movies,
                    isLoading = false
                )
            }
        }
    }

    private fun filterMovies() {
        val current = _state.value
        val query = current.searchQuery.trim().lowercase()
        val filtered = if (query.isEmpty()) {
            current.movies
        } else {
            current.movies.filter { it.title.lowercase().contains(query) || it.genre.lowercase().contains(query) }
        }
        _state.update { it.copy(filteredMovies = filtered) }
    }
}
