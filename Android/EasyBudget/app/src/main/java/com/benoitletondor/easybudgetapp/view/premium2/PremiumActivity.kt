package com.benoitletondor.easybudgetapp.view.premium2

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.helper.setNavigationBarColor
import com.benoitletondor.easybudgetapp.helper.setStatusBarColor
import com.benoitletondor.easybudgetapp.theme.AppTheme
import com.benoitletondor.easybudgetapp.theme.easyBudgetGreenColor

class PremiumActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)

        super.onCreate(savedInstanceState)

        // Cancelled by default
        setResult(Activity.RESULT_CANCELED)

        setContent {
            AppTheme {
                Box(modifier = Modifier
                    .background(easyBudgetGreenColor)
                    .fillMaxWidth()
                    .fillMaxHeight()
                ) {

                }
            }
        }

        setStatusBarColor(R.color.easy_budget_green)
        setNavigationBarColor(R.color.easy_budget_green)
    }
}