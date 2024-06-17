package com.benoitletondor.easybudgetapp.compose

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.benoitletondor.easybudgetapp.view.main.MainDestination
import com.benoitletondor.easybudgetapp.view.main.MainView
import com.benoitletondor.easybudgetapp.view.monthlyreport.MonthlyReportDestination
import com.benoitletondor.easybudgetapp.view.monthlyreport.MonthlyReportView
import com.benoitletondor.easybudgetapp.view.monthlyreport.MonthlyReportViewModelFactory
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
        enterTransition = { slideIntoContainer(
            AnimatedContentTransitionScope.SlideDirection.Left,
            initialOffset = { it / 2 },
        ) + fadeIn() },
        exitTransition = { slideOutOfContainer(
            AnimatedContentTransitionScope.SlideDirection.Left,
            targetOffset = { it / 2 },
        ) + fadeOut() },
        popEnterTransition = { slideIntoContainer(
            AnimatedContentTransitionScope.SlideDirection.Right,
            initialOffset = { it / 2 },
        ) + fadeIn() },
        popExitTransition = { slideOutOfContainer(
            AnimatedContentTransitionScope.SlideDirection.Right,
            targetOffset = { it / 2 },
        ) + fadeOut() },
    ) {
        composable<MainDestination>(
            enterTransition = null,
        ) { navBackStackEntry ->
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
                navigateToMonthlyReport = {
                    navController.navigate(MonthlyReportDestination(fromNotification = false))
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
                    navController.popBackStack()
                }
            )
        }
        composable<PremiumDestination> { backStackEntry ->
            val destination: PremiumDestination = backStackEntry.toRoute()
            PremiumView(
                startOnPro = destination.startOnPro,
                close = {
                    navController.popBackStack()
                }
            )
        }
        composable<MonthlyReportDestination> { backStackEntry ->
            val destination: MonthlyReportDestination = backStackEntry.toRoute()
            MonthlyReportView(
                viewModel = hiltViewModel(
                    creationCallback = { factory: MonthlyReportViewModelFactory ->
                        factory.create(
                            fromNotification = destination.fromNotification,
                        )
                    }
                ),
                navigateUp = {
                    navController.navigateUp()
                }
            )
        }
    }
}