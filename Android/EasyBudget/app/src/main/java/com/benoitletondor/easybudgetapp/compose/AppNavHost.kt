package com.benoitletondor.easybudgetapp.compose

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.benoitletondor.easybudgetapp.view.main.MainDestination
import com.benoitletondor.easybudgetapp.view.main.MainView

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = MainDestination) {
        composable<MainDestination> {
            MainView(
                navController = navController,
            )
        }
    }
}