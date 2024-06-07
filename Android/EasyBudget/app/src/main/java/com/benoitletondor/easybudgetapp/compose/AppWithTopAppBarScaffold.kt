package com.benoitletondor.easybudgetapp.compose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.navigation.NavController

@Composable
fun AppWithTopAppBarScaffold(
    navController: NavController,
    title: String,
    showBackButton: Boolean,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            AppTopAppBar(
                navController = navController,
                title = title,
                showBackButton = showBackButton,
                actions = actions,
            )
        },
        content = content,
    )
}