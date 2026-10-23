package com.vlm.core.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Detail : Screen("detail/{movieId}") {
        fun createRoute(movieId: String) = "detail/$movieId"
    }
    object Player : Screen("player/{movieUrl}") {
        fun createRoute(movieUrl: String) = "player/${java.net.URLEncoder.encode(movieUrl, "UTF-8")}"
    }
}
