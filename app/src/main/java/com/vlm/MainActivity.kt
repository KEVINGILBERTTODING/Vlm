package com.vlm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vlm.core.navigation.Screen
import com.vlm.features.detail.presentation.contract.DetailIntent
import com.vlm.features.detail.presentation.ui.DetailScreen
import com.vlm.features.detail.presentation.vm.DetailVm
import com.vlm.features.home.presentation.ui.HomeScreen
import com.vlm.features.home.presentation.vm.HomeVm
import com.vlm.features.player.presentation.contract.PlayerIntent
import com.vlm.features.player.presentation.ui.PlayerScreen
import com.vlm.features.player.presentation.vm.PlayerVm

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = MaterialTheme.colorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MovieAppNavHost()
                }
            }
        }
    }
}

@Composable
fun MovieAppNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(route = Screen.Home.route) {
            val viewModel: HomeVm = viewModel()
            val state by viewModel.state.collectAsState()
            HomeScreen(
                state = state,
                effect = viewModel.effect,
                onIntent = { viewModel.handleIntent(it) },
                onNavigateDetail = { movieId ->
                    navController.navigate(Screen.Detail.createRoute(movieId))
                }
            )
        }

        composable(
            route = Screen.Detail.route,
            arguments = listOf(navArgument("movieId") { type = NavType.StringType })
        ) { backStackEntry ->
            val movieId = backStackEntry.arguments?.getString("movieId") ?: ""
            val viewModel: DetailVm = viewModel()
            val state by viewModel.state.collectAsState()

            // Load movie details on first entry
            androidx.compose.runtime.LaunchedEffect(movieId) {
                viewModel.handleIntent(DetailIntent.LoadMovie(movieId))
            }

            DetailScreen(
                state = state,
                effect = viewModel.effect,
                onIntent = { viewModel.handleIntent(it) },
                onNavigatePlayer = { watchUrl ->
                    navController.navigate(Screen.Player.createRoute(watchUrl))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.Player.route,
            arguments = listOf(navArgument("movieUrl") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedUrl = backStackEntry.arguments?.getString("movieUrl") ?: ""
            val movieUrl = java.net.URLDecoder.decode(encodedUrl, "UTF-8")
            val viewModel: PlayerVm = viewModel()
            val state by viewModel.state.collectAsState()

            androidx.compose.runtime.LaunchedEffect(movieUrl) {
                viewModel.handleIntent(PlayerIntent.LoadUrl(movieUrl))
            }

            PlayerScreen(
                state = state,
                effect = viewModel.effect,
                onIntent = { viewModel.handleIntent(it) },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
