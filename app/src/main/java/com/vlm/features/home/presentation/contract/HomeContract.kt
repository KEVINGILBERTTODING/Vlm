package com.vlm.features.home.presentation.contract

import com.vlm.core.model.Movie

data class HomeState(
    val movies: List<Movie> = emptyList(),
    val filteredMovies: List<Movie> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String = "Trending",
    val isLoading: Boolean = false
)

sealed interface HomeIntent {
    data class UpdateSearchQuery(val query: String) : HomeIntent
    data class SelectCategory(val category: String) : HomeIntent
    data class MovieClicked(val movie: Movie) : HomeIntent
    object Refresh : HomeIntent
}

sealed interface HomeEffect {
    data class NavigateToDetail(val movieId: String) : HomeEffect
}
