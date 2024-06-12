package com.benoitletondor.easybudgetapp.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.benoitletondor.easybudgetapp.view.main.MainDestination
import com.benoitletondor.easybudgetapp.view.main.MainView
import com.benoitletondor.easybudgetapp.view.onboarding.OnboardingDestination
import com.benoitletondor.easybudgetapp.view.onboarding.OnboardingResult
import com.benoitletondor.easybudgetapp.view.onboarding.OnboardingView
import com.benoitletondor.easybudgetapp.view.premium.PremiumDestination
import com.benoitletondor.easybudgetapp.view.premium.PremiumView
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onEach

private const val OnboardingResultKey = "OnboardingResult"

@Composable
fun AppNavHost(
    closeApp: () -> Unit,
) {
    val navController = rememberNavController()

    NavHost(
        modifier = Modifier.fillMaxSize(),
        navController = navController,
        startDestination = MainDestination,
    ) {
        composable<MainDestination> { navBackStackEntry ->
            val onboardingResultFlow = remember(navBackStackEntry) {
                navBackStackEntry.savedStateHandle.getStateFlow<OnboardingResult?>(OnboardingResultKey, null)
                    .filterNotNull()
                    .onEach {
                        navBackStackEntry.savedStateHandle.remove<OnboardingResult>(OnboardingResultKey)
                    }
            }

            MainView(
                navigateToOnboarding = {
                    navController.navigate(OnboardingDestination)
                },
                onboardingResultFlow = onboardingResultFlow,
                closeApp = closeApp,
                navigateToPremium = { startOnPro ->
                    navController.navigate(PremiumDestination(startOnPro = startOnPro))
                },
            )
        }
        composable<OnboardingDestination>(
            popEnterTransition = null,
            enterTransition = null,
            popExitTransition = null,
        ) {
            OnboardingView(
                finishWithResult = { result ->
                    navController.previousBackStackEntry?.savedStateHandle?.set(OnboardingResultKey, result)
                    navController.navigateUp()
                }
            )
        }
        composable<PremiumDestination> { backStackEntry ->
            val destination: PremiumDestination = backStackEntry.toRoute()
            PremiumView(
                startOnPro = destination.startOnPro,
                close = {
                    navController.navigateUp()
                }
            )
        }
    }
}