package com.vlm.core.data

import com.vlm.core.model.Movie

object MovieRepository {
    private var cachedMovies: List<Movie> = emptyList()

    fun setMovies(movies: List<Movie>) {
        cachedMovies = movies
    }

    fun getMovies(): List<Movie> = cachedMovies

    fun getMovieById(id: String): Movie? {
        return cachedMovies.find { it.id == id }
            ?: MockMovieRepository.getMovies().find { it.id == id }
    }
}
