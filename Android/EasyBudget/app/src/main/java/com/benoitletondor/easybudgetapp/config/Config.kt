package com.benoitletondor.easybudgetapp.config

import kotlinx.coroutines.flow.StateFlow

interface Config {
    fun watchGlobalAlertMessage(): StateFlow<String?>
    fun watchProAlertMessage(): StateFlow<String?>
}