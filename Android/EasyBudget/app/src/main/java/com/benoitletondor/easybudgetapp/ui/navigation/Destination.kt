/*
 *   Copyright 2024 Benoit LETONDOR
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.benoitletondor.easybudgetapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

sealed interface Destination<Args> {
    val path: String

    @Composable fun Content(
        navController: NavController,
        backStackEntry: NavBackStackEntry,
    )

    fun navigate(navController: NavController, args: Args)
}

fun NavGraphBuilder.buildNavGraph(navController: NavController) {
    Destination::class.sealedSubclasses.forEach { routeClass ->
        val route = routeClass.objectInstance as Destination
        composable(
            route = route.path,
            content = {
                route.Content(navController, it)
            },
        )
    }
}

fun NavController.navigate(destination: Destination<Unit>) {
    destination.navigate(this, Unit)
}

fun <Args> NavController.navigate(destination: Destination<Args>, args: Args) {
    destination.navigate(this, args)
}


