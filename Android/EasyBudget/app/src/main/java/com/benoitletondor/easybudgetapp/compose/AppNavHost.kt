package com.benoitletondor.easybudgetapp.compose

import android.os.Build
import android.os.Bundle
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.benoitletondor.easybudgetapp.view.main.MainDestination
import com.benoitletondor.easybudgetapp.view.main.MainView
import com.benoitletondor.easybudgetapp.view.main.MainViewModel
import com.benoitletondor.easybudgetapp.view.manageaccount.ManageAccountDestination
import com.benoitletondor.easybudgetapp.view.manageaccount.ManageAccountView
import com.benoitletondor.easybudgetapp.view.manageaccount.ManageAccountViewModelFactory
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.reflect.typeOf

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
                navigateToManageAccount = { account ->
                    navController.navigate(ManageAccountDestination(selectedAccount = account))
                }
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
        composable<ManageAccountDestination>(
            typeMap = mapOf(typeOf<MainViewModel.SelectedAccount.Selected.Online>() to OnlineAccountNavType),
        ) { backStackEntry ->
            val destination: ManageAccountDestination = backStackEntry.toRoute()
            ManageAccountView(
                viewModel = hiltViewModel(
                    creationCallback = { factory: ManageAccountViewModelFactory ->
                        factory.create(
                            selectedAccount = destination.selectedAccount,
                        )
                    }
                ),
                navigateUp = {
                    navController.navigateUp()
                },
                finish = {
                    navController.popBackStack()
                },
            )
        }
    }
}

private val OnlineAccountNavType = object : NavType<MainViewModel.SelectedAccount.Selected.Online>(
    isNullableAllowed = false
) {
    override fun get(bundle: Bundle, key: String): MainViewModel.SelectedAccount.Selected.Online? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bundle.getParcelable(key, MainViewModel.SelectedAccount.Selected.Online::class.java)
        } else {
            @Suppress("DEPRECATION")
            bundle.getParcelable(key)
        }
    }

    override fun parseValue(value: String): MainViewModel.SelectedAccount.Selected.Online {
        return Json.decodeFromString<MainViewModel.SelectedAccount.Selected.Online>(value)
    }

    override fun serializeAsValue(value: MainViewModel.SelectedAccount.Selected.Online): String {
        return Json.encodeToString(value)
    }

    override fun put(bundle: Bundle, key: String, value: MainViewModel.SelectedAccount.Selected.Online) {
        bundle.putParcelable(key, value)
    }
}