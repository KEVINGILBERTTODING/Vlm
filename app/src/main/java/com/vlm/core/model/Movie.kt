package com.vlm.core.model

data class Movie(
    val id: String,
    val title: String,
    val overview: String,
    val posterUrl: String,
    val backdropUrl: String,
    val releaseYear: String,
    val rating: String,
    val genre: String,
    val watchUrl: String
)
