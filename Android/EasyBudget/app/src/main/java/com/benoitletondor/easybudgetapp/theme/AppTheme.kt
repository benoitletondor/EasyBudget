package com.benoitletondor.easybudgetapp.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.google.accompanist.themeadapter.material3.Mdc3Theme

val easyBudgetGreenColor = Color(0xFF00897B)
val easyBudgetGreenDarkColor = Color(0xFF00695C)

@Composable
fun AppTheme(
    content: @Composable () -> Unit,
) {
    return Mdc3Theme(
        setTextColors = true,
        content = content,
    )
}