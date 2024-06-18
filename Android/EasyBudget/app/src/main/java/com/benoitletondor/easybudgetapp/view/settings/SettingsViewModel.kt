package com.benoitletondor.easybudgetapp.view.settings

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benoitletondor.easybudgetapp.helper.AppTheme
import com.benoitletondor.easybudgetapp.helper.Logger
import com.benoitletondor.easybudgetapp.helper.MutableLiveFlow
import com.benoitletondor.easybudgetapp.helper.combine
import com.benoitletondor.easybudgetapp.helper.watchUserCurrency
import com.benoitletondor.easybudgetapp.iab.Iab
import com.benoitletondor.easybudgetapp.iab.PremiumCheckStatus
import com.benoitletondor.easybudgetapp.parameters.Parameters
import com.benoitletondor.easybudgetapp.parameters.watchFirstDayOfWeek
import com.benoitletondor.easybudgetapp.parameters.watchIsBackupEnabled
import com.benoitletondor.easybudgetapp.parameters.watchLowMoneyWarningAmount
import com.benoitletondor.easybudgetapp.parameters.watchShouldShowCheckedBalance
import com.benoitletondor.easybudgetapp.parameters.watchTheme
import com.benoitletondor.easybudgetapp.parameters.watchUserAllowingDailyReminderPushes
import com.benoitletondor.easybudgetapp.parameters.watchUserAllowingMonthlyReminderPushes
import com.benoitletondor.easybudgetapp.parameters.watchUserAllowingUpdatePushes
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.util.Currency

@HiltViewModel(assistedFactory = SettingsViewModelFactory::class)
class SettingsViewModel @AssistedInject constructor(
    private val parameters: Parameters,
    iab: Iab,
    @Assisted redirectToBackupSettings: Boolean,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val retryLoadingMutableFlow = MutableSharedFlow<Unit>()

    private val isNotificationPermissionGrantedMutableFlow = MutableStateFlow(isNotificationPermissionGranted())

    val stateFlow: StateFlow<State> = combine(
        parameters.watchUserCurrency(),
        parameters.watchLowMoneyWarningAmount(),
        parameters.watchFirstDayOfWeek(),
        iab.iabStatusFlow
            .flatMapLatest { iabStatus ->
                 when(iabStatus) {
                     PremiumCheckStatus.INITIALIZING,
                     PremiumCheckStatus.CHECKING -> flowOf(SubscriptionStatus.Loading)
                     PremiumCheckStatus.ERROR -> flowOf(SubscriptionStatus.Error)
                     PremiumCheckStatus.NOT_PREMIUM -> flowOf(SubscriptionStatus.NotSubscribed)
                     PremiumCheckStatus.LEGACY_PREMIUM,
                     PremiumCheckStatus.PREMIUM_SUBSCRIBED,
                     PremiumCheckStatus.PRO_SUBSCRIBED -> {
                         combine(
                             parameters.watchIsBackupEnabled(),
                             parameters.watchTheme(),
                             parameters.watchShouldShowCheckedBalance(),
                             isNotificationPermissionGrantedMutableFlow,
                             parameters.watchUserAllowingDailyReminderPushes(),
                             parameters.watchUserAllowingMonthlyReminderPushes(),
                         ) { isBackupEnabled, theme, showCheckedBalance, isNotificationPermissionGranted, dailyReminderActivated, monthlyReportNotificationActivated ->
                             when(iabStatus) {
                                 PremiumCheckStatus.LEGACY_PREMIUM,
                                 PremiumCheckStatus.PREMIUM_SUBSCRIBED -> SubscriptionStatus.PremiumSubscribed(
                                     isBackupEnabled,
                                     theme,
                                     showCheckedBalance,
                                     dailyReminderActivated = isNotificationPermissionGranted && dailyReminderActivated,
                                     monthlyReportNotificationActivated = isNotificationPermissionGranted && monthlyReportNotificationActivated,
                                 )
                                 PremiumCheckStatus.PRO_SUBSCRIBED -> SubscriptionStatus.ProSubscribed(
                                     isBackupEnabled,
                                     theme,
                                     showCheckedBalance,
                                     dailyReminderActivated = isNotificationPermissionGranted && dailyReminderActivated,
                                     monthlyReportNotificationActivated = isNotificationPermissionGranted && monthlyReportNotificationActivated,
                                 )
                                 else -> throw IllegalStateException("Unable to handle status $iabStatus")
                             }

                         }
                     }
                 }
            },
        isNotificationPermissionGrantedMutableFlow,
        parameters.watchUserAllowingUpdatePushes(),
    ) { userCurrency, lowMoneyWarningAmount, firstDayOfWeek, subscriptionStatus, isNotificationPermissionGranted, userAllowingUpdatePushes ->
        return@combine State.Loaded(
            userCurrency,
            lowMoneyWarningAmount,
            firstDayOfWeek,
            subscriptionStatus,
            userAllowingUpdatePushes = isNotificationPermissionGranted && userAllowingUpdatePushes,
        ) as State
    }
        .retryWhen { cause, _ ->
            Logger.error("Error loading settings", cause)
            emit(State.Error(cause))

            retryLoadingMutableFlow.first()
            emit(State.Loading)

            true
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    private val eventMutableFlow = MutableLiveFlow<Event>()
    val eventFlow: Flow<Event> = eventMutableFlow

    init {
        if (redirectToBackupSettings) {
            viewModelScope.launch {
                eventMutableFlow.emit(Event.OpenBackupSettings)
            }
        }
    }

    fun onNotificationPermissionChanged() {
        isNotificationPermissionGrantedMutableFlow.value = isNotificationPermissionGranted()
    }

    fun onRetryButtonPressed() {
        viewModelScope.launch {
            retryLoadingMutableFlow.emit(Unit)
        }
    }

    private fun isNotificationPermissionGranted(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

    sealed class State {
        object Loading : State()
        data class Error(val error: Throwable) : State()
        data class Loaded(
            val userCurrency: Currency,
            val lowMoneyWarningAmount: Int,
            val firstDayOfWeek: DayOfWeek,
            val subscriptionStatus: SubscriptionStatus,
            val userAllowingUpdatePushes: Boolean,
        ) : State()
    }

    sealed class SubscriptionStatus {
        sealed interface Subscribed {
            val cloudBackupEnabled: Boolean
            val theme: AppTheme
            val showCheckedBalance: Boolean
            val dailyReminderActivated: Boolean
            val monthlyReportNotificationActivated: Boolean
        }

        data object Loading : SubscriptionStatus()
        data object Error : SubscriptionStatus()
        data object NotSubscribed : SubscriptionStatus()
        data class ProSubscribed(
            override val cloudBackupEnabled: Boolean,
            override val theme: AppTheme,
            override val showCheckedBalance: Boolean,
            override val dailyReminderActivated: Boolean,
            override val monthlyReportNotificationActivated: Boolean
        ) : SubscriptionStatus(), Subscribed
        data class PremiumSubscribed(
            override val cloudBackupEnabled: Boolean,
            override val theme: AppTheme,
            override val showCheckedBalance: Boolean,
            override val dailyReminderActivated: Boolean,
            override val monthlyReportNotificationActivated: Boolean
        ) : SubscriptionStatus(), Subscribed
    }

    sealed class Event {
        data object OpenBackupSettings : Event()
    }
}

@AssistedFactory
interface SettingsViewModelFactory {
    fun create(redirectToBackupSettings: Boolean): SettingsViewModel
}