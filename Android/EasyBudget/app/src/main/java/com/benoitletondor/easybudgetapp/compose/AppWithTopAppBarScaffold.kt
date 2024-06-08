package com.benoitletondor.easybudgetapp.compose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable

@Composable
fun AppWithTopAppBarScaffold(
    title: String,
    backButtonBehavior: BackButtonBehavior,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            AppTopAppBar(
                title = title,
                backButtonBehavior = backButtonBehavior,
                actions = actions,
            )
        },
        content = content,
    )
}