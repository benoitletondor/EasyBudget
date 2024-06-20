package com.benoitletondor.easybudgetapp.compose

import android.os.Build
import android.os.Bundle
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.benoitletondor.easybudgetapp.helper.SerializedYearMonth
import com.benoitletondor.easybudgetapp.helper.launchCollect
import com.benoitletondor.easybudgetapp.helper.toSerializedYearMonth
import com.benoitletondor.easybudgetapp.view.login.LoginDestination
import com.benoitletondor.easybudgetapp.view.login.LoginView
import com.benoitletondor.easybudgetapp.view.login.LoginViewModelFactory
import com.benoitletondor.easybudgetapp.view.main.MainDestination
import com.benoitletondor.easybudgetapp.view.main.MainView
import com.benoitletondor.easybudgetapp.view.main.MainViewModel
import com.benoitletondor.easybudgetapp.view.manageaccount.ManageAccountDestination
import com.benoitletondor.easybudgetapp.view.manageaccount.ManageAccountView
import com.benoitletondor.easybudgetapp.view.manageaccount.ManageAccountViewModelFactory
import com.benoitletondor.easybudgetapp.view.monthlyreport.MonthlyReportDestination
import com.benoitletondor.easybudgetapp.view.monthlyreport.MonthlyReportView
import com.benoitletondor.easybudgetapp.view.monthlyreport.MonthlyReportViewModelFactory
import com.benoitletondor.easybudgetapp.view.monthlyreport.export.MonthlyReportExportDestination
import com.benoitletondor.easybudgetapp.view.monthlyreport.export.MonthlyReportExportView
import com.benoitletondor.easybudgetapp.view.monthlyreport.export.MonthlyReportExportViewModelFactory
import com.benoitletondor.easybudgetapp.view.onboarding.OnboardingDestination
import com.benoitletondor.easybudgetapp.view.onboarding.OnboardingResult
import com.benoitletondor.easybudgetapp.view.onboarding.OnboardingView
import com.benoitletondor.easybudgetapp.view.premium.PremiumDestination
import com.benoitletondor.easybudgetapp.view.premium.PremiumView
import com.benoitletondor.easybudgetapp.view.settings.SettingsView
import com.benoitletondor.easybudgetapp.view.settings.SettingsViewDestination
import com.benoitletondor.easybudgetapp.view.settings.backup.BackupSettingsDestination
import com.benoitletondor.easybudgetapp.view.settings.backup.BackupSettingsView
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.reflect.typeOf

private const val OnboardingResultKey = "OnboardingResult"

@Composable
fun AppNavHost(
    closeApp: () -> Unit,
    openSubscriptionScreenFlow: Flow<Unit>,
) {
    val navController = rememberNavController()

    LaunchedEffect(key1 = "openSubscriptionScreenListener") {
        launchCollect(openSubscriptionScreenFlow) {
            navController.navigate(PremiumDestination)
        }
    }

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
                },
                navigateToSettings = {
                    navController.navigate(SettingsViewDestination)
                },
                navigateToLogin = { shouldDismissAfterAuth ->
                    navController.navigate(LoginDestination(shouldDismissAfterAuth = shouldDismissAfterAuth))
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
                },
                navigateToExportToCsv = { month ->
                    navController.navigate(MonthlyReportExportDestination(month = month.toSerializedYearMonth()))
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
        composable<MonthlyReportExportDestination>(
            typeMap = mapOf(typeOf<SerializedYearMonth>() to SerializedYearMonthNavType),
        ){ backStackEntry ->
            val destination: MonthlyReportExportDestination = backStackEntry.toRoute()
            MonthlyReportExportView(
                viewModel = hiltViewModel(
                    creationCallback = { factory: MonthlyReportExportViewModelFactory ->
                        factory.create(
                            month = destination.month.toYearMonth(),
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
        composable<SettingsViewDestination> {
            SettingsView(
                navigateUp = {
                    navController.navigateUp()
                },
                navigateToBackupSettings = {
                    navController.navigate(BackupSettingsDestination)
                },
                navigateToPremium = {
                    navController.navigate(PremiumDestination(startOnPro = false))
                }
            )
        }
        composable<BackupSettingsDestination> {
            BackupSettingsView(
                navigateUp = {
                    navController.navigateUp()
                }
            )
        }
        composable<LoginDestination> { backStackEntry ->
            val destination: LoginDestination = backStackEntry.toRoute()
            LoginView(
                viewModel = hiltViewModel(
                    creationCallback = { factory: LoginViewModelFactory ->
                        factory.create(
                            shouldDismissAfterAuth = destination.shouldDismissAfterAuth,
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

private val SerializedYearMonthNavType = object : NavType<SerializedYearMonth>(
    isNullableAllowed = false
) {
    override fun get(bundle: Bundle, key: String): SerializedYearMonth? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bundle.getParcelable(key, SerializedYearMonth::class.java)
        } else {
            @Suppress("DEPRECATION")
            bundle.getParcelable(key) as? SerializedYearMonth
        }
    }

    override fun parseValue(value: String): SerializedYearMonth {
        val json = Json.parseToJsonElement(value)
        return SerializedYearMonth(json.jsonObject["year"]!!.jsonPrimitive.int, json.jsonObject["month"]!!.jsonPrimitive.int)
    }

    override fun serializeAsValue(value: SerializedYearMonth): String {
        return Json.encodeToJsonElement(mapOf("year" to value.year, "month" to value.month)).toString()
    }

    override fun put(bundle: Bundle, key: String, value: SerializedYearMonth) {
        bundle.putParcelable(key, value)
    }
}